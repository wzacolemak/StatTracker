package damagetracker.attribution;

import java.util.*;

public class DebuffTracker {
    public static class DebuffRecord {
        String playerId;
        int stacks;
        int turnsRemaining;

        DebuffRecord(String playerId, int stacks) {
            this.playerId = playerId;
            this.stacks = stacks;
            this.turnsRemaining = -1;
        }
    }

    // Map: target creature ID -> power ID -> list of (player, stacks) records
    private final Map<String, Map<String, List<DebuffRecord>>> debuffMap = new HashMap<>();

    public void recordDebuff(String targetId, String powerId, String sourcePlayerId, int stacks) {
        debuffMap
            .computeIfAbsent(targetId, k -> new HashMap<>())
            .computeIfAbsent(powerId, k -> new ArrayList<>())
            .add(new DebuffRecord(sourcePlayerId, stacks));
    }

    public void removeDebuffs(String targetId) {
        debuffMap.remove(targetId);
    }

    public Map<String, Integer> getVulnerableContribution(String targetId) {
        return getDebuffContribution(targetId, "Vulnerable");
    }

    public Map<String, Integer> getPoisonContribution(String targetId) {
        return getDebuffContribution(targetId, "Poison");
    }

    public Map<String, Integer> getDebuffContribution(String targetId, String powerId) {
        Map<String, Integer> result = new HashMap<>();
        Map<String, List<DebuffRecord>> targetDebuffs = debuffMap.get(targetId);
        if (targetDebuffs == null) return result;

        List<DebuffRecord> records = targetDebuffs.get(powerId);
        if (records == null || records.isEmpty()) return result;

        int totalStacks = 0;
        for (DebuffRecord r : records) {
            totalStacks += r.stacks;
        }
        if (totalStacks == 0) return result;

        for (DebuffRecord r : records) {
            result.put(r.playerId, r.stacks);
        }
        return result;
    }

    public boolean hasVulnerable(String targetId) {
        return hasDebuff(targetId, "Vulnerable");
    }

    public boolean hasDebuff(String targetId, String powerId) {
        Map<String, List<DebuffRecord>> targetDebuffs = debuffMap.get(targetId);
        if (targetDebuffs == null) return false;
        List<DebuffRecord> records = targetDebuffs.get(powerId);
        return records != null && !records.isEmpty();
    }

    public void clear() {
        debuffMap.clear();
    }
}
