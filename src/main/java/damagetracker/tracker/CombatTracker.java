package damagetracker.tracker;

import java.util.*;

public class CombatTracker {
    private final Map<String, PlayerDamageStats> playerStats = new LinkedHashMap<>();
    private int currentTurn = 0;
    private boolean inCombat = false;

    public void startCombat() {
        inCombat = true;
        currentTurn = 1;
        // Clear all player entries for a fresh combat
        playerStats.clear();
    }

    public void endCombat() {
        inCombat = false;
    }

    public void nextTurn() {
        for (PlayerDamageStats stats : playerStats.values()) {
            stats.resetTurn();
        }
        currentTurn++;
    }

    public PlayerDamageStats getOrCreateStats(String playerId) {
        return playerStats.computeIfAbsent(playerId, PlayerDamageStats::new);
    }

    public PlayerDamageStats getStats(String playerId) {
        return playerStats.get(playerId);
    }

    public boolean hasStats(String playerId) {
        return playerStats.containsKey(playerId);
    }

    public Collection<PlayerDamageStats> getAllStats() {
        return playerStats.values();
    }

    public boolean isInCombat() { return inCombat; }
    public int getCurrentTurn() { return currentTurn; }

    public void clear() {
        playerStats.clear();
        currentTurn = 0;
        inCombat = false;
    }
}
