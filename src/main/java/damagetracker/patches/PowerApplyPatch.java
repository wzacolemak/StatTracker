package damagetracker.patches;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.powers.AbstractPower;
import damagetracker.DamageTrackerMod;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PowerApplyPatch {

    private static AbstractCreature pendingSource = null;

    @SpirePatch(clz = ApplyPowerAction.class, method = "update")
    public static class UpdatePrefix {
        public static void Prefix(ApplyPowerAction __instance) {
            pendingSource = __instance.source;
        }
    }

    @SpirePatch(clz = ApplyPowerAction.class, method = "update")
    public static class UpdatePostfix {
        public static void Postfix(ApplyPowerAction __instance) {
            pendingSource = null;
        }
    }

    @SpirePatch(clz = AbstractCreature.class, method = "addPower")
    public static class AddPowerPostfix {
        public static void Postfix(AbstractCreature __instance, AbstractPower power) {
            try {
                AbstractCreature source = pendingSource;
                if (source == null) return;

                String sourcePlayerId = DamagePatch.resolvePlayerId(source);
                if (sourcePlayerId == null) {
                    sourcePlayerId = DamageTrackerMod.getLastAttackerId();
                }
                if (sourcePlayerId == null) return;

                // Skip self-applied powers
                if (source == __instance) return;

                String targetId = __instance.id;
                String powerId = power.ID;
                int amount = power.amount;

                DamageTrackerMod.onPowerApplied(targetId, powerId, sourcePlayerId, amount);

                // Track team buff
                if (isBuff(powerId)) {
                    String targetPlayerId = DamagePatch.resolvePlayerId(__instance);
                    if (targetPlayerId != null && !targetPlayerId.equals(sourcePlayerId)) {
                        DamageTrackerMod.onTeamBuffGiven(sourcePlayerId, powerId, amount);
                    }
                }
            } catch (Exception e) {
                DamageTrackerMod.logger.error("[DamageTracker] PowerApplyPatch error", e);
            }
        }
    }

    private static final Set<String> DAMAGE_DEBUFFS = new HashSet<>(Arrays.asList(
        "Vulnerable", "Weakened", "Poison", "Strength",
        "Choked", "Vulnerability", "Weak", "Slow",
        "Double Damage", "Phantasmal", "Pen Nib", "Lockon",
        "Painful Stabs", "Combust", "Brutality", "Envenom",
        "CorpseExplosionPower", "Explosive", "TheBomb", "Sharp Hide",
        "Constricted", "Hex", "Omega"
    ));

    private static final Set<String> BUFF_POWERS = new HashSet<>(Arrays.asList(
        "Plated Armor", "Metallicize", "Rage", "Vigor",
        "Dexterity", "Barricade", "Blur", "Next Turn Block",
        "Regen", "Buffer", "Intangible", "Invincible",
        "Flight", "Curiosity", "Malleable", "Flame Barrier",
        "Thorns"
    ));

    public static boolean isDebuff(String powerId) {
        return DAMAGE_DEBUFFS.contains(powerId);
    }

    public static boolean isBuff(String powerId) {
        return BUFF_POWERS.contains(powerId);
    }
}
