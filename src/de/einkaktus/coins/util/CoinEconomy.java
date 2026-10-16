package de.einkaktus.coins.util;

import de.einkaktus.coins.Main;
import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("deprecation")
public final class CoinEconomy extends AbstractEconomy {

    private static final EconomyResponse.ResponseType SUCCESS = EconomyResponse.ResponseType.SUCCESS;
    private static final EconomyResponse.ResponseType FAILURE = EconomyResponse.ResponseType.FAILURE;
    private static final EconomyResponse.ResponseType NOT_IMPLEMENTED = EconomyResponse.ResponseType.NOT_IMPLEMENTED;

    private final Main plugin;
    private final CoinManager coinManager;
    private final DecimalFormat numberFormat;

    public CoinEconomy(Main plugin, CoinManager coinManager) {
        this.plugin = plugin;
        this.coinManager = coinManager;
        this.numberFormat = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.GERMANY));
    }

    @Override
    public boolean isEnabled() {
        return plugin.isEnabled();
    }

    @Override
    public String getName() {
        return plugin.getDescription().getName();
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 2;
    }

    @Override
    public synchronized String format(double amount) {
        return numberFormat.format(amount) + " " + (Math.abs(amount) == 1.0D ? currencyNameSingular() : currencyNamePlural());
    }

    @Override
    public String currencyNamePlural() {
        return plugin.getConfig().getString("currency.plural", "Coins");
    }

    @Override
    public String currencyNameSingular() {
        return plugin.getConfig().getString("currency.singular", "Coin");
    }

    @Override
    public boolean hasAccount(String playerName) {
        return hasAccount(player(playerName));
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        try {
            return coinManager.hasAccount(player);
        } catch (SQLException exception) {
            databaseError(exception);
            return false;
        }
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }

    @Override
    public double getBalance(String playerName) {
        return getBalance(player(playerName));
    }

    @Override
    public double getBalance(String playerName, String worldName) {
        return getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        try {
            return coinManager.getBalance(player);
        } catch (SQLException exception) {
            databaseError(exception);
            return 0.0D;
        }
    }

    @Override
    public double getBalance(OfflinePlayer player, String worldName) {
        return getBalance(player);
    }

    @Override
    public boolean has(String playerName, double amount) {
        return validAmount(amount) && getBalance(playerName) >= amount;
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return validAmount(amount) && getBalance(player) >= amount;
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        return withdrawPlayer(player(playerName), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        double balance = getBalance(player);
        if (!validAmount(amount)) {
            return failure(amount, balance, "Der Betrag muss eine endliche, positive Zahl sein.");
        }
        if (balance < amount) {
            return failure(amount, balance, "Das Konto ist nicht ausreichend gedeckt.");
        }
        try {
            CoinManager.TransactionResult result = coinManager.withdraw(player, amount);
            return result.isSuccessful()
                    ? success(amount, result.getBalance())
                    : failure(amount, result.getBalance(), "Das Konto ist nicht ausreichend gedeckt.");
        } catch (SQLException exception) {
            databaseError(exception);
            return failure(amount, balance, "Datenbankfehler.");
        }
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        return depositPlayer(player(playerName), amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        double balance = getBalance(player);
        if (!validAmount(amount) || balance > Double.MAX_VALUE - amount) {
            return failure(amount, balance, "Der Betrag ist ungueltig oder zu gross.");
        }
        try {
            return success(amount, coinManager.deposit(player, amount));
        } catch (SQLException exception) {
            databaseError(exception);
            return failure(amount, balance, "Datenbankfehler.");
        }
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        return createPlayerAccount(player(playerName));
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        try {
            return coinManager.createAccount(player);
        } catch (SQLException exception) {
            databaseError(exception);
            return false;
        }
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        return unsupported();
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return unsupported();
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return unsupported();
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return unsupported();
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return unsupported();
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return unsupported();
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return unsupported();
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return unsupported();
    }

    @Override
    public List<String> getBanks() {
        return Collections.emptyList();
    }

    private OfflinePlayer player(String playerName) {
        return Bukkit.getOfflinePlayer(playerName);
    }

    private static boolean validAmount(double amount) {
        return CoinManager.validAmount(amount);
    }

    private void databaseError(SQLException exception) {
        plugin.getLogger().severe("MariaDB-Fehler: " + exception.getMessage());
    }

    private static EconomyResponse success(double amount, double balance) {
        return new EconomyResponse(amount, balance, SUCCESS, null);
    }

    private static EconomyResponse failure(double amount, double balance, String message) {
        return new EconomyResponse(amount, balance, FAILURE, message);
    }

    private static EconomyResponse unsupported() {
        return new EconomyResponse(0.0D, 0.0D, NOT_IMPLEMENTED, "Bankkonten werden nicht unterstuetzt.");
    }
}
