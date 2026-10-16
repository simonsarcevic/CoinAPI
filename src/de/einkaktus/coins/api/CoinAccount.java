package de.einkaktus.coins.api;

import java.util.UUID;

public final class CoinAccount {

    private final UUID uniqueId;
    private final String playerName;
    private final double balance;

    public CoinAccount(UUID uniqueId, String playerName, double balance) {
        this.uniqueId = uniqueId;
        this.playerName = playerName;
        this.balance = balance;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public double getBalance() {
        return balance;
    }
}
