package damagetracker.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.powers.AbstractPower;
import damagetracker.DamageTrackerMod;
import spireTogether.network.P2P.P2PCallbacks;
import spireTogether.network.P2P.P2PManager;
import spireTogether.network.P2P.P2PPlayer;
import spireTogether.monsters.CharacterEntity;

/**
 * Patches Together in Spire's P2PCallbacks to sync stats between players.
 *
 * Strategy:
 * - Damage: Each player broadcasts their own verified damage via ExtraData.
 *   Teammates receive OnPlayerExtraDataChanged and update the stat panel.
 * - Buffs: OnMonsterPowerAdded fires when a remote player applies a power.
 */
public class TisCallbackPatch {

    /** Receive remote player's combat stats via ExtraData sync */
    @SpirePatch(clz = P2PCallbacks.class, method = "OnPlayerExtraDataChanged")
    public static class OnExtraDataChanged {
        @SpirePostfixPatch
        public static void Postfix(P2PPlayer player, String key, Object oldVal, Object newVal) {
            if (player == null || key == null) return;
            if (!DamageTrackerMod.getCombatTracker().isInCombat()) return;

            String playerName = player.username;
            if (playerName == null || playerName.isEmpty()) return;

            switch (key) {
                case "st_dmg":
                    // newVal = "combatDirect,combatDebuff,turnDirect,turnDebuff"
                    try {
                        String[] parts = ((String) newVal).split(",");
                        int combatDirect = Integer.parseInt(parts[0]);
                        int combatDebuff = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                        int turnDirect = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
                        int turnDebuff = parts.length > 3 ? Integer.parseInt(parts[3]) : 0;
                        DamageTrackerMod.onRemoteStatsUpdate(playerName, combatDirect, combatDebuff,
                            turnDirect, turnDebuff);
                    } catch (Exception e) {
                        DamageTrackerMod.logger.error("[StatTracker] Failed to parse st_dmg", e);
                    }
                    break;
                case "st_pwr":
                    // newVal = "powerId:stacks" as String
                    try {
                        String[] parts = ((String) newVal).split(":", 2);
                        DamageTrackerMod.onRemotePowerUpdate(playerName, parts[0],
                            parts.length > 1 ? Integer.parseInt(parts[1]) : 0);
                    } catch (Exception e) {
                        DamageTrackerMod.logger.error("[StatTracker] Failed to parse st_pwr", e);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /** Track powers (debuffs/buffs) applied by remote players to monsters */
    @SpirePatch(clz = P2PCallbacks.class, method = "OnMonsterPowerAdded")
    public static class OnMonsterPower {
        @SpirePostfixPatch
        public static void Postfix(AbstractCreature target, AbstractPower power, CharacterEntity character) {
            if (character == null || target == null || power == null) return;
            if (!DamageTrackerMod.getCombatTracker().isInCombat()) return;

            try {
                Integer playerId = character.playerID;
                if (playerId == null) return;

                // Skip local player — already tracked via PostPowerApplySubscriber
                P2PPlayer self = P2PManager.GetSelf();
                if (self != null && playerId.equals(self.GetID())) return;

                P2PPlayer player = P2PManager.GetPlayer(playerId);
                if (player == null) return;

                String playerName = player.username;
                DamageTrackerMod.onPowerApplied(target.id, power.ID, playerName, power.amount);
                DamageTrackerMod.logger.info("[StatTracker] Remote power: {} applied {} ({}x{}) to {}",
                    playerName, power.ID, power.amount, "", target.id);
            } catch (Exception e) {
                DamageTrackerMod.logger.error("[StatTracker] OnMonsterPower error", e);
            }
        }
    }
}
