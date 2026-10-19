package de.einkaktus.coins.commands;

import de.einkaktus.coins.Main;
import de.einkaktus.coins.structure.Messages;
import de.einkaktus.coins.util.CoinEconomy;
import de.einkaktus.coins.util.CoinManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public final class BalanceTopCommand extends BaseCommand implements CommandExecutor {

    public BalanceTopCommand(Main plugin, CoinManager coinManager, CoinEconomy economy) {
        super(plugin, coinManager, economy);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "coinsystem.baltop")) {
            noPermission(sender);
            return true;
        }
        if (args.length != 0) {
            sender.sendMessage(Messages.prefix + ChatColor.RED + "Verwendung: /baltop");
            return true;
        }

        int limit = Math.max(1, plugin.getConfig().getInt("baltop-size", 10));
        async(sender, () -> {
            List<CoinManager.AccountBalance> top = coinManager.getTopBalances(limit);
            List<String> lines = new ArrayList<>();
            lines.add(ChatColor.GOLD + "----- Reichste Spieler -----");
            if (top.isEmpty()) {
                lines.add(ChatColor.GRAY + "Noch keine Konten vorhanden.");
            } else {
                for (int index = 0; index < top.size(); index++) {
                    CoinManager.AccountBalance account = top.get(index);
                    String name = account.getName() == null ? account.getUuid().toString() : account.getName();
                    lines.add(ChatColor.GREEN + "#" + (index + 1) + " " + ChatColor.WHITE + name
                            + ChatColor.DARK_GRAY + " - " + ChatColor.GREEN + economy.format(account.getBalance()));
                }
            }
            Bukkit.getScheduler().runTask(plugin, () -> lines.forEach(sender::sendMessage));
        });
        return true;
    }
}
