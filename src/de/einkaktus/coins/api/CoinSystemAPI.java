package de.einkaktus.coins.api;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Öffentliche Schnittstelle des CoinSystems für andere Plugins.
 * Alle Methoden mit Datenbankzugriff arbeiten asynchron.
 */
public interface CoinSystemAPI {

    static CoinSystemAPI get() {
        RegisteredServiceProvider<CoinSystemAPI> registration =
                Bukkit.getServicesManager().getRegistration(CoinSystemAPI.class);
        if (registration == null) {
            throw new IllegalStateException("CoinSystemAPI ist nicht registriert.");
        }
        return registration.getProvider();
    }

    CompletableFuture<Double> getBalance(UUID playerId, String playerName);

    CompletableFuture<Double> addCoins(UUID playerId, String playerName, double amount);

    CompletableFuture<CoinTransaction> removeCoins(UUID playerId, String playerName, double amount);

    CompletableFuture<Double> setCoins(UUID playerId, String playerName, double amount);

    CompletableFuture<Double> resetCoins(UUID playerId, String playerName);

    CompletableFuture<CoinTransaction> transferCoins(
            UUID senderId, String senderName,
            UUID recipientId, String recipientName,
            double amount
    );

    CompletableFuture<List<CoinAccount>> getTopBalances(int limit);

    String format(double amount);
}
