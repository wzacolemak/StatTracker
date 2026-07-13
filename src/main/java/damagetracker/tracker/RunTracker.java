package damagetracker.tracker;

import java.util.*;

public class RunTracker {
    private final Map<String, PlayerDamageStats> runStats = new LinkedHashMap<>();
    private final List<CombatSnapshot> combatHistory = new ArrayList<>();
    private final List<String> eventLog = new ArrayList<>();
    private final Set<String> deadPlayers = new HashSet<>();
    private boolean inRun = false;

    public static class CombatSnapshot {
        public final int combatIndex;
        public final Map<String, int[]> playerData;

        public CombatSnapshot(int combatIndex, Map<String, int[]> playerData) {
            this.combatIndex = combatIndex;
            this.playerData = playerData;
        }
    }

    public void startRun() {
        inRun = true;
        runStats.clear();
        combatHistory.clear();
        eventLog.clear();
        deadPlayers.clear();
    }

    public void endRun() {
        inRun = false;
    }

    public void recordCombatEnd(CombatTracker combat) {
        Map<String, int[]> snapshot = new LinkedHashMap<>();
        for (PlayerDamageStats combatStats : combat.getAllStats()) {
            PlayerDamageStats runPlayer = runStats.computeIfAbsent(
                combatStats.getPlayerId(), PlayerDamageStats::new);
            if (combatStats.getPlayerName() != null) {
                runPlayer.setPlayerName(combatStats.getPlayerName());
            }
            if (combatStats.getCharacterName() != null) {
                runPlayer.setCharacterName(combatStats.getCharacterName());
            }

            int direct = combatStats.getCombatDirectDamage();
            int debuff = combatStats.getCombatDebuffDamage();
            int cards = combatStats.getCombatCardsPlayed();
            int teamBlock = combatStats.getCombatTeamBlock();
            int teamBuffs = combatStats.getCombatTeamBuffs();

            runPlayer.addRunDamage(direct, debuff);
            runPlayer.addRunCards(cards);
            runPlayer.addRunTeamContribution(teamBlock, teamBuffs);

            snapshot.put(combatStats.getPlayerId(),
                new int[]{direct, debuff, cards, teamBlock, teamBuffs});
        }

        int combatNum = combatHistory.size() + 1;
        combatHistory.add(new CombatSnapshot(combatNum, snapshot));

        // Build combat summary for event log
        StringBuilder sb = new StringBuilder();
        sb.append("战斗#").append(combatNum).append(" - ");
        boolean first = true;
        for (PlayerDamageStats ps : combat.getAllStats()) {
            if (!first) sb.append(" | ");
            first = false;
            String name = ps.getPlayerName() != null ? ps.getPlayerName() : ps.getPlayerId();
            sb.append(name).append(": ").append(ps.getCombatTotalDamage()).append("伤害");
        }
        eventLog.add(sb.toString());
    }

    // Dead player tracking
    public void recordDeadPlayer(String playerId) {
        deadPlayers.add(playerId);
    }

    public boolean wasDead(String playerId) {
        return deadPlayers.contains(playerId);
    }

    public void clearDead(String playerId) {
        deadPlayers.remove(playerId);
    }

    // Event log
    public void addEvent(String event) {
        eventLog.add(event);
    }

    public List<String> getEventLog() {
        return eventLog;
    }

    public Collection<PlayerDamageStats> getAllStats() {
        return runStats.values();
    }

    public List<CombatSnapshot> getCombatHistory() {
        return combatHistory;
    }

    public boolean isInRun() { return inRun; }

    public int getTotalRunDamage() {
        int total = 0;
        for (PlayerDamageStats stats : runStats.values()) {
            total += stats.getRunTotalDamage();
        }
        return total;
    }
}
