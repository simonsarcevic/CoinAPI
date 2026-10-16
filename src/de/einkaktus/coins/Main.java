package de.einkaktus.coins;

import de.einkaktus.coins.api.CoinSystemAPI;
import de.einkaktus.coins.commands.BalanceCommand;
import de.einkaktus.coins.commands.BalanceTopCommand;
import de.einkaktus.coins.commands.EcoCommand;
import de.einkaktus.coins.commands.PayCommand;
import de.einkaktus.coins.util.CoinEconomy;
import de.einkaktus.coins.util.CoinManager;
import de.einkaktus.coins.util.CoinSystemApiProvider;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private static Main instance;
    private CoinManager coinManager;
    private CoinEconomy economy;
    private CoinSystemAPI coinSystemAPI;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("Vault wurde nicht gefunden. CoinSystem wird deaktiviert.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        try {
            coinManager = new CoinManager(this);
            coinManager.initialize();
        } catch (Exception exception) {
            getLogger().severe("Die MariaDB-Verbindung konnte nicht hergestellt werden: " + exception.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        economy = new CoinEconomy(this, coinManager);
        coinSystemAPI = new CoinSystemApiProvider(this, coinManager, economy);
        Bukkit.getServicesManager().register(Economy.class, economy, this, ServicePriority.Highest);
        Bukkit.getServicesManager().register(CoinSystemAPI.class, coinSystemAPI, this, ServicePriority.Normal);

        EcoCommand ecoCommand = new EcoCommand(this, coinManager, economy);
        BalanceCommand balanceCommand = new BalanceCommand(this, coinManager, economy);
        PayCommand payCommand = new PayCommand(this, coinManager, economy);

        getCommand("eco").setExecutor(ecoCommand);
        getCommand("eco").setTabCompleter(ecoCommand);
        getCommand("balance").setExecutor(balanceCommand);
        getCommand("balance").setTabCompleter(balanceCommand);
        getCommand("pay").setExecutor(payCommand);
        getCommand("pay").setTabCompleter(payCommand);
        getCommand("baltop").setExecutor(new BalanceTopCommand(this, coinManager, economy));

        getLogger().info("CoinSystem wurde mit MariaDB als Vault-Economy registriert.");
    }

    @Override
    public void onDisable() {
        if (economy != null) {
            Bukkit.getServicesManager().unregister(Economy.class, economy);
        }
        if (coinSystemAPI != null) {
            Bukkit.getServicesManager().unregister(CoinSystemAPI.class, coinSystemAPI);
        }
    }

    public static Main getInstance() {
        return instance;
    }

    public CoinManager getCoinManager() {
        return coinManager;
    }

    public CoinSystemAPI getCoinSystemAPI() {
        return coinSystemAPI;
    }
}
