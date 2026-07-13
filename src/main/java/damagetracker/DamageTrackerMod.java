package damagetracker;

import basemod.BaseMod;
import basemod.interfaces.PostInitializeSubscriber;
import basemod.interfaces.PostRenderSubscriber;
import basemod.interfaces.PostBattleSubscriber;
import basemod.interfaces.PostPowerApplySubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import damagetracker.attribution.DamageAttributor;
import damagetracker.attribution.DebuffTracker;
import damagetracker.config.ModConfig;
import damagetracker.patches.CampfirePatch;
import damagetracker.patches.TurnPatch;
import damagetracker.tracker.CombatTracker;
import damagetracker.tracker.PlayerDamageStats;
import damagetracker.tracker.RunTracker;
import damagetracker.ui.StatsOverlay;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

@SpireInitializer
public class DamageTrackerMod implements PostInitializeSubscriber, PostRenderSubscriber, PostBattleSubscriber, PostPowerApplySubscriber {
    public static final Logger logger = LogManager.getLogger("StatTracker");
    public static final String MOD_ID = "StatTracker";

    private static ModConfig config;
    private static CombatTracker combatTracker;
    private static RunTracker runTracker;
    private static DebuffTracker debuffTracker;
    private static DamageAttributor damageAttributor;
    private static StatsOverlay overlay;

    private static String lastAttackerId = null;
    private static boolean isMultiplayer = false;
    private static boolean tisClassLoaded = false;
    private static boolean initialized = false;
    private static int combatCount = 0;
    /** Cached P2P self username for consistent player identification in multiplayer */
    private static String localP2PName = null;

    public DamageTrackerMod() {
        BaseMod.subscribe(this);
        logger.info("[StatTracker] Constructor called, subscribed to BaseMod");
    }

    public static void initialize() {
        logger.info("[StatTracker] SpireInitializer initialize() called");
        new DamageTrackerMod();
    }

    @Override
    public void receivePostInitialize() {
        doInitialize();
    }

    private static void doInitialize() {
        logger.info("[StatTracker] receivePostInitialize START");
        try {
            String configDir = getConfigDir();
            logger.info("[StatTracker] Config dir: {}", configDir);

            config = new ModConfig(configDir);
            logger.info("[StatTracker] ModConfig created");

            combatTracker = new CombatTracker();
            logger.info("[StatTracker] CombatTracker created");

            runTracker = new RunTracker();
            runTracker.startRun();
            logger.info("[StatTracker] RunTracker created and started");

            debuffTracker = new DebuffTracker();
            damageAttributor = new DamageAttributor(debuffTracker);
            logger.info("[StatTracker] DebuffTracker + DamageAttributor created");

            overlay = new StatsOverlay();
            logger.info("[StatTracker] StatsOverlay created");

            tisClassLoaded = checkTisLoaded();
            initialized = true;
            logger.info("[StatTracker] Initialization COMPLETE, TIS loaded: {}", tisClassLoaded);
        } catch (Exception e) {
            logger.error("[StatTracker] FAILED to initialize!", e);
        }
    }

    private static boolean firstRender = true;

    @Override
    public void receivePostRender(SpriteBatch sb) {
        if (!initialized) return;
        if (firstRender) {
            firstRender = false;
            logger.info("[StatTracker] receivePostRender first call OK");
        }
        // Detect event-triggered combats (e.g. Dead Adventurer) that bypass room entry patches
        try {
            AbstractRoom room = AbstractDungeon.getCurrRoom();
            if (room != null && room.phase == AbstractRoom.RoomPhase.COMBAT && !combatTracker.isInCombat()) {
                logger.info("[StatTracker] Detected event-triggered combat from render");
                onCombatStart();
            }
        } catch (Exception ignored) {}
        try {
            config.handleInput();
            overlay.render(sb, config, combatTracker, runTracker, isMultiplayer);
        } catch (Exception e) {
            logger.error("[StatTracker] Render error", e);
        }
    }

