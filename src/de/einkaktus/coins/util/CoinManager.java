package de.einkaktus.coins.util;

import de.einkaktus.coins.Main;
import org.bukkit.OfflinePlayer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CoinManager {

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final String table;
    private final double startingBalance;

    public CoinManager(Main plugin) {
        String host = plugin.getConfig().getString("database.host", "localhost");
        int port = plugin.getConfig().getInt("database.port", 3306);
        String database = plugin.getConfig().getString("database.database", "coinsystem");
        boolean useSsl = plugin.getConfig().getBoolean("database.use-ssl", false);

        this.username = plugin.getConfig().getString("database.username", "root");
        this.password = plugin.getConfig().getString("database.password", "");
        this.table = validateTable(plugin.getConfig().getString("database.table", "coin_accounts"));
        this.startingBalance = money(plugin.getConfig().getDouble("starting-balance", 0.0D));
        this.jdbcUrl = "jdbc:mariadb://" + host + ":" + port + "/" + database
                + "?useSsl=" + useSsl + "&connectTimeout=5000&socketTimeout=10000";
    }

    public void initialize() throws SQLException, ClassNotFoundException {
        Class.forName("org.mariadb.jdbc.Driver");
        String sql = "CREATE TABLE IF NOT EXISTS `" + table + "` ("
                + "`uuid` CHAR(36) NOT NULL,"
                + "`player_name` VARCHAR(16) NULL,"
                + "`balance` DECIMAL(19,2) NOT NULL DEFAULT 0.00,"
                + "`updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (`uuid`),"
                + "INDEX `idx_player_name` (`player_name`)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    public boolean hasAccount(OfflinePlayer player) throws SQLException {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM `" + table + "` WHERE `uuid` = ?")) {
            statement.setString(1, player.getUniqueId().toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    public boolean createAccount(OfflinePlayer player) throws SQLException {
        try (Connection connection = connection()) {
            return ensureAccount(connection, player.getUniqueId(), player.getName());
        }
    }

    public double getBalance(OfflinePlayer player) throws SQLException {
        return getBalance(player.getUniqueId(), player.getName());
    }

    public double getBalance(UUID uuid, String name) throws SQLException {
        try (Connection connection = connection()) {
            ensureAccount(connection, uuid, name);
            return readBalance(connection, uuid);
        }
    }

    public double deposit(OfflinePlayer player, double amount) throws SQLException {
        return deposit(player.getUniqueId(), player.getName(), amount);
    }

    public double deposit(UUID uuid, String name, double amount) throws SQLException {
        amount = money(amount);
        try (Connection connection = connection()) {
            ensureAccount(connection, uuid, name);
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE `" + table + "` SET `balance` = `balance` + ? WHERE `uuid` = ?")) {
                statement.setDouble(1, amount);
                statement.setString(2, uuid.toString());
                statement.executeUpdate();
            }
            return readBalance(connection, uuid);
        }
    }

    public TransactionResult withdraw(OfflinePlayer player, double amount) throws SQLException {
        return withdraw(player.getUniqueId(), player.getName(), amount);
    }

    public TransactionResult withdraw(UUID uuid, String name, double amount) throws SQLException {
        amount = money(amount);
        try (Connection connection = connection()) {
            ensureAccount(connection, uuid, name);
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE `" + table + "` SET `balance` = `balance` - ? "
                            + "WHERE `uuid` = ? AND `balance` >= ?")) {
                statement.setDouble(1, amount);
                statement.setString(2, uuid.toString());
                statement.setDouble(3, amount);
                boolean success = statement.executeUpdate() == 1;
                return new TransactionResult(success, readBalance(connection, uuid));
            }
        }
    }

    public double setBalance(OfflinePlayer player, double amount) throws SQLException {
        return setBalance(player.getUniqueId(), player.getName(), amount);
    }

    public double setBalance(UUID uuid, String name, double amount) throws SQLException {
        amount = money(amount);
        try (Connection connection = connection()) {
            ensureAccount(connection, uuid, name);
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE `" + table + "` SET `balance` = ? WHERE `uuid` = ?")) {
                statement.setDouble(1, amount);
                statement.setString(2, uuid.toString());
                statement.executeUpdate();
            }
        }
        return amount;
    }

    public double resetBalance(OfflinePlayer player) throws SQLException {
        return resetBalance(player.getUniqueId(), player.getName());
    }

    public double resetBalance(UUID uuid, String name) throws SQLException {
        return setBalance(uuid, name, startingBalance);
    }

    public TransactionResult transfer(OfflinePlayer sender, OfflinePlayer recipient, double amount) throws SQLException {
        return transfer(sender.getUniqueId(), sender.getName(), recipient.getUniqueId(), recipient.getName(), amount);
    }

    public TransactionResult transfer(
            UUID senderId, String senderName, UUID recipientId, String recipientName, double amount) throws SQLException {
        amount = money(amount);
        if (senderId.equals(recipientId)) {
            return new TransactionResult(false, getBalance(senderId, senderName));
        }

        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try {
                ensureAccount(connection, senderId, senderName);
                ensureAccount(connection, recipientId, recipientName);

                UUID first = senderId.toString().compareTo(recipientId.toString()) <= 0 ? senderId : recipientId;
                UUID second = first.equals(senderId) ? recipientId : senderId;
                lockAccount(connection, first);
                lockAccount(connection, second);

                double senderBalance = readBalance(connection, senderId);
                if (senderBalance < amount) {
                    connection.rollback();
                    return new TransactionResult(false, senderBalance);
                }

                updateBalance(connection, senderId, -amount);
                updateBalance(connection, recipientId, amount);
                connection.commit();
                return new TransactionResult(true, money(senderBalance - amount));
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public List<AccountBalance> getTopBalances(int limit) throws SQLException {
        List<AccountBalance> balances = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT `uuid`, `player_name`, `balance` FROM `" + table
                             + "` ORDER BY `balance` DESC, `player_name` ASC LIMIT ?")) {
            statement.setInt(1, Math.max(1, limit));
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    balances.add(new AccountBalance(
                            UUID.fromString(result.getString("uuid")),
                            result.getString("player_name"),
                            result.getDouble("balance")));
                }
            }
        }
        return balances;
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    private boolean ensureAccount(Connection connection, UUID uuid, String name) throws SQLException {
        String sql = "INSERT INTO `" + table + "` (`uuid`, `player_name`, `balance`) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE `player_name` = COALESCE(VALUES(`player_name`), `player_name`)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, name);
            statement.setDouble(3, startingBalance);
            return statement.executeUpdate() == 1;
        }
    }

    private double readBalance(Connection connection, UUID uuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT `balance` FROM `" + table + "` WHERE `uuid` = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Konto nicht gefunden: " + uuid);
                }
                return money(result.getDouble("balance"));
            }
        }
    }

    private void lockAccount(Connection connection, UUID uuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT `uuid` FROM `" + table + "` WHERE `uuid` = ? FOR UPDATE")) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Konto konnte nicht gesperrt werden: " + uuid);
                }
            }
        }
    }

    private void updateBalance(Connection connection, UUID uuid, double difference) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE `" + table + "` SET `balance` = `balance` + ? WHERE `uuid` = ?")) {
            statement.setDouble(1, difference);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
        }
    }

    private static String validateTable(String table) {
        if (table == null || !table.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Ungueltiger Datenbank-Tabellenname.");
        }
        return table;
    }

    public static boolean validAmount(double amount) {
        return amount >= 0.0D && !Double.isNaN(amount) && !Double.isInfinite(amount)
                && amount <= 99999999999999999.99D;
    }

    public static double money(double amount) {
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static final class TransactionResult {
        private final boolean successful;
        private final double balance;

        TransactionResult(boolean successful, double balance) {
            this.successful = successful;
            this.balance = balance;
        }

        public boolean isSuccessful() {
            return successful;
        }

        public double getBalance() {
            return balance;
        }
    }

    public static final class AccountBalance {
        private final UUID uuid;
        private final String name;
        private final double balance;

        AccountBalance(UUID uuid, String name, double balance) {
            this.uuid = uuid;
            this.name = name;
            this.balance = balance;
        }

        public UUID getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }

        public double getBalance() {
            return balance;
        }
    }
}
