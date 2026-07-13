package damagetracker.patches;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.actions.GameActionManager;
import damagetracker.DamageTrackerMod;

public class TurnPatch {

    private static int lastTurn = -1;

    @SpirePatch(clz = GameActionManager.class, method = "update")
    public static class OnTurnChange {
        public static void Postfix(GameActionManager __instance) {
            int currentTurn = __instance.turn;
            if (currentTurn != lastTurn && currentTurn > 0) {
                if (lastTurn > 0) {
                    DamageTrackerMod.onTurnStart();
                }
                lastTurn = currentTurn;
            }
        }
    }

    public static void reset() {
        lastTurn = -1;
    }

    public static void initFirstTurn() {
        lastTurn = 0;
    }
}