    @Override
    public void receivePostBattle(AbstractRoom room) {
        onCombatEnd();
    }

    @Override
    public void receivePostPowerApplySubscriber(AbstractPower power, AbstractCreature target, AbstractCreature source) {
        ensureInitialized();
        try {
            if (!combatTracker.isInCombat()) return;
            if (target == null || power == null || source == null) return;

            String sourcePlayerId = resolvePlayerId(source);
            if (sourcePlayerId == null) return; // source is not a player

            // Skip self-applied powers
            if (source == target) return;

            String targetPlayerId = resolvePlayerId(target);
            String targetId = target.id;
            String powerId = power.ID;
            int amount = power.amount;

            // Only track debuffs applied TO enemies (target is not a player)
            if (targetPlayerId == null) {
                logger.info("[StatTracker] Power applied: {} -> {}, power={}, stacks={}",
                    sourcePlayerId, targetId, powerId, amount);
                onPowerApplied(targetId, powerId, sourcePlayerId, amount);
            }

            // Track team buff: player gives buff to a different teammate
            if (targetPlayerId != null && !targetPlayerId.equals(sourcePlayerId)) {
                onTeamBuffGiven(sourcePlayerId, powerId, amount);
            }
        } catch (Exception e) {
            logger.error("[StatTracker] receivePostPowerApplySubscriber error", e);
        }
    }

    // === Called from Patches ===

    private static void ensureInitialized() {
        if (!initialized) {
            logger.warn("[StatTracker] Lazy init triggered");
            doInitialize();
        }
    }

    public static void onCombatStart() {
        ensureInitialized();
        try {
            if (combatTracker.isInCombat()) {
                logger.info("[StatTracker] Previous combat not ended cleanly, force-ending");
                onCombatEnd();
            }

            combatCount++;
            refreshMultiplayerState();
            localP2PName = null;
            combatTracker.startCombat();
            debuffTracker.clear();
            TurnPatch.initFirstTurn();
            CampfirePatch.clearCombatDeaths();

            if (AbstractDungeon.player != null) {
                // In multiplayer, use P2P self username so all players see the same name
                String localName;
                if (isMultiplayer) {
                    localP2PName = getP2PSelfUsername();
                    localName = localP2PName != null ? localP2PName : AbstractDungeon.player.name;
                } else {
                    localName = AbstractDungeon.player.name;
                }
                PlayerDamageStats local = combatTracker.getOrCreateStats(localName);
                local.setPlayerName(localName);
                local.setCharacterName(AbstractDungeon.player.getLocalizedCharacterName());
                local.setCharacterClass(AbstractDungeon.player.chosenClass.name());
            }

            if (isMultiplayer) {
                registerMultiplayerPlayers();
            }
            logger.info("========== Combat #{} START (multiplayer={}) ==========", combatCount, isMultiplayer);
        } catch (Exception e) {
            logger.error("[StatTracker] onCombatStart error", e);
        }
    }

    public static void onCombatEnd() {
        ensureInitialized();
        try {
            if (!combatTracker.isInCombat()) return;
            combatTracker.endCombat();
            runTracker.recordCombatEnd(combatTracker);
            CampfirePatch.onCombatEnd();
            debuffTracker.clear();
            TurnPatch.reset();
            logger.info("========== Combat #{} END (run total: {}) ==========", combatCount, runTracker.getTotalRunDamage());
        } catch (Exception e) {
            logger.error("[StatTracker] onCombatEnd error", e);
        }
    }

    public static void onTurnStart() {
        ensureInitialized();
        try {
            if (combatTracker.isInCombat()) {
                combatTracker.nextTurn();
                // Broadcast cumulative stats at turn start so late-joining teammates can catch up
                broadcastLocalStats();
            }
        } catch (Exception e) {
            logger.error("[StatTracker] onTurnStart error", e);
        }
    }

