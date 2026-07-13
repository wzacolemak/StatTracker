package damagetracker.patches;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import damagetracker.DamageTrackerMod;

public class BlockPatch {

    @SpirePatch(clz = GainBlockAction.class, method = "update")
    public static class TeamBlockPostfix {
        public static void Postfix(GainBlockAction __instance) {
            AbstractCreature source = __instance.source;
            AbstractCreature target = __instance.target;
            if (source == null || target == null) return;
            if (source == target) return;

            String sourceId = DamagePatch.resolvePlayerId(source);
            String targetId = DamagePatch.resolvePlayerId(target);
            if (sourceId == null || targetId == null) return;
            if (sourceId.equals(targetId)) return;

            int amount = __instance.amount;
            if (amount > 0) {
                DamageTrackerMod.onTeamBlockGiven(sourceId, amount);
            }
        }
    }
}
