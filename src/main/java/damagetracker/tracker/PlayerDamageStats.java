package damagetracker.tracker;

import java.util.HashMap;
import java.util.Map;

public class PlayerDamageStats {
    private final String playerId;
    private String playerName;
    private String characterName;
    private String characterClass; // "IRONCLAD", "THE_SILENT", "DEFECT", "WATCHER"

    // Turn stats
    private int turnDirectDamage;
    private int turnDebuffDamage;
    private int turnEnergyUsed;
    private int turnCardsPlayed;
    private Map<String, Integer> turnDebuffContribution = new HashMap<>();

    // Combat stats
    private int combatDirectDamage;
    private int combatDebuffDamage;
    private int combatCardsPlayed;
    private int combatMaxHit;
    private Map<String, Integer> combatDebuffContribution = new HashMap<>();

    // All powers/debuffs applied by this player (for UI display)
    private Map<String, Integer> combatAppliedPowers = new HashMap<>();

    // Team contribution (given TO teammates)
    private int combatTeamBlock;
    private int combatTeamBuffs;

    // Run stats
    private int runDirectDamage;
    private int runDebuffDamage;
    private int runCardsPlayed;
    private int runTeamBlock;
    private int runTeamBuffs;

    public PlayerDamageStats(String playerId) {
        this.playerId = playerId;
    }

    // --- Record methods ---

    public void recordDirectDamage(int amount) {
        turnDirectDamage += amount;
        combatDirectDamage += amount;
        if (amount > combatMaxHit) {
            combatMaxHit = amount;
        }
    }

    public void recordDebuffDamage(int amount, String debuffType) {
        turnDebuffDamage += amount;
        combatDebuffDamage += amount;
        turnDebuffContribution.merge(debuffType, amount, Integer::sum);
        combatDebuffContribution.merge(debuffType, amount, Integer::sum);
    }

    public void recordAppliedPower(String powerId, int stacks) {
        combatAppliedPowers.merge(powerId, stacks, Integer::sum);
    }

    public void recordTeamBlock(int amount) {
        combatTeamBlock += amount;
    }

    public void recordTeamBuff(int stacks) {
        combatTeamBuffs += stacks;
    }

    public void recordCardPlayed() {
        turnCardsPlayed++;
        combatCardsPlayed++;
    }

    public void recordEnergyUsed(int amount) {
        turnEnergyUsed += amount;
    }

    // --- Run-level accumulation ---

    public void addRunDamage(int direct, int debuff) {
        runDirectDamage += direct;
        runDebuffDamage += debuff;
    }

    public void addRunCards(int cards) {
        runCardsPlayed += cards;
    }

    public void addRunTeamContribution(int block, int buffs) {
        runTeamBlock += block;
        runTeamBuffs += buffs;
    }

    // --- Reset ---

    public void resetTurn() {
        turnDirectDamage = 0;
        turnDebuffDamage = 0;
        turnEnergyUsed = 0;
        turnCardsPlayed = 0;
        turnDebuffContribution.clear();
    }

    // --- Remote sync setter (replace combat + turn damage, not accumulate) ---

    public void setRemoteCombatDamage(int combatDirect, int combatDebuff,
                                       int turnDirect, int turnDebuff) {
        this.combatDirectDamage = combatDirect;
        this.combatDebuffDamage = combatDebuff;
        this.turnDirectDamage = turnDirect;
        this.turnDebuffDamage = turnDebuff;
    }

    // --- Getters ---

    public String getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String name) { this.playerName = name; }
    public String getCharacterName() { return characterName; }
    public void setCharacterName(String name) { this.characterName = name; }
    public String getCharacterClass() { return characterClass; }
    public void setCharacterClass(String cls) { this.characterClass = cls; }

    // Turn
    public int getTurnDirectDamage() { return turnDirectDamage; }
    public int getTurnDebuffDamage() { return turnDebuffDamage; }
    public int getTurnCardsPlayed() { return turnCardsPlayed; }
    public int getTurnTotalDamage() { return turnDirectDamage + turnDebuffDamage; }
    public Map<String, Integer> getTurnDebuffContribution() { return turnDebuffContribution; }

    // Combat
    public int getCombatDirectDamage() { return combatDirectDamage; }
    public int getCombatDebuffDamage() { return combatDebuffDamage; }
    public int getCombatCardsPlayed() { return combatCardsPlayed; }
    public int getCombatMaxHit() { return combatMaxHit; }
    public int getCombatTotalDamage() { return combatDirectDamage + combatDebuffDamage; }
    public Map<String, Integer> getCombatDebuffContribution() { return combatDebuffContribution; }
    public Map<String, Integer> getCombatAppliedPowers() { return combatAppliedPowers; }
    public int getCombatTeamBlock() { return combatTeamBlock; }
    public int getCombatTeamBuffs() { return combatTeamBuffs; }

    // Run
    public int getRunTotalDamage() { return runDirectDamage + runDebuffDamage; }
    public int getRunDirectDamage() { return runDirectDamage; }
    public int getRunTotalDebuffDamage() { return runDebuffDamage; }
    public int getRunTotalCardsPlayed() { return runCardsPlayed; }
    public int getRunTeamBlock() { return runTeamBlock; }
    public int getRunTeamBuffs() { return runTeamBuffs; }
}
