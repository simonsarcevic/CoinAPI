# CoinSystem-API verwenden

## Abhängigkeit einrichten

Das andere Plugin verwendet die `CoinSystem.jar` ausschließlich als
`provided`-/`compileOnly`-Abhängigkeit. Die API-Klassen dürfen nicht in die JAR
des anderen Plugins kopiert oder eingebettet werden.

In der `plugin.yml` des anderen Plugins muss CoinSystem als feste Abhängigkeit
eingetragen werden:

```yaml
name: BeispielPlugin
version: 1.0
main: de.beispiel.plugin.BeispielPlugin
depend: [CoinSystem]
```

Auf dem Server liegen anschließend beide Plugins als getrennte Dateien:

```text
plugins/CoinSystem.jar
plugins/BeispielPlugin.jar
plugins/Vault.jar
```

## Vollständiges Java-Beispiel

Das folgende Beispiel ist eine vollständige und kompilierbare Plugin-Hauptklasse:

```java
package de.beispiel.plugin;

import de.einkaktus.coins.api.CoinSystemAPI;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class BeispielPlugin extends JavaPlugin {

    private CoinSystemAPI coinAPI;

    @Override
    public void onEnable() {
        coinAPI = CoinSystemAPI.get();
    }

    public void showBalance(Player player) {
        coinAPI.getBalance(player.getUniqueId(), player.getName())
                .thenAccept(balance -> getServer().getScheduler().runTask(
                        this,
                        () -> player.sendMessage(
                                "Coins: " + coinAPI.format(balance)
                        )
                ))
                .exceptionally(error -> {
                    getLogger().warning(
                            "Coin-Abfrage fehlgeschlagen: " + error.getMessage()
                    );
                    return null;
                });
    }
}
```

Alle Datenbankoperationen sind asynchron und geben ein `CompletableFuture`
zurück. Innerhalb von `thenAccept` läuft der Code zunächst auf einem
Hintergrundthread. Aufrufe der Bukkit-API müssen deshalb, wie im Beispiel, mit
`runTask` zurück auf den Server-Hauptthread gelegt werden.

## Verfügbare Operationen

- `getBalance(uuid, name)` – Kontostand abrufen
- `addCoins(uuid, name, amount)` – Coins hinzufügen
- `removeCoins(uuid, name, amount)` – Coins abziehen
- `setCoins(uuid, name, amount)` – Kontostand festlegen
- `resetCoins(uuid, name)` – Kontostand zurücksetzen
- `transferCoins(senderUuid, senderName, recipientUuid, recipientName, amount)` – Coins überweisen
- `getTopBalances(limit)` – Rangliste abrufen
- `format(amount)` – Betrag mit Währungsnamen formatieren

`removeCoins` und `transferCoins` liefern eine `CoinTransaction`. Mit
`isSuccessful()` wird geprüft, ob die Transaktion ausgeführt wurde. Der danach
verfügbare Kontostand steht in `getBalance()`.

## Beispiel für eine Abbuchung

Diese Methode kann in die oben gezeigte Klasse `BeispielPlugin` eingefügt
werden:

```java
public void removeCoins(Player player) {
    coinAPI.removeCoins(player.getUniqueId(), player.getName(), 100.0D)
            .thenAccept(transaction -> {
                if (!transaction.isSuccessful()) {
                    getServer().getScheduler().runTask(
                            this,
                            () -> player.sendMessage("Nicht genügend Coins.")
                    );
                }
            })
            .exceptionally(error -> {
                getLogger().warning(
                        "Abbuchung fehlgeschlagen: " + error.getMessage()
                );
                return null;
            });
}
```
