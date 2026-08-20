package com.extraban;

import com.extraban.commands.ExtraBanCommand;
import com.extraban.commands.ExtraBanTabCompleter;
import com.extraban.commands.FreezeManager;
import com.extraban.commands.WarnManager;
import com.extraban.utils.BanUtils;
import com.extraban.utils.MessageUtils;
import com.extraban.utils.SimpleUpdateChecker;
import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.BanEntry;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main plugin class.
 * Initializes commands, managers, and configuration handling.
 */
public class Main extends JavaPlugin {

    private static Main instance;
    private FreezeManager freezeManager;
    private SimpleUpdateChecker updateChecker;
    private WarnManager warnManager;
    private MessageUtils messages;

    // ASCII art displayed on startup
    private final String PLUGIN_ART = """
            
            •    ███████╗██╗  ██╗████████╗██████╗  █████╗ ██████╗  █████╗ ███╗   ██╗    •
            •    ██╔════╝╚██╗██╔╝╚══██╔══╝██╔══██╗██╔══██╗██╔══██╗██╔══██╗████╗  ██║    •
            •    █████╗   ╚███╔╝    ██║   ██████╔╝███████║██████╔╝███████║██╔██╗ ██║    •
            •    ██╔══╝   ██╔██╗    ██║   ██╔══██╗██╔══██║██╔══██╗██╔══██║██║╚██╗██║    •
            •    ███████╗██╔╝ ██╗   ██║   ██║  ██║██║  ██║██████╔╝██║  ██║██║ ╚████║    •
            •    ╚══════╝╚═╝  ╚═╝   ╚═╝   ╚═╝  ╚═╝╚═╝  ╚═╝╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═══╝    •
            """;

