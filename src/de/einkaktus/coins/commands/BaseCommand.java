package de.einkaktus.coins.commands;

import de.einkaktus.coins.Main;
import de.einkaktus.coins.structure.Messages;
import de.einkaktus.coins.util.CoinEconomy;
import de.einkaktus.coins.util.CoinManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

abstract class BaseCommand {

    protected final Main plugin;
    protected final CoinManager coinManager;
    protected final CoinEconomy economy;

    BaseCommand(Main plugin, CoinManager coinManager, CoinEconomy economy) {
        this.plugin = plugin;
        this.coinManager = coinManager;
        this.economy = economy;
    }

    protected void async(CommandSender sender, DatabaseAction action) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                action.run();
            } catch (SQLException exception) {
                plugin.getLogger().severe("MariaDB-Fehler: " + exception.getMessage());
                reply(sender, Messages.prefix + Messages.databaseError);
            }
        });
    }

    protected void reply(CommandSender sender, String message) {
        Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(message));
    }

    protected boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(permission) || sender.hasPermission("coinsystem.admin");
    }

    protected void noPermission(CommandSender sender) {
        sender.sendMessage(Messages.prefix + Messages.noPermission);
    }

    @SuppressWarnings("deprecation")
    protected OfflinePlayer offlinePlayer(String name) {
        return Bukkit.getOfflinePlayer(name);
    }

    protected String displayName(OfflinePlayer player) {
        return player.getName() == null ? player.getUniqueId().toString() : player.getName();
    }

    protected Double parseAmount(String input, boolean allowZero) {
        try {
            double amount = Double.parseDouble(input.replace(',', '.'));
            if (!CoinManager.validAmount(amount) || (!allowZero && amount <= 0.0D)) {
                return null;
            }
            return CoinManager.money(amount);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    protected List<String> onlinePlayerNames(String prefix) {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return filter(names, prefix);
    }

    protected List<String> filter(List<String> values, String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                matches.add(value);
            }
        }
        return matches;
    }

    protected interface DatabaseAction {
        void run() throws SQLException;
    }
}
