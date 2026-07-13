package damagetracker.patches;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import damagetracker.DamageTrackerMod;

import java.util.HashSet;
import java.util.Set;

public class CampfirePatch {

    private static final Set<String> deadThisCombat = new HashSet<>();

    public static void onPlayerDeathInCombat(String playerId) {
        deadThisCombat.add(playerId);
    }

    public static void clearCombatDeaths() {
        deadThisCombat.clear();
    }

    public static void onCombatEnd() {
        for (String playerId : deadThisCombat) {
            DamageTrackerMod.getRunTracker().recordDeadPlayer(playerId);
        }
        deadThisCombat.clear();
    }

    // Patch heal(int) on AbstractPlayer
    @SpirePatch(clz = AbstractPlayer.class, method = "heal", paramtypez = {int.class})
    public static class PlayerHealPostfix {
        public static void Postfix(AbstractPlayer __instance, int amount) {
            checkRevive(__instance, amount);
        }
    }

    // Patch heal(int) on AbstractMonster (covers CharacterEntity teammates)
    @SpirePatch(clz = AbstractMonster.class, method = "heal", paramtypez = {int.class})
    public static class MonsterHealPostfix {
        public static void Postfix(AbstractMonster __instance, int amount) {
            checkRevive(__instance, amount);
        }
    }

    private static void checkRevive(AbstractCreature creature, int amount) {
        if (amount <= 0) return;
        if (DamageTrackerMod.getCombatTracker().isInCombat()) return;

        String playerId = DamagePatch.resolvePlayerId(creature);
        if (playerId == null) return;

        if (DamageTrackerMod.getRunTracker().wasDead(playerId)) {
            DamageTrackerMod.getRunTracker().clearDead(playerId);
            DamageTrackerMod.onPlayerRevived(playerId);
        }
    }
}
