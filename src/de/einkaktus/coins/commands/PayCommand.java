package de.einkaktus.coins.commands;

import de.einkaktus.coins.Main;
import de.einkaktus.coins.structure.Messages;
import de.einkaktus.coins.util.CoinEconomy;
import de.einkaktus.coins.util.CoinManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public final class PayCommand extends BaseCommand implements CommandExecutor, TabCompleter {

    public PayCommand(Main plugin, CoinManager coinManager, CoinEconomy economy) {
        super(plugin, coinManager, economy);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.prefix + Messages.onlyPlayers);
            return true;
        }
        if (!hasPermission(sender, "coinsystem.pay")) {
            noPermission(sender);
            return true;
        }
        if (args.length != 2) {
            sender.sendMessage(Messages.prefix + ChatColor.RED + "Verwendung: /pay <Spieler> <Betrag>");
            return true;
        }

        Player player = (Player) sender;
        OfflinePlayer target = offlinePlayer(args[0]);
        if (player.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(Messages.prefix + ChatColor.GRAY + "Du kannst dir §cnicht §7selbst Coins überweisen!");
            return true;
        }

        Double amount = parseAmount(args[1], false);
        if (amount == null) {
            sender.sendMessage(Messages.prefix + Messages.invalidAmount);
            return true;
        }

        async(sender, () -> {
            CoinManager.TransactionResult result = coinManager.transfer(player, target, amount);
            if (!result.isSuccessful()) {
                reply(sender, Messages.prefix + ChatColor.GRAY + "Du besitzt §cnicht §7genügend Coins!");
                return;
            }

            reply(sender, Messages.prefix + "Du hast " + ChatColor.GREEN + displayName(target) + ChatColor.GRAY
                    + " " + ChatColor.GREEN + economy.format(amount) + ChatColor.GRAY + " überwiesen!");
            Bukkit.getScheduler().runTask(plugin, () -> notifyRecipient(player, target, amount));
        });
        return true;
    }

    private void notifyRecipient(Player sender, OfflinePlayer recipient, double amount) {
        Player onlineRecipient = Bukkit.getPlayer(recipient.getUniqueId());
        if (onlineRecipient != null) {
            onlineRecipient.sendMessage(Messages.prefix + ChatColor.GREEN + sender.getName() + ChatColor.GRAY
                    + " hat dir " + ChatColor.GREEN + economy.format(amount) + ChatColor.GRAY + " überwiesen!");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? onlinePlayerNames(args[0]) : Collections.emptyList();
    }
}
