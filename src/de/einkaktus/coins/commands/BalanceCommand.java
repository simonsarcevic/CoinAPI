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
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class BalanceCommand extends BaseCommand implements CommandExecutor, TabCompleter {

    public BalanceCommand(Main plugin, CoinManager coinManager, CoinEconomy economy) {
        super(plugin, coinManager, economy);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 1) {
            sender.sendMessage(Messages.prefix + ChatColor.RED + "Verwendung: /bal [Spieler]");
            return true;
        }

        final OfflinePlayer target;
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Messages.prefix + ChatColor.RED + "Die Konsole muss einen Spieler angeben.");
                return true;
            }
            if (!hasPermission(sender, "coinsystem.balance")) {
                noPermission(sender);
                return true;
            }
            target = (Player) sender;
        } else {
            if (!hasPermission(sender, "coinsystem.balance.others")) {
                noPermission(sender);
                return true;
            }
            target = offlinePlayer(args[0]);
        }

        UUID targetId = target.getUniqueId();
        String targetName = target.getName();
        String targetDisplayName = displayName(target);
        boolean ownBalance = sender instanceof Player
                && ((Player) sender).getUniqueId().equals(targetId);

        async(sender, () -> {
            double balance = coinManager.getBalance(targetId, targetName);
            String owner = ownBalance
                    ? "Dein Kontostand beträgt"
                    : "Kontostand von " + targetDisplayName + " beträgt";
            reply(sender, Messages.prefix + ChatColor.GRAY + owner + ChatColor.DARK_GRAY + ": "
                    + ChatColor.GREEN + economy.format(balance));
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? onlinePlayerNames(args[0]) : Collections.emptyList();
    }
}
