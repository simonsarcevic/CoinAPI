package de.einkaktus.coins.api;

public final class CoinTransaction {

    private final boolean successful;
    private final double balance;

    public CoinTransaction(boolean successful, double balance) {
        this.successful = successful;
        this.balance = balance;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public double getBalance() {
        return balance;
    }
}
