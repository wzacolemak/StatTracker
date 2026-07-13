package damagetracker.attribution;

import damagetracker.tracker.CombatTracker;
import damagetracker.tracker.PlayerDamageStats;

import java.util.Map;

public class DamageAttributor {

    private static final float VULNERABLE_MULTIPLIER = 0.5f;
    private final DebuffTracker debuffTracker;

    public DamageAttributor(DebuffTracker debuffTracker) {
        this.debuffTracker = debuffTracker;
    }

    /**
     * When a player deals damage to a vulnerable target, split the damage:
     * - baseDamage -> attacker's direct damage
     * - bonusDamage -> debuff contribution attributed to vulnerable applier(s)
     *
     * Returns the baseDamage (without vulnerable bonus) so the caller records
     * only the correct amount as direct damage.
     */
    public int splitVulnerableDamage(CombatTracker combatTracker,
                                      String targetId, int totalDamage) {
        if (!debuffTracker.hasVulnerable(targetId)) return totalDamage;

        int baseDamage = Math.round(totalDamage / (1.0f + VULNERABLE_MULTIPLIER));
        int bonusDamage = totalDamage - baseDamage;

        if (bonusDamage <= 0) return totalDamage;

        Map<String, Integer> vulnContrib = debuffTracker.getVulnerableContribution(targetId);
        int totalStacks = 0;
        for (int stacks : vulnContrib.values()) {
            totalStacks += stacks;
        }
        if (totalStacks == 0) return totalDamage;

        distributeBonus(combatTracker, vulnContrib, totalStacks, bonusDamage, "Vulnerable");
        return baseDamage;
    }

    /**
     * Attribute poison damage to the player(s) who applied poison.
     */
    public void attributePoisonDamage(CombatTracker combatTracker,
                                       String targetId, int poisonDamage) {
        Map<String, Integer> poisonContrib = debuffTracker.getPoisonContribution(targetId);
        if (poisonContrib.isEmpty()) return;

        int totalStacks = 0;
        for (int stacks : poisonContrib.values()) {
            totalStacks += stacks;
        }
        if (totalStacks == 0) return;

        distributeBonus(combatTracker, poisonContrib, totalStacks, poisonDamage, "Poison");
    }

    /**
     * Distribute bonus damage among contributors proportional to their stacks.
     * Remainder from integer division goes to the largest contributor.
     */
    private void distributeBonus(CombatTracker combatTracker,
                                  Map<String, Integer> contrib,
                                  int totalStacks, int bonus, String debuffType) {
        int attributed = 0;
        String largestContributor = null;
        int maxStacks = 0;

        for (Map.Entry<String, Integer> entry : contrib.entrySet()) {
            String playerId = entry.getKey();
            int stacks = entry.getValue();

            if (stacks > maxStacks) {
                maxStacks = stacks;
                largestContributor = playerId;
            }

            int share = (bonus * stacks) / totalStacks;
            if (share > 0) {
                PlayerDamageStats stats = combatTracker.getOrCreateStats(playerId);
                stats.recordDebuffDamage(share, debuffType);
                attributed += share;
            }
        }

        int remainder = bonus - attributed;
        if (remainder > 0 && largestContributor != null) {
            PlayerDamageStats stats = combatTracker.getOrCreateStats(largestContributor);
            stats.recordDebuffDamage(remainder, debuffType);
        }
    }
}
