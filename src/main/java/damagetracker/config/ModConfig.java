package damagetracker.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

import java.io.*;
import java.util.Properties;

public class ModConfig {
    private static final String CONFIG_FILE = "DamageTrackerConfig.properties";

    private int toggleKey = Input.Keys.F5;
    private int nextViewKey = Input.Keys.F4;
    private float panelX = 10.0f;
    private float panelY = 400.0f;
    private float panelAlpha = 0.85f;
    private boolean showPanel = false;
    private int currentView = 0;
    private static final int VIEW_COUNT = 4;
    private String lang = "zh"; // "zh" or "en"
    private float uiScale = 1.0f; // 0.5 ~ 2.0

    private final String configPath;

    public ModConfig(String configDir) {
        this.configPath = new File(configDir, CONFIG_FILE).getAbsolutePath();
        load();
    }

    public void load() {
        File f = new File(configPath);
        if (!f.exists()) return;
        try (InputStream is = new FileInputStream(f)) {
            Properties props = new Properties();
            props.load(is);
            toggleKey = Integer.parseInt(props.getProperty("toggleKey", String.valueOf(toggleKey)));
            nextViewKey = Integer.parseInt(props.getProperty("nextViewKey", String.valueOf(nextViewKey)));
            panelX = Float.parseFloat(props.getProperty("panelX", String.valueOf(panelX)));
            panelY = Float.parseFloat(props.getProperty("panelY", String.valueOf(panelY)));
            panelAlpha = Float.parseFloat(props.getProperty("panelAlpha", String.valueOf(panelAlpha)));
            uiScale = Float.parseFloat(props.getProperty("uiScale", String.valueOf(uiScale)));
            if (uiScale < 0.5f) uiScale = 0.5f;
            if (uiScale > 2.0f) uiScale = 2.0f;
            lang = props.getProperty("lang", lang);
            if (!lang.equals("zh") && !lang.equals("en")) lang = "zh";
        } catch (Exception e) {
            // Use defaults
        }
    }

    public void save() {
        Properties props = new Properties();
        props.setProperty("toggleKey", String.valueOf(toggleKey));
        props.setProperty("nextViewKey", String.valueOf(nextViewKey));
        props.setProperty("panelX", String.valueOf(panelX));
        props.setProperty("panelY", String.valueOf(panelY));
        props.setProperty("panelAlpha", String.valueOf(panelAlpha));
        props.setProperty("uiScale", String.valueOf(uiScale));
        props.setProperty("lang", lang);
        try {
            File f = new File(configPath);
            f.getParentFile().mkdirs();
            try (OutputStream os = new FileOutputStream(f)) {
                props.store(os, "DamageTracker Mod Configuration");
            }
        } catch (Exception e) {
            // Silently fail
        }
    }

    private static org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(ModConfig.class);

    public void handleInput() {
        if (Gdx.input.isKeyJustPressed(toggleKey)) {
            showPanel = !showPanel;
            log.info("[StatTracker] Toggle key pressed! showPanel={}", showPanel);
        }
        if (showPanel && Gdx.input.isKeyJustPressed(nextViewKey)) {
            currentView = (currentView + 1) % VIEW_COUNT;
        }
    }

    // Getters and setters
    public boolean isShowPanel() { return showPanel; }
    public void setShowPanel(boolean show) { this.showPanel = show; }
    public int getCurrentView() { return currentView; }
    public int getToggleKey() { return toggleKey; }
    public void setToggleKey(int key) { this.toggleKey = key; save(); }
    public int getNextViewKey() { return nextViewKey; }
    public void setNextViewKey(int key) { this.nextViewKey = key; save(); }
    public float getPanelX() { return panelX; }
    public void setPanelX(float x) { this.panelX = x; }
    public float getPanelY() { return panelY; }
    public void setPanelY(float y) { this.panelY = y; }
    public float getPanelAlpha() { return panelAlpha; }
    public void setPanelAlpha(float alpha) { this.panelAlpha = alpha; }
    public String getLang() { return lang; }
    public void toggleLang() { this.lang = "zh".equals(this.lang) ? "en" : "zh"; save(); }
    public boolean isEn() { return "en".equals(lang); }
    public float getUiScale() { return uiScale; }
    public void setUiScale(float s) { this.uiScale = Math.max(0.5f, Math.min(2.0f, s)); save(); }
    public void adjustScale(float delta) { setUiScale(Math.round((uiScale + delta) * 10.0f) / 10.0f); }
}
