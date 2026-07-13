package damagetracker.patches;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.rooms.*;
import damagetracker.DamageTrackerMod;

public class CombatPatch {

    @SpirePatch(clz = MonsterRoom.class, method = "onPlayerEntry")
    public static class OnCombatStartMonsterRoom {
        public static void Postfix(MonsterRoom __instance) {
            DamageTrackerMod.onCombatStart();
        }
    }

    @SpirePatch(clz = MonsterRoomElite.class, method = "onPlayerEntry")
    public static class OnCombatStartElite {
        public static void Postfix(MonsterRoomElite __instance) {
            DamageTrackerMod.onCombatStart();
        }
    }

    @SpirePatch(clz = MonsterRoomBoss.class, method = "onPlayerEntry")
    public static class OnCombatStartBoss {
        public static void Postfix(MonsterRoomBoss __instance) {
            DamageTrackerMod.onCombatStart();
        }
    }

    @SpirePatch(clz = AbstractRoom.class, method = "endBattle")
    public static class OnCombatEnd {
        public static void Postfix(AbstractRoom __instance) {
            DamageTrackerMod.onCombatEnd();
        }
    }
}
