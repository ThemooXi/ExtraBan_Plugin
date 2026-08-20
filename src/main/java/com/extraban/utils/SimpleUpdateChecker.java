package com.extraban.utils;

import com.extraban.Main;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class SimpleUpdateChecker implements Listener {

    private final Main plugin;
    private final MessageUtils messages;
    private final Logger logger;
    private final String currentVersion;
    private String latestVersion;
    private boolean updateAvailable = false;

    private static final String SPIGOT_RESOURCE_ID = "119078";
    private static final String SPIGOT_API =
            "https://api.spiget.org/v2/resources/" + SPIGOT_RESOURCE_ID + "/versions?size=1&sort=-releaseDate";
    private static final String SPIGOT_URL =
            "https://www.spigotmc.org/resources/exstraban-plugin." + SPIGOT_RESOURCE_ID + "/";

    public SimpleUpdateChecker(Main plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessages();
        this.logger = plugin.getLogger();
        this.currentVersion = plugin.getPluginMeta().getVersion();
        this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void checkForUpdates() {
        if (!plugin.getConfig().getBoolean("update-checker.enabled", true)) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                boolean success = checkSpigotUpdates();
                if (!plugin.getConfig().getBoolean("update-checker.notify-console", true)) {
                    return;
                }
                if (updateAvailable) {
                    sendUpdateAvailableMessage();
                } else if (success) {
                    sendLatestVersionMessage();
                }
            } catch (Exception ignored) {
            }
        });
    }

    private boolean checkSpigotUpdates() {
        try {
            URL url = URI.create(SPIGOT_API).toURL();
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("User-Agent", "ExtraBan-UpdateChecker");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            JsonArray versionsArray = JsonParser.parseString(response.toString()).getAsJsonArray();
            if (versionsArray.isEmpty()) {
                return false;
            }

            JsonObject versionObj = versionsArray.get(0).getAsJsonObject();
            if (!versionObj.has("name") || versionObj.get("name").isJsonNull()) {
                return false;
            }

            String cleanedVersion = extractVersionNumber(versionObj.get("name").getAsString());
            if (cleanedVersion.isEmpty()) {
                return false;
            }

            this.latestVersion = cleanedVersion;
            if (isNewerVersion(latestVersion, currentVersion)) {
                this.updateAvailable = true;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String extractVersionNumber(String versionName) {
        if (versionName == null) return "";
        String cleaned = versionName.replaceAll("[^0-9.]", "").trim();
        cleaned = cleaned.replaceAll("^\\.+|\\.+$", "");
        return cleaned.matches(".*\\d+.*") ? cleaned : "";
    }

    private boolean isNewerVersion(String newVersion, String current) {
        try {
            String[] newParts = newVersion.split("\\.");
            String[] currentParts = current.split("\\.");
            for (int i = 0; i < Math.max(newParts.length, currentParts.length); i++) {
                int newPart = i < newParts.length && !newParts[i].isEmpty()
                        ? Integer.parseInt(newParts[i]) : 0;
                int currentPart = i < currentParts.length && !currentParts[i].isEmpty()
                        ? Integer.parseInt(currentParts[i]) : 0;
                if (newPart > currentPart) return true;
                if (newPart < currentPart) return false;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void sendUpdateAvailableMessage() {
        logger.info("========================================");
        logger.info("[ExtraBan] A NEW UPDATE IS AVAILABLE!");
        logger.info("[ExtraBan] Current Version: " + currentVersion);
        logger.info("[ExtraBan] Latest Version: " + latestVersion);
        logger.info("[ExtraBan] Download here: " + SPIGOT_URL);
        logger.info("========================================");
    }

    private void sendLatestVersionMessage() {
        logger.info("========================================");
        logger.info("[ExtraBan] You are running the latest version: " + currentVersion);
        logger.info("========================================");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getConfig().getBoolean("update-checker.notify-staff", true)) {
            return;
        }
        if (player.hasPermission("extraban.update") && updateAvailable) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> sendIngameUpdateMessage(player), 40L);
        }
    }

    private void sendIngameUpdateMessage(Player player) {
        messages.sendRaw(player, "update.available-header");
        messages.send(player, "update.available-title");
        messages.send(player, "update.available-current", "current", currentVersion);
        messages.send(player, "update.available-latest", "latest", latestVersion);
        messages.send(player, "update.available-url", "url", SPIGOT_URL);
        messages.sendRaw(player, "update.available-footer");
    }

    public void manualCheck(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = checkSpigotUpdates();
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (updateAvailable) {
                    sendIngameUpdateMessage(player);
                } else if (success) {
                    messages.sendRaw(player, "update.latest-header");
                    messages.send(player, "update.latest-title");
                    messages.send(player, "update.latest-version", "current", currentVersion);
                    messages.sendRaw(player, "update.latest-footer");
                } else {
                    messages.send(player, "errors.update-check-failed");
                }
            });
        });
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}