    @Override
    public void onEnable() {
        try {
            instance = this;

            setupConfig();
            messages = new MessageUtils(this);

            freezeManager = new FreezeManager(this);
            warnManager = new WarnManager(this);

            ExtraBanCommand commandExecutor = new ExtraBanCommand(this);
            ExtraBanTabCompleter tabCompleter = new ExtraBanTabCompleter();

            Objects.requireNonNull(this.getCommand("eb")).setExecutor(commandExecutor);
            Objects.requireNonNull(this.getCommand("eb")).setTabCompleter(tabCompleter);

            showStartupMessage();
            startScheduledTasks();
            startUpdateChecker();

            getLogger().info("✓ ExtraBan plugin enabled successfully!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable ExtraBan: ", e);
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        this.saveConfig();
        getLogger().info("ExtraBan: Plugin disabled!");
    }

    /** Loads and migrates config.yml on startup. */
    private void setupConfig() {
        try {
            this.saveDefaultConfig();
            updateConfig();
            getLogger().info("✓ Config initialized successfully");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize config: ", e);
        }
    }

    /**
     * Updates config.yml while preserving user values and adding missing keys from the JAR default.
     */
    private void updateConfig() {
        Logger pluginLogger = getLogger();

        try {
            pluginLogger.info("ExtraBan: Checking config.yml for updates...");

            File configFile = new File(this.getDataFolder(), "config.yml");
            FileConfiguration currentConfig = YamlConfiguration.loadConfiguration(configFile);

            InputStream defaultStream = this.getResource("config.yml");
            if (defaultStream == null) {
                pluginLogger.warning("ExtraBan: Default config.yml not found in JAR!");
                return;
            }

            YamlConfiguration defaultConfig;
            try (InputStream stream = defaultStream;
                 Reader defaultReader = new InputStreamReader(stream)) {
                defaultConfig = YamlConfiguration.loadConfiguration(defaultReader);
            }

            String currentVersion = currentConfig.getString("config-version", "0.0.0");
            String defaultVersion = defaultConfig.getString("config-version", "2.0.0");

            pluginLogger.info("Config versions: Current: " + currentVersion + ", Default: " + defaultVersion);

            boolean missingKeys = hasMissingKeys(currentConfig, defaultConfig);
            boolean versionOutdated = !isVersionUpToDate(currentVersion, defaultVersion);

            if (!missingKeys && !versionOutdated) {
                pluginLogger.info("ExtraBan: config.yml is already up to date.");
                this.reloadConfig();
                return;
            }

            if (versionOutdated || missingKeys) {
                pluginLogger.info("ExtraBan: Updating config.yml (version " + currentVersion
                        + " -> " + defaultVersion + ", missing keys: " + missingKeys + ")");
                backupCurrentConfig(configFile, currentVersion);
            }

            boolean changesMade = mergeMissingKeys(currentConfig, defaultConfig);

            if (versionOutdated) {
                currentConfig.set("config-version", defaultVersion);
                changesMade = true;
            }

            if (changesMade) {
                currentConfig.save(configFile);
                this.reloadConfig();
                pluginLogger.info("ExtraBan: config.yml successfully updated to version " + defaultVersion);
            }

        } catch (Exception e) {
            pluginLogger.log(Level.SEVERE, "ExtraBan: Failed to update config.yml: " + e.getMessage(), e);
        }
    }

    /** Returns true if the current version is greater than or equal to the default version. */
    private boolean isVersionUpToDate(String currentVersion, String defaultVersion) {
        try {
            String[] currentParts = currentVersion.replaceAll("[^\\d.]", "").split("\\.");
            String[] defaultParts = defaultVersion.replaceAll("[^\\d.]", "").split("\\.");

            int maxLength = Math.max(currentParts.length, defaultParts.length);

            for (int i = 0; i < maxLength; i++) {
                int currentPart = i < currentParts.length && !currentParts[i].isEmpty()
                        ? Integer.parseInt(currentParts[i]) : 0;
                int defaultPart = i < defaultParts.length && !defaultParts[i].isEmpty()
                        ? Integer.parseInt(defaultParts[i]) : 0;

                if (currentPart > defaultPart) return true;
                if (currentPart < defaultPart) return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return currentVersion.equals(defaultVersion);
        }
    }

    private void backupCurrentConfig(File configFile, String version) {
        Logger pluginLogger = getLogger();
        try {
            if (!configFile.exists()) {
                return;
            }

            File backupsFolder = new File(this.getDataFolder(), "config-backups");
            if (!backupsFolder.exists() && !backupsFolder.mkdirs()) {
                pluginLogger.warning("Failed to create backups folder!");
                return;
            }

            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File backupFile = new File(backupsFolder, "config-v" + version + "-" + timestamp + ".yml");

            Files.copy(configFile.toPath(), backupFile.toPath());
            pluginLogger.info("Config backup created: " + backupFile.getName());

        } catch (IOException e) {
            pluginLogger.log(Level.WARNING, "Failed to create config backup: ", e);
        }
    }

    /** Checks whether the current config is missing any keys from the default config. */
    private boolean hasMissingKeys(FileConfiguration current, YamlConfiguration defaults) {
        for (String key : defaults.getKeys(true)) {
            if (!defaults.isConfigurationSection(key) && !current.contains(key)) {
                return true;
            }
        }
        return false;
    }

    /** Adds only missing keys while preserving existing user values. */
    private boolean mergeMissingKeys(FileConfiguration current, YamlConfiguration defaults) {
        boolean changesMade = false;
        Logger pluginLogger = getLogger();

        for (String key : defaults.getKeys(true)) {
            if (defaults.isConfigurationSection(key)) {
                continue;
            }
            if (!current.contains(key)) {
                current.set(key, defaults.get(key));
                pluginLogger.info("Added new config option: " + key);
                changesMade = true;
            }
        }

        return changesMade;
    }

    /** Displays the startup banner in the console. */
    private void showStartupMessage() {
        String version = this.getPluginMeta().getVersion();
        String author = "m_1z.4";
        String minecraftVersion = "26.2+";

        System.out.print(PLUGIN_ART);
        System.out.print("\n");
        System.out.print("•    ╔══════════════════════════════════════════════╗\n");
        System.out.print("•    ║    " + MessageUtils.padText("EXTRABAN v" + version, 36) + "      ║\n");
        System.out.print("•    ║    " + MessageUtils.padText("Ban & Moderation System", 36) + "      ║\n");
        System.out.print("•    ╠══════════════════════════════════════════════╣\n");
        System.out.print("•    ║    " + MessageUtils.padText("Developer: " + author, 36) + "      ║\n");
        System.out.print("•    ║    " + MessageUtils.padText("Minecraft Version: " + minecraftVersion, 36) + "      ║\n");
        System.out.print("•    ║    " + MessageUtils.padText("Status: Enabled", 36) + "      ║\n");
        System.out.print("•    ╚══════════════════════════════════════════════╝\n");
        System.out.print("\n");
    }

    private void startScheduledTasks() {
        getLogger().info("Starting scheduled maintenance tasks...");
        Bukkit.getScheduler().runTaskTimer(this, this::cleanExpiredBans, 0L, 12000L);
        getLogger().info("✓ Scheduled tasks started");
    }

    /**
     * Removes expired temporary bans from the ban list.
     * Runs periodically to clean up expired entries.
     */
    private void cleanExpiredBans() {
        try {
            int removedCount = 0;
            Instant now = Instant.now();

            for (BanEntry<PlayerProfile> ban : new HashSet<>(BanUtils.entries())) {
                Date expiration = ban.getExpiration();
                if (expiration != null && now.isAfter(expiration.toInstant())) {
                    BanUtils.list().pardon(ban.getBanTarget());
                    removedCount++;
                }
            }

            if (removedCount > 0) {
                getLogger().info("✓ Cleaned " + removedCount + " expired ban(s)");
            }
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Error during ban cleanup: ", e);
        }
    }

    /** Starts the update checker system. */
    private void startUpdateChecker() {
        if (!getConfig().getBoolean("update-checker.enabled", true)) {
            getLogger().info("Update checker is disabled in config.");
            return;
        }

        updateChecker = new SimpleUpdateChecker(this);

        if (getConfig().getBoolean("update-checker.check-on-startup", true)) {
            Bukkit.getScheduler().runTaskLater(this, () -> updateChecker.checkForUpdates(), 60L);
        }
    }

    // --- Getters ---

    /** Returns the singleton Main instance. */
    public static Main getInstance() {
        return instance;
    }

    /** Returns the freeze manager. */
    public FreezeManager getFreezeManager() {
        return freezeManager;
    }

    /** Returns the update checker. */
    public SimpleUpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    /** Returns the warning manager. */
    public WarnManager getWarnManager() {
        return warnManager;
    }

    public MessageUtils getMessages() {
        return messages;
    }
}