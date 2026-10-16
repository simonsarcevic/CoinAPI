package de.einkaktus.coins.util;

import de.einkaktus.coins.Main;
import de.einkaktus.coins.api.CoinAccount;
import de.einkaktus.coins.api.CoinSystemAPI;
import de.einkaktus.coins.api.CoinTransaction;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class CoinSystemApiProvider implements CoinSystemAPI {

    private final Main plugin;
    private final CoinManager coinManager;
    private final CoinEconomy economy;

    public CoinSystemApiProvider(Main plugin, CoinManager coinManager, CoinEconomy economy) {
        this.plugin = plugin;
        this.coinManager = coinManager;
        this.economy = economy;
    }

    @Override
    public CompletableFuture<Double> getBalance(UUID playerId, String playerName) {
        return supplyAsync(() -> coinManager.getBalance(playerId, playerName));
    }

    @Override
    public CompletableFuture<Double> addCoins(UUID playerId, String playerName, double amount) {
        requirePositiveAmount(amount);
        return supplyAsync(() -> coinManager.deposit(playerId, playerName, amount));
    }

    @Override
    public CompletableFuture<CoinTransaction> removeCoins(UUID playerId, String playerName, double amount) {
        requirePositiveAmount(amount);
        return supplyAsync(() -> transaction(coinManager.withdraw(playerId, playerName, amount)));
    }

    @Override
    public CompletableFuture<Double> setCoins(UUID playerId, String playerName, double amount) {
        requireAmount(amount);
        return supplyAsync(() -> coinManager.setBalance(playerId, playerName, amount));
    }

    @Override
    public CompletableFuture<Double> resetCoins(UUID playerId, String playerName) {
        return supplyAsync(() -> coinManager.resetBalance(playerId, playerName));
    }

    @Override
    public CompletableFuture<CoinTransaction> transferCoins(
            UUID senderId, String senderName, UUID recipientId, String recipientName, double amount) {
        requirePositiveAmount(amount);
        return supplyAsync(() -> transaction(coinManager.transfer(
                senderId, senderName, recipientId, recipientName, amount)));
    }

    @Override
    public CompletableFuture<List<CoinAccount>> getTopBalances(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("Das Limit muss mindestens 1 sein.");
        }
        return supplyAsync(() -> {
            List<CoinAccount> accounts = new ArrayList<>();
            for (CoinManager.AccountBalance account : coinManager.getTopBalances(limit)) {
                accounts.add(new CoinAccount(account.getUuid(), account.getName(), account.getBalance()));
            }
            return accounts;
        });
    }

    @Override
    public String format(double amount) {
        return economy.format(amount);
    }

    private <T> CompletableFuture<T> supplyAsync(DatabaseSupplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                future.complete(supplier.get());
            } catch (Exception exception) {
                plugin.getLogger().severe("API-Datenbankfehler: " + exception.getMessage());
                future.completeExceptionally(exception);
            }
        });
        return future;
    }

    private static CoinTransaction transaction(CoinManager.TransactionResult result) {
        return new CoinTransaction(result.isSuccessful(), result.getBalance());
    }

    private static void requirePositiveAmount(double amount) {
        requireAmount(amount);
        if (amount <= 0.0D) {
            throw new IllegalArgumentException("Der Betrag muss größer als 0 sein.");
        }
    }

    private static void requireAmount(double amount) {
        if (!CoinManager.validAmount(amount)) {
            throw new IllegalArgumentException("Ungültiger Betrag.");
        }
    }

    private interface DatabaseSupplier<T> {
        T get() throws Exception;
    }
}
