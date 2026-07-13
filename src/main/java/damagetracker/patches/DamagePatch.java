package damagetracker.patches;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import damagetracker.DamageTrackerMod;
import damagetracker.patches.CampfirePatch;

public class DamagePatch {

    @SpirePatch(clz = AbstractMonster.class, method = "damage", paramtypez = {DamageInfo.class})
    public static class MonsterPrefix {
        public static SpireReturn Prefix(AbstractMonster __instance, DamageInfo info) {
            // Save CorpseExplosionPower info BEFORE damage.
            // Boss die() may remove debuff powers during death processing (e.g. AwakenedOne),
            // so hasPower() in Postfix would fail. Check here while powers still exist.
            prefixCePlayer = null;
            if (__instance.hasPower("CorpseExplosionPower")) {
                prefixCePlayer = DamageTrackerMod.resolveMonsterDebuff(__instance.id);
            }
            if (__instance.currentHealth > 0) {
                processDamage(__instance, info);
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = AbstractMonster.class, method = "damage", paramtypez = {DamageInfo.class})
    public static class MonsterPostfix {
        public static void Postfix(AbstractMonster __instance, DamageInfo info) {
            if (__instance.currentHealth <= 0) {
                // Promote prefix-saved info to pending explosion player
                if (prefixCePlayer != null) {
                    pendingExplosionPlayer = prefixCePlayer;
                    explosionAttributionCount = 0;
                }
                String playerId = resolvePlayerId(__instance);
                if (playerId != null) {
                    CampfirePatch.onPlayerDeathInCombat(playerId);
                }
            }
        }
    }

    @SpirePatch(clz = AbstractPlayer.class, method = "damage", paramtypez = {DamageInfo.class})
    public static class PlayerPrefix {
        public static SpireReturn Prefix(AbstractPlayer __instance, DamageInfo info) {
            if (__instance.currentHealth > 0) {
                processDamage(__instance, info);
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = AbstractPlayer.class, method = "damage", paramtypez = {DamageInfo.class})
    public static class PlayerPostfix {
        public static void Postfix(AbstractPlayer __instance, DamageInfo info) {
            if (__instance.currentHealth <= 0) {
                CampfirePatch.onPlayerDeathInCombat(__instance.name);
            }
        }
    }

    // === Pending explosion attribution ===
    // CorpseExplosionPower.onDeath creates DamageAllEnemiesAction(null, ..., THORNS).
    // Since source is null, we track which player applied the power and attribute subsequent THORNS damage.
    private static String pendingExplosionPlayer = null;
    private static int explosionAttributionCount = 0;
    // Saved in Prefix (before damage), promoted to pending in Postfix (after death confirmed)
    private static String prefixCePlayer = null;

    // Player powers that create DamageAllEnemiesAction(null, ..., THORNS):
    // CombustPower.atEndOfTurn, FireBreathingPower.onCardDraw, OmegaPower.atEndOfTurn
    // all use null owner. (Panache/ThousandCuts use the player as owner, so they are
    // attributed via the non-null owner path and are NOT listed here.)
    private static final String[] BURN_POWERS = {"Combust", "Fire Breathing", "OmegaPower"};

    // Relics that create null-owner THORNS damage via DamageAllEnemiesAction:
    //   Charon's Ashes  - atTurnStart, 3 damage to all enemies
    //   Letter Opener   - every 3rd SKILL played, 5 damage to all enemies
    //   Mercury Hourglass - atTurnStart, 3 damage to all enemies
    //   Stone Calendar  - atTurnStart, 20+ damage to all enemies (conditional)
    private static final String[] BURN_RELICS = {
        "Charon's Ashes", "Letter Opener", "Mercury Hourglass", "StoneCalendar"
    };

    private static void processDamage(AbstractCreature target, DamageInfo info) {
        int damageDealt = info.output;
        if (damageDealt <= 0) return;

        String attackerId = resolvePlayerId(info.owner);

        // --- HP_LOSS ---
        // ChokePower.onUseCard: LoseHPAction(owner, null, amount)
        //   -> LoseHPAction.update() calls target.damage(new DamageInfo(null, amount, HP_LOSS))
        // CorpseExplosionPower (some variants) may also use HP_LOSS
        if (info.type == DamageInfo.DamageType.HP_LOSS) {
            if (target instanceof AbstractMonster) {
                if (attackerId != null) {
                    DamageTrackerMod.onDamageDealt(attackerId, target, damageDealt);
                } else {
                    String debuffPlayer = DamageTrackerMod.resolveSelfDamageDebuff(target.id);
                    if (debuffPlayer != null) {
                        DamageTrackerMod.onDamageDealt(debuffPlayer, target, damageDealt);
                    }
                }
            }
            return;
        }

        // --- THORNS ---
        // Poison: uses HP_LOSS type via PoisonLoseHpAction, handled in HP_LOSS block above.
        // The THORNS block handles: CorpseExplosionPower, CombustPower, FireBreathingPower,
        // Lightning Orb, Thorns power, Flame Barrier.
        if (info.type == DamageInfo.DamageType.THORNS) {
            boolean isExplosionDamage = info.owner == null && pendingExplosionPlayer != null;

            // 1. CorpseExplosionPower: DamageAllEnemiesAction(null, ..., THORNS)
            if (isExplosionDamage) {
                DamageTrackerMod.onDamageDealt(pendingExplosionPlayer, target, damageDealt);
                explosionAttributionCount++;
                if (explosionAttributionCount > 10) {
                    pendingExplosionPlayer = null;
                    explosionAttributionCount = 0;
                }
            }
            // 2. Player burn powers (Combust, Fire Breathing) create DamageAllEnemiesAction(null, ..., THORNS)
            else if (info.owner == null) {
                String burnPlayer = resolveBurnPlayer();
                if (burnPlayer != null) {
                    DamageTrackerMod.onDamageDealt(burnPlayer, target, damageDealt);
                }
            }
            // 3. Lightning Orb / Thorns / Flame Barrier: owner is a player
            else if (attackerId != null) {
                DamageTrackerMod.onDamageDealt(attackerId, target, damageDealt);
            }
            // 4. Monster-sourced THORNS (shouldn't happen normally but just in case)
            else if (info.owner instanceof AbstractMonster) {
                attackerId = DamageTrackerMod.resolveMonsterDebuff(info.owner.id);
                if (attackerId != null) {
                    DamageTrackerMod.onDamageDealt(attackerId, target, damageDealt);
                }
            }
            return;
        }

        // --- NORMAL ---
        // Source is a monster — check for attributed debuffs
        if (attackerId == null && info.owner instanceof AbstractMonster) {
            attackerId = DamageTrackerMod.resolveMonsterDebuff(info.owner.id);
        }
        // Pending explosion fallback for NORMAL type
        if (attackerId == null && info.owner == null && pendingExplosionPlayer != null) {
            attackerId = pendingExplosionPlayer;
            explosionAttributionCount++;
            if (explosionAttributionCount > 10) {
                pendingExplosionPlayer = null;
                explosionAttributionCount = 0;
            }
        }

        if (attackerId != null) {
            DamageTrackerMod.setLastAttackerId(attackerId);
            DamageTrackerMod.onDamageDealt(attackerId, target, damageDealt);
        }
    }

    public static String resolvePlayerId(AbstractCreature creature) {
        if (creature == null) return null;

        try {
            Class<?> charEntityClass = Class.forName("spireTogether.monsters.CharacterEntity");
            if (charEntityClass.isInstance(creature)) {
                Integer playerId = (Integer) charEntityClass.getField("playerID").get(creature);
                if (playerId != null) {
                    Class<?> p2pManagerClass = Class.forName("spireTogether.network.P2P.P2PManager");
                    Object player = p2pManagerClass.getMethod("GetPlayer", Integer.class).invoke(null, playerId);
                    if (player != null) {
                        return (String) player.getClass().getField("username").get(player);
                    }
                }
            }
        } catch (Exception e) {
            // Not a character entity or Together in Spire not available
        }

        if (creature instanceof AbstractPlayer) {
            String p2pName = DamageTrackerMod.getLocalP2PName();
            if (p2pName != null) return p2pName;
            return creature.name;
        }

        return null;
    }

    /** Check if the local player has a burn power or relic that creates
     *  null-owner THORNS damage via DamageAllEnemiesAction. */
    private static String resolveBurnPlayer() {
        try {
            AbstractPlayer player = AbstractDungeon.player;
            if (player == null) return null;
            for (String powerId : BURN_POWERS) {
                if (player.hasPower(powerId)) {
                    return resolvePlayerId(player);
                }
            }
            for (String relicId : BURN_RELICS) {
                if (player.hasRelic(relicId)) {
                    return resolvePlayerId(player);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
