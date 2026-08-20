package com.extraban.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Central message and settings helper for ExtraBan.
 */
public class MessageUtils {

    private final JavaPlugin plugin;
    private final Set<String> missingKeys = new HashSet<>();

    public MessageUtils(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public String getPrefix() {
        return color(plugin.getConfig().getString("prefix", "&8[&6ExtraBan&8]&r "));
    }

    public String getDiscord() {
        return plugin.getConfig().getString("settings.general.Discord",
                plugin.getConfig().getString("settings.Discord", "https://discord.gg/"));
    }

    public String getDefaultReason() {
        return plugin.getConfig().getString("settings.general.default-reason", "Unfair advantage");
    }

    public String getDateFormat() {
        return plugin.getConfig().getString("settings.general.date-format", "yyyy-MM-dd HH:mm");
    }

    public String formatDate(Date date) {
        return new SimpleDateFormat(getDateFormat()).format(date);
    }

    public boolean isBanBroadcast() {
        if (plugin.getConfig().contains("settings.general.ban-broadcast")) {
            return plugin.getConfig().getBoolean("settings.general.ban-broadcast", true);
        }
        return plugin.getConfig().getBoolean("settings.ban-broadcast", true);
    }

    public boolean isFreezeBroadcast() {
        return plugin.getConfig().getBoolean("settings.general.freeze-broadcast", true);
    }

    public boolean isWarnBroadcast() {
        return plugin.getConfig().getBoolean("settings.general.warn-broadcast", true);
    }

    public int getBanListMaxDisplay() {
        return Math.max(1, plugin.getConfig().getInt("settings.banlist.max-display", 10));
    }

    public String getTempBanMaxDuration() {
        return plugin.getConfig().getString("settings.general.tempban-max-duration",
                plugin.getConfig().getString("settings.tempban-max-duration", "30d"));
    }

    public boolean isWarnBarrierEnabled() {
        return plugin.getConfig().getBoolean("settings.warn.barrier-enabled", true);
    }

    public int getWarnCustomModelData() {
        return plugin.getConfig().getInt("settings.warn.custom-model-data", 9999);
    }

    /**
     * Prefixed colored message from messages.&lt;path&gt;.
     */
    public String msg(String path) {
        return getPrefix() + raw(path);
    }

    public String msg(String path, String... placeholders) {
        return getPrefix() + raw(path, placeholders);
    }

    public String msg(String path, Map<String, String> placeholders) {
        return getPrefix() + raw(path, placeholders);
    }

    /**
     * Colored message without prefix (kick screens, boxed menus, action bars).
     */
    public String raw(String path) {
        String message = plugin.getConfig().getString("messages." + path);
        if (message == null) {
            warnMissing(path);
            message = "&cMissing message: " + path;
        }
        return color(message);
    }

    public String raw(String path, String... placeholders) {
        return apply(raw(path), placeholders);
    }

    public String raw(String path, Map<String, String> placeholders) {
        String message = raw(path);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}",
                        entry.getValue() != null ? entry.getValue() : "");
            }
        }
        return message;
    }

    public String setting(String path, String defaultValue) {
        return color(plugin.getConfig().getString(path, defaultValue));
    }

    public String setting(String path, String defaultValue, String... placeholders) {
        return apply(setting(path, defaultValue), placeholders);
    }

    public void send(CommandSender sender, String path) {
        sender.sendMessage(asComponent(msg(path)));
    }

    public void send(CommandSender sender, String path, String... placeholders) {
        sender.sendMessage(asComponent(msg(path, placeholders)));
    }

    public void sendRaw(CommandSender sender, String path) {
        for (String line : raw(path).split("\n")) {
            sender.sendMessage(asComponent(line));
        }
    }

    public void sendRaw(CommandSender sender, String path, String... placeholders) {
        for (String line : raw(path, placeholders).split("\n")) {
            sender.sendMessage(asComponent(line));
        }
    }

    public Component asComponent(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(input.replace('§', '&'));
    }

    public String color(String input) {
        if (input == null) {
            return "";
        }
        return LegacyComponentSerializer.legacySection().serialize(asComponent(input));
    }

    private String apply(String message, String... placeholders) {
        if (placeholders == null || placeholders.length == 0) {
            return message;
        }
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            String key = placeholders[i];
            String value = placeholders[i + 1] != null ? placeholders[i + 1] : "";
            message = message.replace("{" + key + "}", value);
        }
        return message;
    }

    private void warnMissing(String path) {
        if (missingKeys.add(path)) {
            plugin.getLogger().warning("Missing config message: messages." + path);
        }
    }

    public static String formatDuration(long durationMs) {
        if (durationMs <= 0) {
            return "0 seconds";
        }

        long days = TimeUnit.MILLISECONDS.toDays(durationMs);
        long hours = TimeUnit.MILLISECONDS.toHours(durationMs) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60;

        StringBuilder result = new StringBuilder();
        if (days > 0) {
            result.append(days).append(days == 1 ? " day" : " days");
        }
        if (hours > 0) {
            if (result.length() > 0) result.append(", ");
            result.append(hours).append(hours == 1 ? " hour" : " hours");
        }
        if (minutes > 0 && days == 0) {
            if (result.length() > 0) result.append(", ");
            result.append(minutes).append(minutes == 1 ? " minute" : " minutes");
        }
        if (result.length() == 0) {
            result.append(seconds).append(seconds == 1 ? " second" : " seconds");
        }
        return result.toString();
    }

    public static long parseDuration(String timeString) throws IllegalArgumentException {
        if (timeString == null || timeString.length() < 2) {
            throw new IllegalArgumentException("Invalid time format");
        }

        String unit = timeString.substring(timeString.length() - 1).toLowerCase();
        String numberStr = timeString.substring(0, timeString.length() - 1);

        try {
            long number = Long.parseLong(numberStr);
            return switch (unit) {
                case "s" -> number * 1000L;
                case "m" -> number * 60L * 1000L;
                case "h" -> number * 60L * 60L * 1000L;
                case "d" -> number * 24L * 60L * 60L * 1000L;
                case "w" -> number * 7L * 24L * 60L * 60L * 1000L;
                default -> throw new IllegalArgumentException("Invalid time unit: " + unit);
            };
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format: " + numberStr);
        }
    }

    public static String padText(String text, int length) {
        if (text.length() >= length) {
            return text.substring(0, length);
        }
        StringBuilder padded = new StringBuilder(length);
        padded.append(text);
        while (padded.length() < length) {
            padded.append(' ');
        }
        return padded.toString();
    }
}
