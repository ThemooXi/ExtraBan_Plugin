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
     * Reloads config from disk after merging any new keys from the JAR defaults.
     * Used by /eb reload so updates appear without deleting the plugin folder.
     */
    public void reloadPluginConfig() {
        updateConfig();
    }

    /**
     * Updates config.yml while preserving user values and adding missing keys from the JAR.
     * When rewriting, the file is rebuilt in the same key order as the shipped config.yml.
     */
    private void updateConfig() {
        Logger pluginLogger = getLogger();

        try {
            pluginLogger.info("ExtraBan: Checking config.yml for updates...");

            File configFile = new File(this.getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                this.saveDefaultConfig();
            }

            // Live file only — never attach JAR defaults here (that broke missing-key detection).
            YamlConfiguration currentConfig = new YamlConfiguration();
            currentConfig.load(configFile);
            currentConfig.options().copyDefaults(false);

            InputStream defaultStream = this.getResource("config.yml");
            if (defaultStream == null) {
                pluginLogger.warning("ExtraBan: Default config.yml not found in JAR!");
                this.reloadConfig();
                return;
            }

            YamlConfiguration defaultConfig = new YamlConfiguration();
            try (InputStream stream = defaultStream;
                 Reader defaultReader = new InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)) {
                defaultConfig.load(defaultReader);
            }
            defaultConfig.options().copyDefaults(false);

            if (defaultConfig.getKeys(true).isEmpty()) {
                pluginLogger.severe("ExtraBan: Default config.yml from JAR is empty or failed to parse!");
                this.reloadConfig();
                return;
            }

            String currentVersion = currentConfig.getString("config-version", "0.0.0");
            String defaultVersion = defaultConfig.getString("config-version", "2.1.0");
            int currentRevision = currentConfig.getInt("config-revision", 0);
            int defaultRevision = defaultConfig.getInt("config-revision", 1);

            boolean versionOutdated = !isVersionUpToDate(currentVersion, defaultVersion);
            boolean revisionOutdated = currentRevision < defaultRevision;
            boolean hasLockedConfigEntries = hasRemovableLockedEntries(currentConfig);
            boolean needsStructureCleanup = hasLegacyStructure(currentConfig);
            boolean orderMismatch = !hasSameKeyOrder(currentConfig, defaultConfig);

            // Migrate old paths first so user values sit on the new key paths.
            boolean migrated = migrateLegacyStructure(currentConfig);
            int missingBefore = countMissingKeys(currentConfig, defaultConfig);

            if (versionOutdated) {
                currentConfig.set("config-version", defaultVersion);
            }
            if (revisionOutdated || missingBefore > 0 || migrated || versionOutdated) {
                currentConfig.set("config-revision", defaultRevision);
            }

            boolean removedLocked = removeLockedConfigEntries(currentConfig);
            boolean removedLegacy = removeLegacyStructure(currentConfig);

            boolean needsRewrite = migrated || missingBefore > 0 || versionOutdated || revisionOutdated
                    || removedLocked || removedLegacy || hasLockedConfigEntries
                    || needsStructureCleanup || orderMismatch;

            if (!needsRewrite) {
                pluginLogger.info("ExtraBan: config.yml is already up to date.");
                this.reloadConfig();
                return;
            }

            pluginLogger.info("ExtraBan: Updating config.yml (version " + currentVersion
                    + " -> " + defaultVersion + ", rev " + currentRevision + " -> " + defaultRevision
                    + ", added " + missingBefore + " key(s)"
                    + (orderMismatch ? ", fixing key order" : "") + ")");
            backupCurrentConfig(configFile, currentVersion + "-r" + currentRevision);

            // Rebuild from the JAR template so section/key order matches the shipped config.
            saveOrderedConfig(configFile, currentConfig, defaultConfig);
            pluginLogger.info("ExtraBan: config.yml successfully updated.");
            this.reloadConfig();

        } catch (Exception e) {
            pluginLogger.log(Level.SEVERE, "ExtraBan: Failed to update config.yml: " + e.getMessage(), e);
            this.reloadConfig();
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

    /**
     * Counts leaf keys present in the JAR defaults but missing from the live file.
     */
    private int countMissingKeys(FileConfiguration current, YamlConfiguration defaults) {
        int missing = 0;
        for (String key : defaults.getKeys(true)) {
            if (defaults.isConfigurationSection(key)) {
                continue;
            }
            if (!current.contains(key, true)) {
                missing++;
            }
        }
        return missing;
    }

    /**
     * True when the live file's leaf-key order (for keys shared with the JAR) differs
     * from the shipped config.yml order.
     */
    private boolean hasSameKeyOrder(FileConfiguration current, YamlConfiguration defaults) {
        List<String> defaultLeaves = leafKeys(defaults);
        List<String> currentLeaves = leafKeys(current);

        List<String> expected = new ArrayList<>();
        for (String key : defaultLeaves) {
            if (currentLeaves.contains(key)) {
                expected.add(key);
            }
        }

        List<String> actual = new ArrayList<>();
        for (String key : currentLeaves) {
            if (defaultLeaves.contains(key)) {
                actual.add(key);
            }
        }

        return expected.equals(actual);
    }

    private List<String> leafKeys(FileConfiguration config) {
        List<String> leaves = new ArrayList<>();
        for (String key : config.getKeys(true)) {
            if (!config.isConfigurationSection(key)) {
                leaves.add(key);
            }
        }
        return leaves;
    }

    /**
     * Writes config.yml using the JAR template order, then overlays the user's values.
     * New keys appear in their correct place (not dumped at the bottom of the file).
     */
    private void saveOrderedConfig(File configFile, FileConfiguration current, YamlConfiguration defaults)
            throws IOException, org.bukkit.configuration.InvalidConfigurationException {
        Logger pluginLogger = getLogger();

        // Start from the exact shipped file so key/section order matches the project config.yml.
        try (InputStream in = this.getResource("config.yml")) {
            if (in == null) {
                throw new IOException("Default config.yml missing from JAR");
            }
            Files.copy(in, configFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        YamlConfiguration ordered = new YamlConfiguration();
        try (Reader reader = Files.newBufferedReader(configFile.toPath(), java.nio.charset.StandardCharsets.UTF_8)) {
            ordered.load(reader);
        }
        ordered.options().copyDefaults(false);

        for (String key : leafKeys(ordered)) {
            if (!current.contains(key, true)) {
                pluginLogger.info("Added new config option: " + key);
                continue;
            }
            Object userValue = current.get(key);
            if (!java.util.Objects.equals(userValue, ordered.get(key))) {
                ordered.set(key, userValue);
            }
        }

        // Keep unknown custom keys the owner may have added (after the template keys).
        for (String key : leafKeys(current)) {
            if (!defaults.contains(key, true)) {
                ordered.set(key, current.get(key));
                pluginLogger.info("Preserved custom config option: " + key);
            }
        }

        ordered.save(configFile);
    }

    /** True if the live config still contains locked Discord / message sections. */
    private boolean hasRemovableLockedEntries(FileConfiguration current) {
        Set<String> keys = current.getKeys(true);
        if (keys.contains("settings.general.Discord") || keys.contains("settings.Discord")) {
            return true;
        }
        String[] lockedRoots = {
                "messages.errors",
                "messages.usage",
                "messages.help",
                "messages.banlist",
                "messages.system",
                "messages.update"
        };
        for (String root : lockedRoots) {
            if (keys.contains(root) || current.isConfigurationSection(root)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes locked branding/message keys from the server config so owners
     * cannot keep outdated editable copies that no longer apply.
     */
    private boolean removeLockedConfigEntries(FileConfiguration current) {
        Logger pluginLogger = getLogger();
        boolean changesMade = false;

        if (current.contains("settings.general.Discord")) {
            current.set("settings.general.Discord", null);
            pluginLogger.info("Removed locked config entry: settings.general.Discord");
            changesMade = true;
        }
        if (current.contains("settings.Discord")) {
            current.set("settings.Discord", null);
            pluginLogger.info("Removed locked config entry: settings.Discord");
            changesMade = true;
        }

        String[] lockedRoots = {
                "messages.errors",
                "messages.usage",
                "messages.help",
                "messages.banlist",
                "messages.system",
                "messages.update"
        };
        for (String root : lockedRoots) {
            if (current.contains(root)) {
                current.set(root, null);
                pluginLogger.info("Removed locked config section: " + root);
                changesMade = true;
            }
        }

        return changesMade;
    }

    private boolean hasLegacyStructure(FileConfiguration current) {
        return current.contains("settings.general")
                || current.contains("settings.action-bar")
                || current.contains("settings.freeze.countdown-message")
                || current.contains("settings.freeze.frozen-message-temporary")
                || current.contains("settings.freeze.frozen-message-permanent")
                || current.contains("messages.freeze.success-permanent")
                || current.contains("messages.freeze.notify-temporary")
                || current.contains("messages.freeze.action-bar-frozen-temporary")
                || current.contains("settings.ban.unban-broadcast");
    }

    /** Copies values from the old layout into the new feature-based paths. */
    private boolean migrateLegacyStructure(FileConfiguration current) {
        Logger pluginLogger = getLogger();
        boolean changesMade = false;

        changesMade |= moveConfigValue(current, "settings.general.default-reason", "settings.ban.default-reason");
        changesMade |= moveConfigValue(current, "settings.general.ban-broadcast", "settings.ban.broadcast");
        changesMade |= moveConfigValue(current, "settings.general.tempban-max-duration", "settings.ban.tempban-max-duration");
        changesMade |= moveConfigValue(current, "settings.general.date-format", "settings.ban.date-format");
        changesMade |= moveConfigValue(current, "settings.general.freeze-broadcast", "settings.freeze.broadcast");
        changesMade |= moveConfigValue(current, "settings.general.warn-broadcast", "settings.warn.broadcast");
        changesMade |= moveConfigValue(current, "settings.ban.unban-broadcast", "settings.unban.broadcast");

        changesMade |= moveConfigValue(current, "settings.action-bar.enabled", "settings.ban.action-bar.enabled");
        changesMade |= moveConfigValue(current, "settings.action-bar.countdown-time", "settings.ban.action-bar.countdown-time");
        changesMade |= moveConfigValue(current, "settings.action-bar.warning-message", "messages.ban.action-bar-warning");
        changesMade |= moveConfigValue(current, "settings.action-bar.executed-message", "messages.ban.action-bar-executed");
        changesMade |= moveConfigValue(current, "settings.action-bar.cancelled-message", "messages.ban.action-bar-cancelled");

        changesMade |= moveConfigValue(current, "settings.freeze.countdown-message", "messages.freeze.action-bar-countdown");
        changesMade |= moveConfigValue(current, "settings.freeze.frozen-message-temporary", "messages.freeze.action-bar-frozen");
        changesMade |= moveConfigValue(current, "settings.freeze.frozen-message-permanent", "messages.freeze.action-bar-frozen");

        // Old duplicated freeze message keys -> simplified keys
        changesMade |= moveConfigValue(current, "messages.freeze.success-temporary", "messages.freeze.success");
        changesMade |= moveConfigValue(current, "messages.freeze.success-permanent", "messages.freeze.success");
        changesMade |= moveConfigValue(current, "messages.freeze.broadcast-temporary", "messages.freeze.broadcast");
        changesMade |= moveConfigValue(current, "messages.freeze.broadcast-permanent", "messages.freeze.broadcast");
        changesMade |= moveConfigValue(current, "messages.freeze.notify-temporary", "messages.freeze.notify");
        changesMade |= moveConfigValue(current, "messages.freeze.notify-permanent", "messages.freeze.notify");
        changesMade |= moveConfigValue(current, "messages.freeze.action-bar-frozen-temporary", "messages.freeze.action-bar-frozen");
        changesMade |= moveConfigValue(current, "messages.freeze.action-bar-frozen-permanent", "messages.freeze.action-bar-frozen");

        if (changesMade) {
            pluginLogger.info("Migrated legacy config keys to the new structure.");
        }
        return changesMade;
    }

    private boolean moveConfigValue(FileConfiguration current, String from, String to) {
        if (!current.contains(from) || current.contains(to)) {
            return false;
        }
        current.set(to, current.get(from));
        return true;
    }

    /** Removes obsolete sections after migration. */
    private boolean removeLegacyStructure(FileConfiguration current) {
        Logger pluginLogger = getLogger();
        boolean changesMade = false;
        String[] obsolete = {
                "settings.general",
                "settings.action-bar",
                "settings.freeze.countdown-message",
                "settings.freeze.frozen-message-temporary",
                "settings.freeze.frozen-message-permanent",
                "settings.ban.unban-broadcast",
                "messages.freeze.success-permanent",
                "messages.freeze.success-temporary",
                "messages.freeze.broadcast-permanent",
                "messages.freeze.broadcast-temporary",
                "messages.freeze.notify-temporary",
                "messages.freeze.notify-permanent",
                "messages.freeze.action-bar-frozen-temporary",
                "messages.freeze.action-bar-frozen-permanent"
        };
        for (String key : obsolete) {
            if (current.contains(key)) {
                current.set(key, null);
                pluginLogger.info("Removed obsolete config entry: " + key);
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