    public static void onDamageDealt(String attackerId, Object target, int totalDamage) {
        ensureInitialized();
        try {
            if (!combatTracker.isInCombat()) return;

            String targetId = resolveTargetId(target);
            int baseDamage = damageAttributor.splitVulnerableDamage(
                combatTracker, targetId, totalDamage);
            PlayerDamageStats stats = combatTracker.getOrCreateStats(attackerId);
            stats.recordDirectDamage(baseDamage);

            // Broadcast to teammates if this is the local player
            broadcastLocalStats();
        } catch (Exception e) {
            logger.error("[StatTracker] onDamageDealt error", e);
        }
    }

    public static void onPoisonDamage(String targetId, int amount) {
        ensureInitialized();
        try {
            if (!combatTracker.isInCombat()) return;
            damageAttributor.attributePoisonDamage(combatTracker, targetId, amount);
            broadcastLocalStats();
        } catch (Exception e) {
            logger.error("[StatTracker] onPoisonDamage error", e);
        }
    }

    public static void onPowerApplied(String targetId, String powerId,
                                       String sourcePlayerId, int stacks) {
        ensureInitialized();
        try {
            if (!combatTracker.isInCombat()) return;
            logger.info("[StatTracker] onPowerApplied: target={}, power={}, source={}, stacks={}",
                targetId, powerId, sourcePlayerId, stacks);
            if (damagetracker.patches.PowerApplyPatch.isDebuff(powerId)) {
                debuffTracker.recordDebuff(targetId, powerId, sourcePlayerId, stacks);
            }
            PlayerDamageStats stats = combatTracker.getOrCreateStats(sourcePlayerId);
            stats.recordAppliedPower(powerId, stacks);
            broadcastLocalStats();
        } catch (Exception e) {
            logger.error("[StatTracker] onPowerApplied error", e);
        }
    }

    // Team contribution: block given TO teammates
    public static void onTeamBlockGiven(String sourcePlayerId, int amount) {
        ensureInitialized();
        try {
            if (!combatTracker.isInCombat()) return;
            PlayerDamageStats stats = combatTracker.getOrCreateStats(sourcePlayerId);
            stats.recordTeamBlock(amount);
        } catch (Exception e) {
            logger.error("[StatTracker] onTeamBlockGiven error", e);
        }
    }

    // Team contribution: buffs given TO teammates
    public static void onTeamBuffGiven(String sourcePlayerId, String powerId, int stacks) {
        ensureInitialized();
        try {
            if (!combatTracker.isInCombat()) return;
            PlayerDamageStats stats = combatTracker.getOrCreateStats(sourcePlayerId);
            stats.recordTeamBuff(stacks);
            logger.info("[StatTracker] Team buff: {} gave {} ({}x{}) to teammate",
                sourcePlayerId, powerId, stacks);
        } catch (Exception e) {
            logger.error("[StatTracker] onTeamBuffGiven error", e);
        }
    }

    // Campfire revive event
    public static void onPlayerRevived(String playerId) {
        ensureInitialized();
        try {
            runTracker.addEvent("篝火 - " + playerId + " 被复活");
            logger.info("[StatTracker] Player revived at campfire: {}", playerId);
        } catch (Exception e) {
            logger.error("[StatTracker] onPlayerRevived error", e);
        }
    }

    public static String resolvePlayerId(com.megacrit.cardcrawl.core.AbstractCreature creature) {
        return damagetracker.patches.DamagePatch.resolvePlayerId(creature);
    }

    /** Check if a monster has a damage-triggering debuff applied by a player */
    private static final String[] MONSTER_DEBUFFS = {"CorpseExplosionPower", "Explosive", "TheBomb"};

    /** Debuffs on monsters that cause HP_LOSS self-damage (Choked reduces card play damage).
     *  Note: Combust/Brutality are powers on the PLAYER, not debuffs on monsters;
     *  their THORNS damage is now handled in DamagePatch.resolveBurnPlayer(). */
    private static final String[] SELF_DAMAGE_DEBUFFS = {"Choked"};

