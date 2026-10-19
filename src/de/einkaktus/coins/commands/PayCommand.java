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
import java.util.UUID;

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

        UUID senderId = player.getUniqueId();
        String senderName = player.getName();
        UUID targetId = target.getUniqueId();
        String targetName = target.getName();
        String targetDisplayName = displayName(target);

        async(sender, () -> {
            CoinManager.TransactionResult result = coinManager.transfer(
                    senderId, senderName, targetId, targetName, amount
            );
            if (!result.isSuccessful()) {
                reply(sender, Messages.prefix + ChatColor.GRAY + "Du besitzt §cnicht §7genügend Coins!");
                return;
            }

            reply(sender, Messages.prefix + "Du hast " + ChatColor.GREEN + targetDisplayName + ChatColor.GRAY
                    + " " + ChatColor.GREEN + economy.format(amount) + ChatColor.GRAY + " überwiesen!");
            Bukkit.getScheduler().runTask(plugin, () -> notifyRecipient(senderName, targetId, amount));
        });
        return true;
    }

    private void notifyRecipient(String senderName, UUID recipientId, double amount) {
        Player onlineRecipient = Bukkit.getPlayer(recipientId);
        if (onlineRecipient != null) {
            onlineRecipient.sendMessage(Messages.prefix + ChatColor.GREEN + senderName + ChatColor.GRAY
                    + " hat dir " + ChatColor.GREEN + economy.format(amount) + ChatColor.GRAY + " überwiesen!");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? onlinePlayerNames(args[0]) : Collections.emptyList();
    }
}
