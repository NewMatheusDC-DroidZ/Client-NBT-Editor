package com.trusted.systemnbteditor.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_DIR_NAME = "system_nbt_editor";
    private static final String THEMES_DIR_NAME = "themes";
    private static final String CONFIG_FILE_NAME = "config.json";
    
    public boolean syntaxHighlightingEnabled = true;
    public boolean customSpoofingEnabled = false;
    public String currentTheme = "";
    public boolean showEditorBackground = false;
    public TranslationProtectionMode translationProtectionMode = TranslationProtectionMode.SMART;

    public static enum TranslationProtectionMode {
        OFF,
        SIMPLE,
        SMART
    }

    // Advanced AntiCrash Settings
    public boolean entityLimitEnabled = true;
    public int entityThreshold = 2000;
    public boolean particleLimitEnabled = true;
    public int particleThreshold = 2000;
    public boolean itemNameLimitEnabled = true;
    public boolean bossbarLimitEnabled = true;
    public boolean entityNameLimitEnabled = true;
    public int lengthThreshold = 750;
    public boolean cancelFireworks = false;
    public boolean sectionFix = true;
    public boolean commandBlockLimitEnabled = true;
    public int commandBlockThreshold = 50000;
    
    // Additional AntiCrash Settings
    public boolean chatMessageLimitEnabled = true;
    public int chatMessageThreshold = 256;
    public boolean itemTooltipLimitEnabled = true;
    public int itemTooltipThreshold = 50;
    public boolean entityScaleLimitEnabled = true;
    public float entityScaleThreshold = 10.0f;
    public boolean cancelElderGuardian = false;
    public boolean tridentProtectionEnabled = true;

    // Danger Zone Modules
    public boolean bookBotEnabled = false;

    // Librarian Addon Integration
    public java.util.Set<Integer> librarianPreloadedPages = new java.util.HashSet<>();

    // Skid Feature Settings
    public boolean skidFeatureEnabled = false;
    public boolean keepSkidOnRestart = false;
    public boolean adminPermissions = false;
    public int skidMaxItemSize = 1048576; // Default to 1MB

    // Editor Mechanics & UI Integration
    public boolean showEditorSuggestions = true;
    public java.util.List<Integer> editorKeybind = new java.util.ArrayList<>(java.util.Arrays.asList(340, 32)); // Default LSHIFT + SPACE

    // NBT Editor Tweaks
    public boolean tooltipOverflowFix = true;
    public String maxEnchantLevelDisplay = "NEVER"; // NEVER, ALWAYS, NOT_MAX, NOT_EXACT
    public boolean useArabicEnchantLevels = true;
    public boolean noSlotRestrictions = true;
    public boolean enchantGlintFix = true;
    public boolean specialNumbers = true;
    public double scrollSpeed = 5.0;
    public String itemSize = "AUTO"; // HIDDEN, AUTO, BYTE, KILOBYTE, MEGABYTE, GIGABYTE, AUTO_COMPRESSED, BYTE_COMPRESSED, KILOBYTE_COMPRESSED, MEGABYTE_COMPRESSED, GIGABYTE_COMPRESSED
    public boolean clientChestButtonVisible = true;

    private static ModConfig instance;

    public static void init() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_DIR_NAME);
        File configDir = configPath.toFile();
        
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File themesDir = configPath.resolve(THEMES_DIR_NAME).toFile();
        if (!themesDir.exists()) {
            themesDir.mkdirs();
        }
        
        File configFile = new File(configDir, CONFIG_FILE_NAME);
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                instance = GSON.fromJson(reader, ModConfig.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        if (instance == null) {
            instance = new ModConfig();
            save();
        }
    }

    public static void save() {
        if (instance == null) return;
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_DIR_NAME);
        File configFile = new File(configPath.toFile(), CONFIG_FILE_NAME);
        
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(instance, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ModConfig getInstance() {
        if (instance == null) init();
        return instance;
    }
}
