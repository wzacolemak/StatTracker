package damagetracker.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import damagetracker.config.ModConfig;
import damagetracker.DamageTrackerMod;
import damagetracker.tracker.CombatTracker;
import damagetracker.tracker.PlayerDamageStats;
import damagetracker.tracker.RunTracker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class StatsOverlay {
    private static float S, W, PAD, LH, BAR_H, BORDER_W, ROW_GAP, EXTRA_LH;

    private void updateLayout(ModConfig config) {
        S = Settings.scale * config.getUiScale();
        W = 420.0f * S;
        PAD = 14.0f * S;
        LH = 26.0f * S;
        BAR_H = 3.0f * S;
        BORDER_W = 3.0f * S;
        ROW_GAP = 10.0f * S;
        EXTRA_LH = 24.0f * S;
    }

    private static final Color C_BG       = new Color(0.0f, 0.0f, 0.0f, 0.78f);
    private static final Color C_ROW      = new Color(0.12f, 0.12f, 0.18f, 0.45f);
    private static final Color C_GRAY     = new Color(0.63f, 0.66f, 0.71f, 1f);
    private static final Color C_DIM      = new Color(0.41f, 0.45f, 0.50f, 0.7f);
    private static final Color C_YELLOW   = new Color(0.98f, 0.80f, 0.08f, 1f);
    private static final Color C_CYAN     = new Color(0.13f, 0.83f, 0.93f, 1f);
    private static final Color C_PINK     = new Color(1f, 0.47f, 0.77f, 1f);
    private static final Color C_GREEN    = new Color(0.29f, 0.87f, 0.50f, 1f);
    private static final Color C_ORANGE   = new Color(0.96f, 0.65f, 0.14f, 1f);
    private static final Color C_BORDER   = new Color(0.23f, 0.23f, 0.36f, 0.5f);
    private static final Color C_BAR_TRK  = new Color(1f, 1f, 1f, 0.04f);
    private static final Color C_BTN_BG   = new Color(0.18f, 0.18f, 0.28f, 0.7f);
    private static final Color C_BTN_HOVER = new Color(0.28f, 0.28f, 0.42f, 0.9f);

    // === i18n ===
    private static String t(ModConfig cfg, String zh, String en) {
        return cfg.isEn() ? en : zh;
    }

    private static Color charTheme(String charClass) {
        if (charClass == null) return C_GRAY;
        switch (charClass) {
            case "IRONCLAD": return new Color(0.88f, 0.31f, 0.31f, 1f);
            case "THE_SILENT": return new Color(0.36f, 0.72f, 0.36f, 1f);
            case "DEFECT": return new Color(0.29f, 0.66f, 0.85f, 1f);
            case "WATCHER": return new Color(0.69f, 0.38f, 0.82f, 1f);
            default: return C_GRAY;
        }
    }

    private static String charDisplayName(String charClass, ModConfig config) {
        if (charClass == null) return "";
        boolean en = config.isEn();
        switch (charClass) {
            case "IRONCLAD": return en ? "Ironclad" : "铁甲战士";
            case "THE_SILENT": return en ? "Silent" : "静默猎手";
            case "DEFECT": return en ? "Defect" : "故障机器人";
            case "WATCHER": return en ? "Watcher" : "观者";
            default: return charClass;
        }
    }

    private static String powerCN(String id, boolean en) {
        if (en) {
            switch (id) {
                case "Weakened": return "Weak";
                case "Vulnerability": return "Vuln";
                default: return id;
            }
        }
        switch (id) {
            case "Vulnerable": return "易伤"; case "Weakened": return "虚弱";
            case "Weak": return "虚弱"; case "Poison": return "中毒";
            case "Strength": return "力量"; case "Choked": return "勒脖";
            case "Slow": return "迟缓"; case "Envenom": return "涂毒";
            case "Combust": return "燃烧"; case "Brutality": return "残暴";
            case "Constricted": return "束缚"; case "Hex": return "妖术";
            case "Omega": return "欧米伽"; case "Explosive": return "爆炸";
            case "TheBomb": return "炸弹"; case "Pen Nib": return "钢笔尖";
            case "Lockon": return "锁定"; case "Frail": return "脆弱";
            case "Entangled": return "缠绕"; case "Vulnerability": return "易伤";
            case "Double Damage": return "双倍"; case "Phantasmal": return "幻影";
            case "CorpseExplosionPower": return "尸爆"; case "Sharp Hide": return "反伤";
            case "Plated Armor": return "护甲"; case "Metallicize": return "金属化";
            case "Rage": return "愤怒"; case "Vigor": return "活力";
            case "Dexterity": return "敏捷"; case "Regen": return "再生";
            case "Buffer": return "缓冲"; case "Next Turn Block": return "下回合盾";
            case "Blur": return "模糊"; case "Barricade": return "壁垒";
            case "Intangible": return "无形"; case "Flight": return "飞行";
            case "Thorns": return "荆棘"; case "Flame Barrier": return "火焰屏障";
            case "Shackled": return "枷锁";
            default: return id;
        }
    }

    private ShapeRenderer shape;
    private boolean dragging;

    /** Get the overlay font */
    private BitmapFont font() {
        return FontHelper.tipBodyFont;
    }
    private float dragOffX, dragOffY;
    private int logScrollOffset = 0;

    // Lang toggle button bounds (recalculated each frame)
    private float langBtnX, langBtnY, langBtnW, langBtnH;
    private boolean langBtnHovered = false;

    // Zoom button bounds
    private float zoomInX, zoomInY, zoomInW, zoomInH;
    private float zoomOutX, zoomOutY, zoomOutW, zoomOutH;
    private boolean zoomInHovered = false, zoomOutHovered = false;

    // Scroll delta (read once per frame)
    private int lastScrollDelta = 0;

    private float origFontScaleX, origFontScaleY;
    private Texture.TextureFilter origMinFilter, origMagFilter;

    private void ensureFont(ModConfig config) {
        BitmapFont font = FontHelper.tipBodyFont;
        origFontScaleX = font.getData().scaleX;
        origFontScaleY = font.getData().scaleY;
        float targetScale = config.getUiScale() * Settings.scale * 0.6f;
        font.getData().setScale(targetScale);
        // Save filter from first page, then set Linear on ALL texture pages
        com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g2d.TextureRegion> pages = font.getRegions();
        if (pages.size > 0) {
            Texture tex0 = pages.get(0).getTexture();
            origMinFilter = tex0.getMinFilter();
            origMagFilter = tex0.getMagFilter();
        }
        for (int i = 0; i < pages.size; i++) {
            pages.get(i).getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
    }

    private void restoreFont() {
        BitmapFont font = FontHelper.tipBodyFont;
        font.getData().setScale(origFontScaleX, origFontScaleY);
        com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g2d.TextureRegion> pages = font.getRegions();
        for (int i = 0; i < pages.size; i++) {
            pages.get(i).getTexture().setFilter(origMinFilter, origMagFilter);
        }
    }

    public void render(SpriteBatch sb, ModConfig config, CombatTracker ct,
                       RunTracker rt, boolean isMultiplayer) {
        if (!config.isShowPanel()) return;
        if (shape == null) shape = new ShapeRenderer();

        // Read scroll once per frame
        lastScrollDelta = org.lwjgl.input.Mouse.getDWheel();
        if (lastScrollDelta != 0 && (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.CONTROL_LEFT)
            || Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.CONTROL_RIGHT))) {
            config.adjustScale(lastScrollDelta > 0 ? 0.1f : -0.1f);
            lastScrollDelta = 0;
        }

        updateLayout(config);
        ensureFont(config);
        try {
            handleDrag(config);
            handleClicks(config);

            int view = config.getCurrentView();
            if (view == 3) {
                renderLogView(sb, config, rt);
            } else if (view == 2) {
                renderBuffView(sb, config, ct);
            } else {
                renderCombatRunView(sb, config, ct, rt, view);
            }
        } finally {
            restoreFont();
        }

        try {
            if (CardCrawlGame.cursor != null) {
                CardCrawlGame.cursor.render(sb);
            }
        } catch (Exception ignored) {}
    }

    private void handleClicks(ModConfig config) {
        if (Gdx.input.justTouched() && Gdx.input.isButtonPressed(0)) {
            float mx = Gdx.input.getX();
            float my = Settings.HEIGHT - Gdx.input.getY();
            if (mx >= langBtnX && mx <= langBtnX + langBtnW &&
                my >= langBtnY && my <= langBtnY + langBtnH) {
                config.toggleLang();
            }
            if (mx >= zoomOutX && mx <= zoomOutX + zoomOutW &&
                my >= zoomOutY && my <= zoomOutY + zoomOutH) {
                config.adjustScale(-0.1f);
            }
            if (mx >= zoomInX && mx <= zoomInX + zoomInW &&
                my >= zoomInY && my <= zoomInY + zoomInH) {
                config.adjustScale(0.1f);
            }
        }
    }

    private void renderLangButton(SpriteBatch sb, ModConfig config, float rightX, float centerY) {
        String label = config.isEn() ? "中文" : "EN";
        gl.setText(font(), label);
        float btnW = gl.width + 10 * S;
        float btnH = LH;
        float btnX = rightX - btnW;
        float btnY = centerY - btnH / 2;

        // Check hover
        float mx = Gdx.input.getX();
        float my = Settings.HEIGHT - Gdx.input.getY();
        langBtnHovered = mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH;

        // Draw button background
        sb.end();
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(langBtnHovered ? C_BTN_HOVER : C_BTN_BG);
        shape.rect(btnX, btnY, btnW, btnH);
        shape.end();
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(C_BORDER);
        shape.rect(btnX, btnY, btnW, btnH);
        shape.end();
        sb.begin();

        FontHelper.renderFontCentered(sb, font(), label,
            btnX + btnW / 2, btnY + btnH / 2, C_YELLOW);

        // Store bounds for click detection
        this.langBtnX = btnX;
        this.langBtnY = btnY;
        this.langBtnW = btnW;
        this.langBtnH = btnH;
    }

    private void renderZoomButtons(SpriteBatch sb, ModConfig config, float rightX, float centerY) {
        float btnSize = 22 * S;
        float gap = 3 * S;
        float btnH = LH;

        float outX = rightX - btnSize;
        float outY = centerY - btnH / 2;
        float inX = outX - btnSize - gap;
        float inY = outY;

        float mx = Gdx.input.getX();
        float my = Settings.HEIGHT - Gdx.input.getY();

        zoomOutHovered = mx >= outX && mx <= outX + btnSize && my >= outY && my <= outY + btnH;
        zoomInHovered = mx >= inX && mx <= inX + btnSize && my >= inY && my <= inY + btnH;

        sb.end();
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(zoomOutHovered ? C_BTN_HOVER : C_BTN_BG);
        shape.rect(outX, outY, btnSize, btnH);
        shape.setColor(zoomInHovered ? C_BTN_HOVER : C_BTN_BG);
        shape.rect(inX, inY, btnSize, btnH);
        shape.end();
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(C_BORDER);
        shape.rect(outX, outY, btnSize, btnH);
        shape.rect(inX, inY, btnSize, btnH);
        shape.end();
        sb.begin();

        FontHelper.renderFontCentered(sb, font(), "-",
            outX + btnSize / 2, outY + btnH / 2, C_YELLOW);
        FontHelper.renderFontCentered(sb, font(), "+",
            inX + btnSize / 2, inY + btnH / 2, C_YELLOW);

        this.zoomOutX = outX; this.zoomOutY = outY; this.zoomOutW = btnSize; this.zoomOutH = btnH;
        this.zoomInX = inX; this.zoomInY = inY; this.zoomInW = btnSize; this.zoomInH = btnH;
    }

    // === Combat / Run View (view 0, 1) ===

    private void renderCombatRunView(SpriteBatch sb, ModConfig config, CombatTracker ct,
                                      RunTracker rt, int view) {
        Collection<PlayerDamageStats> stats = (view == 0) ? ct.getAllStats() : rt.getAllStats();

        String[] tabNames = {
            t(config, "当前战斗", "Combat"),
            t(config, "整局统计", "Run Stats"),
            t(config, "Buff统计", "Buffs"),
            t(config, "战斗日志", "Battle Log")
        };

        float headerH = PAD * 1.5f + LH + PAD;
        float colHeadH = LH + 22 * S;
        float rowsH = 0;
        if (stats.isEmpty()) {
            rowsH = LH + PAD;
        } else {
            for (PlayerDamageStats ps : stats) {
                rowsH += ROW_GAP + calcCombatRowHeight(ps, config);
            }
        }
        float footerH = LH + PAD;
        float totalH = headerH + colHeadH + rowsH + footerH;

        float x = config.getPanelX();
        float y = Settings.HEIGHT - config.getPanelY() - totalH;
        if (y < 0) y = 0;
        if (y + totalH > Settings.HEIGHT) y = Settings.HEIGHT - totalH;

        drawBg(sb, x, y, totalH);

        float curY = y + totalH - PAD * 1.5f;
        FontHelper.renderFontLeft(sb, font(), tabNames[view],
            x + PAD, curY, C_YELLOW);
        renderLangButton(sb, config, x + W - PAD, curY);
        renderZoomButtons(sb, config, langBtnX - 6 * S, curY);

        curY -= LH + PAD;
        float c1 = x + PAD;
        float c2 = x + W - PAD - 170 * S;
        float c3 = x + W - PAD - 85 * S;
        float c4 = x + W - PAD;
        if (view == 0) {
            FontHelper.renderFontLeft(sb, font(), t(config, "玩家", "Player"), c1, curY, C_DIM);
            FontHelper.renderFontRightAligned(sb, font(), t(config, "本回合", "Turn"), c2, curY, C_DIM);
            FontHelper.renderFontRightAligned(sb, font(), t(config, "总伤害", "Total"), c3, curY, C_DIM);
            FontHelper.renderFontRightAligned(sb, font(), t(config, "占比", "%"), c4, curY, C_DIM);
        } else {
            FontHelper.renderFontLeft(sb, font(), t(config, "玩家", "Player"), c1, curY, C_DIM);
            FontHelper.renderFontRightAligned(sb, font(), t(config, "总伤害", "Total"), c3, curY, C_DIM);
            FontHelper.renderFontRightAligned(sb, font(), t(config, "占比", "%"), c4, curY, C_DIM);
        }

        curY -= 12 * S;
        sb.end();
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(C_BORDER);
        shape.rect(x + PAD, curY, W - PAD * 2, 1);
        shape.end();
        sb.begin();

        curY -= 10 * S;

        if (stats.isEmpty()) {
            curY -= LH;
            FontHelper.renderFontLeft(sb, font(), t(config, "暂无数据", "No data"),
                x + PAD, curY, C_DIM);
        } else {
            int teamTotalDmg = 0;
            int maxDmg = 1;
            for (PlayerDamageStats ps : stats) {
                int d = (view == 0) ? ps.getCombatTotalDamage() : ps.getRunTotalDamage();
                teamTotalDmg += d;
                if (d > maxDmg) maxDmg = d;
            }

            for (PlayerDamageStats ps : stats) {
                float rowH = calcCombatRowHeight(ps, config);
                int dmg = (view == 0) ? ps.getCombatTotalDamage() : ps.getRunTotalDamage();
                Color theme = charTheme(ps.getCharacterClass());
                Color nameColor = new Color(
                    Math.min(1f, theme.r * 0.6f + 0.4f),
                    Math.min(1f, theme.g * 0.6f + 0.4f),
                    Math.min(1f, theme.b * 0.6f + 0.4f), 1f);

                curY -= ROW_GAP;

                sb.end();
                shape.begin(ShapeRenderer.ShapeType.Filled);
                shape.setColor(C_ROW);
                shape.rect(x + PAD * 0.5f, curY - rowH, W - PAD, rowH);
                shape.setColor(theme);
                shape.rect(x + PAD * 0.5f, curY - rowH, BORDER_W, rowH);
                float barY = curY - rowH + 4 * S;
                shape.setColor(C_BAR_TRK);
                shape.rect(x + PAD + BORDER_W, barY, W - PAD * 2 - BORDER_W, BAR_H);
                if (dmg > 0) {
                    shape.setColor(new Color(theme.r, theme.g, theme.b, 0.55f));
                    shape.rect(x + PAD + BORDER_W, barY,
                        (W - PAD * 2 - BORDER_W) * ((float) dmg / maxDmg), BAR_H);
                }
                shape.end();
                sb.begin();

                float textX = x + PAD + BORDER_W + 6 * S;
                float lineY = curY - 14 * S;

                String name = ps.getPlayerName() != null ? ps.getPlayerName() : ps.getPlayerId();
                FontHelper.renderFontLeft(sb, font(), name, textX, lineY, nameColor);

                if (view == 0) {
                    FontHelper.renderFontRightAligned(sb, font(),
                        String.valueOf(ps.getTurnDirectDamage()), c2, lineY, C_CYAN);
                    FontHelper.renderFontRightAligned(sb, font(),
                        fmtNum(dmg), c3, lineY, C_YELLOW);
                    FontHelper.renderFontRightAligned(sb, font(),
                        (teamTotalDmg > 0 ? String.format("%.0f%%", dmg * 100.0f / teamTotalDmg) : "-"),
                        c4, lineY, C_PINK);
                } else {
                    FontHelper.renderFontRightAligned(sb, font(),
                        fmtNum(dmg), c3, lineY, C_YELLOW);
                    FontHelper.renderFontRightAligned(sb, font(),
                        (teamTotalDmg > 0 ? String.format("%.0f%%", dmg * 100.0f / teamTotalDmg) : "-"),
                        c4, lineY, C_PINK);
                }

                lineY -= LH;
                String charN = charDisplayName(ps.getCharacterClass(), config);
                if (charN != null) {
                    FontHelper.renderFontLeft(sb, font(), charN, textX, lineY, theme);
                }

                if (view == 0) {
                    int teamBlock = ps.getCombatTeamBlock();
                    int teamBuffs = ps.getCombatTeamBuffs();
                    if (teamBlock > 0 || teamBuffs > 0) {
                        lineY -= EXTRA_LH;
                        StringBuilder line = new StringBuilder(t(config, "团队", "Team") + ":");
                        if (teamBlock > 0) line.append(" ").append(t(config, "护盾", "Blk")).append(teamBlock);
                        if (teamBuffs > 0) line.append(" ").append(t(config, "增益", "Buff")).append(teamBuffs);
                        FontHelper.renderFontLeft(sb, font(),
                            line.toString(), textX, lineY, C_ORANGE);
                    }
                }

                curY -= rowH;
            }
        }

        FontHelper.renderFontCentered(sb, font(),
            t(config, "[F4] 切换  [F5] 隐藏  Ctrl+滚轮缩放", "[F4] View  [F5] Hide  Ctrl+Scroll Zoom"),
            x + W / 2, y + PAD, C_DIM);
    }

    private float calcCombatRowHeight(PlayerDamageStats ps, ModConfig config) {
        float h = 14 * S + LH + LH;
        if (ps.getCombatTeamBlock() > 0 || ps.getCombatTeamBuffs() > 0) h += EXTRA_LH;
        h += 8 * S;
        return h;
    }

    // === Buff View (view 2) ===

    private void renderBuffView(SpriteBatch sb, ModConfig config, CombatTracker ct) {
        Collection<PlayerDamageStats> stats = ct.getAllStats();

        float headerH = PAD * 1.5f + LH + PAD;
        float contentH = 0;
        if (stats.isEmpty()) {
            contentH = LH + PAD;
        } else {
            for (PlayerDamageStats ps : stats) {
                contentH += ROW_GAP + calcBuffRowHeight(ps, config);
            }
        }
        float footerH = LH + PAD;
        float totalH = headerH + contentH + footerH;

        float x = config.getPanelX();
        float y = Settings.HEIGHT - config.getPanelY() - totalH;
        if (y < 0) y = 0;
        if (y + totalH > Settings.HEIGHT) y = Settings.HEIGHT - totalH;

        drawBg(sb, x, y, totalH);

        float curY = y + totalH - PAD * 1.5f;
        FontHelper.renderFontLeft(sb, font(), t(config, "Buff统计", "Buffs"),
            x + PAD, curY, C_YELLOW);
        renderLangButton(sb, config, x + W - PAD, curY);
        renderZoomButtons(sb, config, langBtnX - 6 * S, curY);

        curY -= LH + PAD;
        sb.end();
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(C_BORDER);
        shape.rect(x + PAD, curY, W - PAD * 2, 1);
        shape.end();
        sb.begin();
        curY -= 10 * S;

        if (stats.isEmpty()) {
            curY -= LH;
            FontHelper.renderFontLeft(sb, font(), t(config, "暂无数据", "No data"),
                x + PAD, curY, C_DIM);
        } else {
            for (PlayerDamageStats ps : stats) {
                float rowH = calcBuffRowHeight(ps, config);
                Color theme = charTheme(ps.getCharacterClass());

                curY -= ROW_GAP;

                sb.end();
                shape.begin(ShapeRenderer.ShapeType.Filled);
                shape.setColor(C_ROW);
                shape.rect(x + PAD * 0.5f, curY - rowH, W - PAD, rowH);
                shape.setColor(theme);
                shape.rect(x + PAD * 0.5f, curY - rowH, BORDER_W, rowH);
                shape.end();
                sb.begin();

                float textX = x + PAD + BORDER_W + 6 * S;
                float lineY = curY - 14 * S;

                String name = ps.getPlayerName() != null ? ps.getPlayerName() : ps.getPlayerId();
                FontHelper.renderFontLeft(sb, font(), name, textX, lineY,
                    new Color(Math.min(1f, theme.r * 0.6f + 0.4f),
                              Math.min(1f, theme.g * 0.6f + 0.4f),
                              Math.min(1f, theme.b * 0.6f + 0.4f), 1f));

                lineY -= LH;
                String charN = charDisplayName(ps.getCharacterClass(), config);
                if (charN != null) {
                    FontHelper.renderFontLeft(sb, font(), charN, textX, lineY, theme);
                }

                Map<String, Integer> applied = ps.getCombatAppliedPowers();
                Map<String, Integer> debuffDmg = ps.getCombatDebuffContribution();
                if (applied != null && !applied.isEmpty()) {
                    float maxWidth = W - PAD * 2 - BORDER_W - 12 * S;
                    List<String> wrappedLines = wrapBuffLines(applied, debuffDmg, maxWidth, config);
                    for (String line : wrappedLines) {
                        lineY -= EXTRA_LH;
                        FontHelper.renderFontLeft(sb, font(),
                            line, textX, lineY, C_CYAN);
                    }
                }

                int teamBlock = ps.getCombatTeamBlock();
                int teamBuffs = ps.getCombatTeamBuffs();
                if (teamBlock > 0 || teamBuffs > 0) {
                    lineY -= EXTRA_LH;
                    StringBuilder line = new StringBuilder(t(config, "团队", "Team") + ":");
                    if (teamBlock > 0) line.append(" ").append(t(config, "护盾", "Blk")).append(teamBlock);
                    if (teamBuffs > 0) line.append(" ").append(t(config, "增益", "Buff")).append(teamBuffs);
                    FontHelper.renderFontLeft(sb, font(),
                        line.toString(), textX, lineY, C_ORANGE);
                }

                curY -= rowH;
            }
        }

        FontHelper.renderFontCentered(sb, font(),
            t(config, "[F4] 切换  [F5] 隐藏  Ctrl+滚轮缩放", "[F4] View  [F5] Hide  Ctrl+Scroll Zoom"),
            x + W / 2, y + PAD, C_DIM);
    }

    private float calcBuffRowHeight(PlayerDamageStats ps, ModConfig config) {
        float h = 14 * S + LH + LH;
        Map<String, Integer> applied = ps.getCombatAppliedPowers();
        if (applied != null && !applied.isEmpty()) {
            float maxWidth = W - PAD * 2 - BORDER_W - 12 * S;
            h += wrapBuffLines(applied, ps.getCombatDebuffContribution(), maxWidth, config).size() * EXTRA_LH;
        }
        if (ps.getCombatTeamBlock() > 0 || ps.getCombatTeamBuffs() > 0) h += EXTRA_LH;
        h += 8 * S;
        return h;
    }

    // === Log View (view 3) ===

    private void renderLogView(SpriteBatch sb, ModConfig config, RunTracker rt) {
        List<String> events = rt.getEventLog();
        float maxVisible = 12;

        List<List<String>> wrappedEvents = new ArrayList<>();
        int totalLines = 0;
        float maxW = W - PAD * 2;
        for (String evt : events) {
            List<String> lines = wrapLogEvent(evt, maxW);
            wrappedEvents.add(lines);
            totalLines += lines.size();
        }

        float logH = Math.max(140 * S, Math.min(totalLines * LH + PAD * 2, maxVisible * LH + PAD * 2));

        float headerH = PAD * 1.5f + LH + PAD;
        float footerH = LH + PAD;
        float totalH = headerH + logH + footerH;

        float x = config.getPanelX();
        float y = Settings.HEIGHT - config.getPanelY() - totalH;
        if (y < 0) y = 0;
        if (y + totalH > Settings.HEIGHT) y = Settings.HEIGHT - totalH;

        handleLogScroll(config, totalLines, (int) maxVisible);

        drawBg(sb, x, y, totalH);

        float curY = y + totalH - PAD * 1.5f;
        FontHelper.renderFontLeft(sb, font(),
            t(config, "战斗日志", "Battle Log"), x + PAD, curY, C_YELLOW);
        renderLangButton(sb, config, x + W - PAD, curY);
        renderZoomButtons(sb, config, langBtnX - 6 * S, curY);

        if (logScrollOffset > 0 || totalLines > maxVisible) {
            String scrollInfo = (logScrollOffset > 0 ? " ↑" : "") +
                (logScrollOffset + Math.min(totalLines - logScrollOffset, (int) maxVisible) < totalLines ? " ↓" : "");
            if (!scrollInfo.trim().isEmpty()) {
                // Render scroll indicator to the left of lang button
                gl.setText(font(), scrollInfo);
                FontHelper.renderFontRightAligned(sb, font(),
                    scrollInfo, langBtnX - 4 * S, curY, C_DIM);
            }
        }

        curY -= LH + PAD;
        sb.end();
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(C_BORDER);
        shape.rect(x + PAD, curY, W - PAD * 2, 1);
        shape.end();
        sb.begin();

        curY -= 12 * S;

        if (events.isEmpty()) {
            curY -= LH;
            FontHelper.renderFontLeft(sb, font(),
                t(config, "暂无记录", "No records"),
                x + PAD, curY, C_DIM);
        } else {
            float bottomY = y + footerH;

            int linesToSkip = logScrollOffset;
            for (int i = wrappedEvents.size() - 1; i >= 0 && curY >= bottomY; i--) {
                List<String> lines = wrappedEvents.get(i);
                String rawEvt = events.get(i);
                boolean isCampfire = rawEvt.startsWith("篝火") || rawEvt.startsWith("Campfire");

                for (int j = lines.size() - 1; j >= 0 && curY >= bottomY; j--) {
                    if (linesToSkip > 0) {
                        linesToSkip--;
                        continue;
                    }
                    if (isCampfire) {
                        FontHelper.renderFontLeft(sb, font(), lines.get(j), x + PAD, curY, C_GREEN);
                    } else {
                        // First line of a combat entry: highlight "战斗#X" / "Combat#X"
                        String line = lines.get(j);
                        int sepIdx = line.indexOf(" - ");
                        if (j == lines.size() - 1 && sepIdx > 0) {
                            String combatLabel = line.substring(0, sepIdx);
                            String rest = line.substring(sepIdx);
                            FontHelper.renderFontLeft(sb, font(), combatLabel, x + PAD, curY, C_YELLOW);
                            gl.setText(font(), combatLabel);
                            FontHelper.renderFontLeft(sb, font(), rest, x + PAD + gl.width, curY, C_DIM);
                        } else {
                            FontHelper.renderFontLeft(sb, font(), line, x + PAD, curY, C_DIM);
                        }
                    }
                    curY -= LH;
                }
            }
        }

        FontHelper.renderFontCentered(sb, font(),
            t(config, "[F4] 切换  [F5] 隐藏  滚轮翻页  Ctrl+滚轮缩放", "[F4] View  [F5] Hide  Scroll  Ctrl+Scroll Zoom"),
            x + W / 2, y + PAD, C_DIM);
    }

    private void handleLogScroll(ModConfig config, int totalLines, int maxVisible) {
        int view = config.getCurrentView();
        if (view != 3) {
            logScrollOffset = 0;
            return;
        }
        if (lastScrollDelta != 0) {
            int direction = lastScrollDelta > 0 ? 1 : -1;
            logScrollOffset += direction;
            int maxOffset = Math.max(0, totalLines - maxVisible);
            if (logScrollOffset > maxOffset) logScrollOffset = maxOffset;
            if (logScrollOffset < 0) logScrollOffset = 0;
        }
    }

    private List<String> wrapLogEvent(String evt, float maxW) {
        List<String> result = new ArrayList<>();
        if (evt == null || evt.isEmpty()) return result;

        String[] segments = evt.split(" \\| ");

        String currentLine = null;
        for (String seg : segments) {
            if (currentLine == null) {
                currentLine = seg;
            } else {
                String test = currentLine + " | " + seg;
                gl.setText(font(), test);
                if (gl.width <= maxW) {
                    currentLine = test;
                } else {
                    result.add(currentLine);
                    currentLine = seg;
                }
            }
        }
        if (currentLine != null) {
            result.add(currentLine);
        }
        return result;
    }

    private List<String> wrapBuffLines(Map<String, Integer> applied,
                                        Map<String, Integer> debuffDmg, float maxW,
                                        ModConfig config) {
        boolean en = config.isEn();
        List<String> result = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();

        for (Map.Entry<String, Integer> e : applied.entrySet()) {
            StringBuilder unit = new StringBuilder();
            if (currentLine.length() > 0) unit.append("  ");
            unit.append(powerCN(e.getKey(), en)).append(":").append(e.getValue());
            int dmgContrib = debuffDmg != null ? debuffDmg.getOrDefault(e.getKey(), 0) : 0;
            if (dmgContrib > 0) {
                unit.append("(+").append(dmgContrib).append(")");
            }

            String test = currentLine.toString() + unit.toString();
            gl.setText(font(), test);
            if (gl.width <= maxW || currentLine.length() == 0) {
                currentLine.append(unit);
            } else {
                result.add(currentLine.toString());
                String stripped = unit.toString().trim();
                currentLine = new StringBuilder(stripped);
            }
        }
        if (currentLine.length() > 0) {
            result.add(currentLine.toString());
        }
        return result;
    }

    // === Helpers ===

    private void drawBg(SpriteBatch sb, float x, float y, float h) {
        sb.end();
        shape.setProjectionMatrix(sb.getProjectionMatrix());
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(C_BG);
        shape.rect(x, y, W, h);
        shape.end();
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(C_BORDER);
        shape.rect(x, y, W, h);
        shape.end();
        sb.begin();
    }

    private void handleDrag(ModConfig config) {
        if (Gdx.input.justTouched() && Gdx.input.isButtonPressed(0)) {
            float mx = Gdx.input.getX();
            float my = Settings.HEIGHT - Gdx.input.getY();
            float px = config.getPanelX();
            float py = Settings.HEIGHT - config.getPanelY();
            if (mx >= px && mx <= px + W && my >= py - 50 * S && my <= py + 10 * S) {
                dragging = true;
                dragOffX = mx - px;
                dragOffY = my - py;
            }
        }
        if (dragging && Gdx.input.isButtonPressed(0)) {
            float mx = Gdx.input.getX();
            float my = Settings.HEIGHT - Gdx.input.getY();
            config.setPanelX(mx - dragOffX);
            config.setPanelY(-(my - dragOffY - Settings.HEIGHT));
        }
        if (dragging && !Gdx.input.isButtonPressed(0)) {
            dragging = false;
        }
    }

    private static final GlyphLayout gl = new GlyphLayout();

    private String truncRight(String text, float maxW) {
        if (text == null) return "";
        gl.setText(font(), text);
        if (gl.width <= maxW) return text;
        while (text.length() > 0) {
            text = text.substring(0, text.length() - 1);
            gl.setText(font(), text + "..");
            if (gl.width <= maxW) return text + "..";
        }
        return "";
    }

    private static String fmtNum(int v) {
        if (v >= 1000000) return String.format("%.1fM", v / 1000000.0);
        if (v >= 10000) return String.format("%.1fk", v / 1000.0);
        return String.valueOf(v);
    }

    public void dispose() {
        if (shape != null) { shape.dispose(); shape = null; }
    }
}
