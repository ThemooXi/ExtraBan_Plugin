package com.extraban.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Built-in messages and branding that cannot be changed from config.yml.
 */
public final class LockedMessages {

    public static final String DISCORD = "https://discord.gg/fJTSA6vnVQ";

    private static final Map<String, String> MESSAGES;

    static {
        Map<String, String> map = new HashMap<>();

        // Errors
        map.put("errors.no-permission", "&cYou don't have permission to do that.");
        map.put("errors.player-not-found", "&cPlayer &f{player} &cwas not found or is offline.");
        map.put("errors.player-not-banned", "&cPlayer &f{player} &cis not banned.");
        map.put("errors.already-frozen", "&cPlayer &f{player} &cis already frozen.");
        map.put("errors.not-frozen", "&cPlayer &f{player} &cis not frozen.");
        map.put("errors.invalid-time-format", "&cInvalid time format. Use &e1s&c, &e1m&c, &e1h&c, &e1d&c, or &e1w&c.");
        map.put("errors.time-examples", "&7Examples: &f1h&7, &f2d&7, &f30m&7, &f1w");
        map.put("errors.duration-too-long", "&cDuration exceeds the maximum allowed (&e{duration}&c).");
        map.put("errors.update-disabled", "&eUpdate checker is disabled in the configuration.");
        map.put("errors.update-check-started", "&eChecking for updates... Results will appear in the console.");
        map.put("errors.update-check-failed", "&cCould not check for updates. Please try again later.");
        map.put("errors.frozen-inventory", "&cYou cannot open inventories while frozen.");
        map.put("errors.warn-offline", "&cThat player must be online to receive a warning.");
        map.put("errors.warn-none", "&c{player} has no warnings on record.");
        map.put("errors.warn-barrier-drop", "&cYou cannot drop a warning barrier.");
        map.put("errors.warn-barrier-swap", "&cYou cannot swap a warning barrier.");

        // Usage
        map.put("usage.ban", "&eUsage: &f/eb ban <player> [reason]");
        map.put("usage.kick", "&eUsage: &f/eb kick <player> [reason]");
        map.put("usage.unban", "&eUsage: &f/eb unban <player>");
        map.put("usage.tban", "&eUsage: &f/eb tban <player> <time> [reason]");
        map.put("usage.freeze", "&eUsage: &f/eb freeze <player> [time] [reason]");
        map.put("usage.unfreeze", "&eUsage: &f/eb unfreeze <player>");
        map.put("usage.warn", "&eUsage: &f/eb warn <player> [reason]");
        map.put("usage.unwarn", "&eUsage: &f/eb unwarn <player>");
        map.put("usage.warn-list", "&eUsage: &f/eb warn list <player>");

        // Help
        map.put("help.header", "&8&m---------------&r &6ExtraBan Help &8&m---------------");
        map.put("help.blank", " ");
        map.put("help.section-ban", "&c&lBan");
        map.put("help.ban", "&e /eb ban <player> [reason] &8- &7Permanent ban");
        map.put("help.kick", "&e /eb kick <player> [reason] &8- &7Kick a player");
        map.put("help.unban", "&e /eb unban <player> &8- &7Remove a ban");
        map.put("help.tban", "&e /eb tban <player> <time> [reason] &8- &7Temporary ban");
        map.put("help.section-freeze", "&b&lFreeze");
        map.put("help.freeze", "&e /eb freeze <player> [time] [reason] &8- &7Freeze a player");
        map.put("help.unfreeze", "&e /eb unfreeze <player> &8- &7Unfreeze a player");
        map.put("help.section-warn", "&6&lWarn");
        map.put("help.warn", "&e /eb warn <player> [reason] &8- &7Warn a player");
        map.put("help.unwarn", "&e /eb unwarn <player> &8- &7Remove last warning");
        map.put("help.section-utility", "&a&lUtility");
        map.put("help.banlist", "&e /eb banlist &8- &7View banned players");
        map.put("help.reload", "&e /eb reload &8- &7Reload configuration");
        map.put("help.update", "&e /eb update &8- &7Check for updates");
        map.put("help.help", "&e /eb help &8- &7Show this menu");
        map.put("help.version", "&e /eb version &8- &7Show plugin version");
        map.put("help.time-formats", "&7Time formats: &f1s &8| &f1m &8| &f1h &8| &f1d &8| &f1w");
        map.put("help.aliases", "&7Aliases: &f/extraban&8, &f/eban&8, &f/extrab");
        map.put("help.footer", "&8&m---------------------------------------------");

        // Ban list
        map.put("banlist.empty", "&aThere are currently no banned players.");
        map.put("banlist.header", "&8&m---------------&r &6Banned Players &8&m---------------");
        map.put("banlist.total", "&7Total: &a{count}");
        map.put("banlist.separator", "&8&m---------------------------------------------");
        map.put("banlist.more", "&7... and &f{count} &7more (showing top entries).");
        map.put("banlist.permanent", "&c{player} &8» &7Permanent &8| &f{reason}");
        map.put("banlist.temporary", "&c{player} &8» &7Expires &f{expiry} &8| &f{reason}");
        map.put("banlist.tip", "&7Tip: &e/eb unban <player> &7to remove a ban.");
        map.put("banlist.footer", "&8&m---------------------------------------------");

        // System
        map.put("system.reload-success", "&aConfiguration reloaded successfully.");
        map.put("system.version-info", "&6ExtraBan &av{version} &8» &7Advanced moderation suite");

        // Update
        map.put("update.available-header", "&8&m---------------------------------------------");
        map.put("update.available-title", "&eA new update is available!");
        map.put("update.available-current", "&7Current &8» &f{current}");
        map.put("update.available-latest", "&7Latest &8» &a{latest}");
        map.put("update.available-url", "&b{url}");
        map.put("update.available-footer", "&8&m---------------------------------------------");
        map.put("update.latest-header", "&8&m---------------------------------------------");
        map.put("update.latest-title", "&aYou are running the latest version.");
        map.put("update.latest-version", "&7Version &8» &f{current}");
        map.put("update.latest-footer", "&8&m---------------------------------------------");

        MESSAGES = Collections.unmodifiableMap(map);
    }

    private LockedMessages() {
    }

    public static boolean isLocked(String path) {
        return MESSAGES.containsKey(path);
    }

    public static String get(String path) {
        return MESSAGES.get(path);
    }

    public static Set<String> paths() {
        return MESSAGES.keySet();
    }
}