    public static String resolveMonsterDebuff(String sourceMonsterId) {
        try {
            for (String debuff : MONSTER_DEBUFFS) {
                Map<String, Integer> contrib = debuffTracker.getDebuffContribution(sourceMonsterId, debuff);
                if (contrib.isEmpty()) continue;
                String best = null;
                int bestStacks = 0;
                for (Map.Entry<String, Integer> e : contrib.entrySet()) {
                    if (e.getValue() > bestStacks) {
                        bestStacks = e.getValue();
                        best = e.getKey();
                    }
                }
                if (best != null) return best;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    /** Check if a monster's self-damage debuffs were applied by a player (Choked, etc.) */
    public static String resolveSelfDamageDebuff(String targetId) {
        try {
            for (String debuff : SELF_DAMAGE_DEBUFFS) {
                Map<String, Integer> contrib = debuffTracker.getDebuffContribution(targetId, debuff);
                if (contrib.isEmpty()) continue;
                String best = null;
                int bestStacks = 0;
                for (Map.Entry<String, Integer> e : contrib.entrySet()) {
                    if (e.getValue() > bestStacks) {
                        bestStacks = e.getValue();
                        best = e.getKey();
                    }
                }
                if (best != null) return best;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    // === Multiplayer ===

    private static boolean checkTisLoaded() {
        try {
            Class.forName("spireTogether.network.P2P.P2PManager");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static void refreshMultiplayerState() {
        if (!tisClassLoaded) {
            isMultiplayer = false;
            return;
        }
        try {
            Class<?> modClass = Class.forName("spireTogether.network.P2P.P2PManager");
            try {
                java.lang.reflect.Field field = modClass.getField("isConnected");
                isMultiplayer = field.getBoolean(null);
            } catch (NoSuchFieldException e) {
                isMultiplayer = (AbstractDungeon.player != null);
            }
        } catch (Exception e) {
            isMultiplayer = false;
        }
    }

    private static String getP2PSelfUsername() {
        if (!tisClassLoaded) return null;
        try {
            Object self = Class.forName("spireTogether.network.P2P.P2PManager")
                .getMethod("GetSelf").invoke(null);
            if (self != null) {
                return (String) self.getClass().getField("username").get(self);
            }
        } catch (Exception e) {
            logger.error("[StatTracker] Failed to get P2P self username", e);
        }
        return null;
    }

    private static void registerMultiplayerPlayers() {
        try {
            Class<?> p2pManagerClass = Class.forName("spireTogether.network.P2P.P2PManager");
            java.lang.reflect.Method getAllPlayers = p2pManagerClass.getMethod("GetAllPlayers", boolean.class);
            Object result = getAllPlayers.invoke(null, false);

            if (result instanceof java.util.Iterator) {
                @SuppressWarnings("unchecked")
                java.util.Iterator<Object> iter = (java.util.Iterator<Object>) result;
                while (iter.hasNext()) {
                    Object p2pPlayer = iter.next();
                    String displayName = (String) p2pPlayer.getClass().getField("username").get(p2pPlayer);
                    PlayerDamageStats stats = combatTracker.getOrCreateStats(displayName);
                    stats.setPlayerName(displayName);

                    // Get character class
                    Object playerClass = p2pPlayer.getClass().getField("playerClass").get(p2pPlayer);
                    if (playerClass != null) {
                        String classId = playerClass.toString();
                        stats.setCharacterClass(classId);
                        stats.setCharacterName(playerClassToName(classId));
                    }

                    logger.info("[StatTracker] Registered multiplayer player: {} ({})",
                        displayName, stats.getCharacterName());
                }
            }
        } catch (Exception e) {
            logger.error("[StatTracker] Failed to register multiplayer players", e);
        }
    }

    private static String playerClassToName(String playerClass) {
        if (playerClass == null) return null;
        switch (playerClass) {
            case "IRONCLAD": return "铁甲战士";
            case "THE_SILENT": return "静默猎手";
            case "DEFECT": return "故障机器人";
            case "WATCHER": return "观者";
            default: return playerClass;
        }
    }

    private static String resolveTargetId(Object target) {
        if (target instanceof com.megacrit.cardcrawl.core.AbstractCreature) {
            return ((com.megacrit.cardcrawl.core.AbstractCreature) target).id;
        }
        return "unknown";
    }

    private static String getConfigDir() {
        try {
            return Gdx.files.getLocalStoragePath();
        } catch (Exception e) {
            return System.getProperty("user.home", ".");
        }
    }

    // === Getters ===

    public static String getLastAttackerId() { return lastAttackerId; }
    public static void setLastAttackerId(String id) { lastAttackerId = id; }
    public static CombatTracker getCombatTracker() { return combatTracker; }
    public static RunTracker getRunTracker() { return runTracker; }
    public static ModConfig getModConfig() { return config; }
    public static boolean isMultiplayer() { return isMultiplayer; }
    public static String getLocalP2PName() { return localP2PName; }

    public static int getMultiplayerCount() {
        if (!tisClassLoaded || !isMultiplayer) return 1;
        try {
            Object result = Class.forName("spireTogether.network.P2P.P2PManager")
                .getMethod("GetPlayerCount").invoke(null);
            if (result instanceof Integer) return (Integer) result;
        } catch (Exception e) {}
        return 1;
    }

    // === Multiplayer Data Sync ===

    /** Broadcast local player's combat stats to teammates via TIS ExtraData.
     *  Each player only broadcasts their OWN stats to avoid stale data overwriting. */
    public static void broadcastLocalStats() {
        if (!isMultiplayer || !tisClassLoaded) return;
        try {
            Object self = Class.forName("spireTogether.network.P2P.P2PManager")
                .getMethod("GetSelf").invoke(null);
            if (self == null) return;

            // Use P2P username for local player (consistent across all clients)
            String localName = localP2PName != null ? localP2PName : AbstractDungeon.player.name;
            PlayerDamageStats localStats = combatTracker.getOrCreateStats(localName);
            String dmgData = localStats.getCombatDirectDamage() + ","
                + localStats.getCombatDebuffDamage() + ","
                + localStats.getTurnDirectDamage() + ","
                + localStats.getTurnDebuffDamage();
            self.getClass().getMethod("UpdateExtraData", String.class, Object.class, boolean.class)
                .invoke(self, "st_dmg", dmgData, true);

            logger.info("[StatTracker] Broadcast stats: {} (as {})", dmgData, localName);
        } catch (Exception e) {
            logger.error("[StatTracker] broadcastLocalStats error", e);
        }
    }

    /** Called when receiving remote player's damage stats from ExtraData sync */
    public static void onRemoteStatsUpdate(String playerName, int combatDirect, int combatDebuff,
                                            int turnDirect, int turnDebuff) {
        ensureInitialized();
        try {
            if (!combatTracker.isInCombat()) return;
            // Only update stats for players we already know about.
            // Remote st_all may contain player names from their perspective
            // that don't match our registered names, causing ghost entries.
            if (!combatTracker.hasStats(playerName)) {
                return;
            }
            PlayerDamageStats stats = combatTracker.getOrCreateStats(playerName);
            stats.setRemoteCombatDamage(combatDirect, combatDebuff, turnDirect, turnDebuff);
            logger.info("[StatTracker] Remote stats: {} -> combat={}/{}, turn={}/{}",
                playerName, combatDirect, combatDebuff, turnDirect, turnDebuff);
        } catch (Exception e) {
            logger.error("[StatTracker] onRemoteStatsUpdate error", e);
        }
    }

    /** Called when receiving remote player's power application */
    public static void onRemotePowerUpdate(String playerName, String powerId, int stacks) {
        ensureInitialized();
        try {
            if (!combatTracker.isInCombat()) return;
            PlayerDamageStats stats = combatTracker.getOrCreateStats(playerName);
            stats.recordAppliedPower(powerId, stacks);
        } catch (Exception e) {
            logger.error("[StatTracker] onRemotePowerUpdate error", e);
        }
    }
}
