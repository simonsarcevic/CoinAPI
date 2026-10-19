package de.einkaktus.coins.commands;

import de.einkaktus.coins.Main;
import de.einkaktus.coins.structure.Messages;
import de.einkaktus.coins.util.CoinEconomy;
import de.einkaktus.coins.util.CoinManager;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class EcoCommand extends BaseCommand implements CommandExecutor, TabCompleter {

    public EcoCommand(Main plugin, CoinManager coinManager, CoinEconomy economy) {
        super(plugin, coinManager, economy);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        if (!Arrays.asList("give", "take", "set", "reset").contains(action)) {
            sendHelp(sender);
            return true;
        }
        if (!hasPermission(sender, "coinsystem.eco." + action)) {
            noPermission(sender);
            return true;
        }

        int requiredArguments = action.equals("reset") ? 2 : 3;
        if (args.length != requiredArguments) {
            sender.sendMessage(Messages.prefix + ChatColor.RED + usage(action));
            return true;
        }

        OfflinePlayer target = offlinePlayer(args[1]);
        UUID targetId = target.getUniqueId();
        String targetName = target.getName();
        String targetDisplayName = displayName(target);
        if (action.equals("reset")) {
            async(sender, () -> {
                double balance = coinManager.resetBalance(targetId, targetName);
                reply(sender, Messages.prefix + ChatColor.RED + targetDisplayName + ChatColor.GRAY
                        + " wurde auf " + ChatColor.RED + economy.format(balance) + ChatColor.GRAY + " zurückgesetzt!");
            });
            return true;
        }

        Double amount = parseAmount(args[2], action.equals("set"));
        if (amount == null) {
            sender.sendMessage(Messages.prefix + Messages.invalidAmount);
            return true;
        }

        async(sender, () -> executeChange(
                sender, action, targetId, targetName, targetDisplayName, amount
        ));
        return true;
    }

    private void executeChange(CommandSender sender, String action, UUID targetId,
                               String targetName, String targetDisplayName, double amount)
            throws java.sql.SQLException {
        if (action.equals("give")) {
            double balance = coinManager.deposit(targetId, targetName, amount);
            reply(sender, changedMessage(targetDisplayName, amount, balance, "gegeben"));
        } else if (action.equals("take")) {
            CoinManager.TransactionResult result = coinManager.withdraw(targetId, targetName, amount);
            if (!result.isSuccessful()) {
                reply(sender, Messages.prefix + ChatColor.RED + targetDisplayName
                        + "§7 besitzt §cnicht §7genügend Coins!");
                return;
            }
            reply(sender, changedMessage(targetDisplayName, amount, result.getBalance(), "abgezogen"));
        } else {
            double balance = coinManager.setBalance(targetId, targetName, amount);
            reply(sender, Messages.prefix + ChatColor.GREEN + targetDisplayName + ChatColor.GRAY
                    + " hat nun " + ChatColor.GREEN + economy.format(balance) + ChatColor.GRAY + "!");
        }
    }

    private String changedMessage(String targetName, double amount, double balance, String operation) {
        return Messages.prefix + ChatColor.GREEN + targetName + ChatColor.GRAY + " wurden "
                + ChatColor.GREEN + economy.format(amount) + ChatColor.GRAY + " " + operation
                + ". Neuer Kontostand: " + ChatColor.GREEN + economy.format(balance);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "----- CoinSystem Admin -----");
        sender.sendMessage(ChatColor.GREEN + "/eco give <Spieler> <Betrag>");
        sender.sendMessage(ChatColor.GREEN + "/eco take <Spieler> <Betrag>");
        sender.sendMessage(ChatColor.GREEN + "/eco set <Spieler> <Betrag>");
        sender.sendMessage(ChatColor.GREEN + "/eco reset <Spieler>");
    }

    private String usage(String action) {
        return action.equals("reset")
                ? "Verwendung: /eco reset <Spieler>"
                : "Verwendung: /eco " + action + " <Spieler> <Betrag>";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("give", "take", "set", "reset"), args[0]);
        }
        if (args.length == 2) {
            return onlinePlayerNames(args[1]);
        }
        return Collections.emptyList();
    }
}
