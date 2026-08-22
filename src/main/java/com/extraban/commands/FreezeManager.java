package com.extraban.commands;

import com.extraban.Main;
import com.extraban.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FreezeManager implements Listener {

    private final Main plugin;
    private final MessageUtils messages;
    private final Map<UUID, FreezeData> frozenPlayers = new HashMap<>();
    private final Map<UUID, BukkitTask> freezeTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> countdownTasks = new HashMap<>();
    private final Map<UUID, GameMode> originalGameModes = new HashMap<>();
    private final Map<UUID, Float> originalWalkSpeeds = new HashMap<>();
    private final Map<UUID, Float> originalFlySpeeds = new HashMap<>();

    public FreezeManager(Main plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessages();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public static class FreezeData {
        public final String staffName;
        public final String reason;
        public final Long expiryTime;
        public boolean countdownFinished;

        public FreezeData(String staffName, String reason, Long expiryTime, boolean countdownFinished) {
            this.staffName = staffName;
            this.reason = reason;
            this.expiryTime = expiryTime;
            this.countdownFinished = countdownFinished;
        }

        public boolean isTemporary() {
            return expiryTime != null;
        }

        public boolean isExpired() {
            return isTemporary() && System.currentTimeMillis() > expiryTime;
        }
    }

    public void freezePlayer(Player target, String staffName, String reason, String duration) {
        UUID targetId = target.getUniqueId();
        unfreezePlayer(targetId, false);

        Long expiryTime = null;
        int countdownTime = plugin.getConfig().getInt("settings.freeze.countdown-time", 15);

        if (duration != null && !duration.equalsIgnoreCase("permanent")) {
            try {
                expiryTime = System.currentTimeMillis() + MessageUtils.parseDuration(duration);
            } catch (IllegalArgumentException e) {
                expiryTime = null;
            }
        }

        FreezeData freezeData = new FreezeData(staffName, reason, expiryTime, false);
        frozenPlayers.put(targetId, freezeData);
        originalGameModes.put(targetId, target.getGameMode());
        originalWalkSpeeds.put(targetId, target.getWalkSpeed());
        originalFlySpeeds.put(targetId, target.getFlySpeed());
        startFreezeCountdown(target, freezeData, countdownTime);
    }

    public void unfreezePlayer(UUID playerId) {
        unfreezePlayer(playerId, true);
    }

    public void unfreezePlayer(UUID playerId, boolean notify) {
        frozenPlayers.remove(playerId);
        BukkitTask task = freezeTasks.remove(playerId);
        BukkitTask countdownTask = countdownTasks.remove(playerId);

        if (task != null) task.cancel();
        if (countdownTask != null) countdownTask.cancel();

        Player target = Bukkit.getPlayer(playerId);
        if (target != null) {
            removeFreezeEffects(target);
            if (notify) {
                sendActionBar(target, messages.raw("freeze.unfreeze-notify"));
                messages.send(target, "freeze.unfreeze-notify");
            }
        }

        originalGameModes.remove(playerId);
        originalWalkSpeeds.remove(playerId);
        originalFlySpeeds.remove(playerId);
    }

    public void unfreezePlayer(String playerName) {
        for (Map.Entry<UUID, FreezeData> entry : new HashMap<>(frozenPlayers).entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.getName().equalsIgnoreCase(playerName)) {
                unfreezePlayer(entry.getKey());
                break;
            }
        }
    }

    public boolean isFrozen(UUID playerId) {
        return frozenPlayers.containsKey(playerId);
    }

    public boolean isFrozen(String playerName) {
        for (UUID playerId : frozenPlayers.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.getName().equalsIgnoreCase(playerName)) {
                return true;
            }
        }
        return false;
    }

    private void startFreezeCountdown(Player target, FreezeData freezeData, int countdownTime) {
        UUID targetId = target.getUniqueId();
        int[] currentCountdown = {countdownTime};

        BukkitTask countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                FreezeData currentFreeze = frozenPlayers.get(targetId);
                if (currentFreeze == null) {
                    cancel();
                    return;
                }

                if (currentCountdown[0] <= 0) {
                    applyFreezeEffects(target);
                    currentFreeze.countdownFinished = true;
                    messages.sendRaw(target, "freeze.notify",
                            "reason", currentFreeze.reason,
                            "duration", currentFreeze.isTemporary()
                                    ? MessageUtils.formatDuration(currentFreeze.expiryTime - System.currentTimeMillis())
                                    : "Permanent",
                            "staff", currentFreeze.staffName);
                    startFreezeTask(target, currentFreeze);
                    cancel();
                    countdownTasks.remove(targetId);
                    return;
                }

                String countdownMessage = messages.raw(
                        "freeze.action-bar-countdown",
                        "time", String.valueOf(currentCountdown[0]));
                sendActionBar(target, countdownMessage);
                currentCountdown[0]--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        countdownTasks.put(targetId, countdownTask);
    }

    private void startFreezeTask(Player target, FreezeData freezeData) {
        UUID targetId = target.getUniqueId();

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                FreezeData currentFreeze = frozenPlayers.get(targetId);
                if (currentFreeze == null) {
                    cancel();
                    return;
                }

                if (currentFreeze.isTemporary() && currentFreeze.isExpired()) {
                    unfreezePlayer(targetId);
                    messages.send(target, "freeze.auto-unfreeze");
                    return;
                }

                sendActionBar(target, getFrozenActionBarMessage(currentFreeze));
            }
        }.runTaskTimer(plugin, 0L, 20L);

        freezeTasks.put(targetId, task);
    }

    private void applyFreezeEffects(Player player) {
        player.setWalkSpeed(0.0f);
        player.setFlySpeed(0.0f);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setGameMode(GameMode.ADVENTURE);
        player.setAI(false);
        player.setCollidable(false);
    }

    private void removeFreezeEffects(Player player) {
        UUID playerId = player.getUniqueId();
        player.setWalkSpeed(originalWalkSpeeds.getOrDefault(playerId, 0.2f));
        player.setFlySpeed(originalFlySpeeds.getOrDefault(playerId, 0.1f));
        player.setGameMode(originalGameModes.getOrDefault(playerId, GameMode.SURVIVAL));
        player.setAI(true);
        player.setCollidable(true);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!isFrozen(event.getPlayer().getUniqueId())) return;
        if (event.getFrom().getX() != event.getTo().getX()
                || event.getFrom().getY() != event.getTo().getY()
                || event.getFrom().getZ() != event.getTo().getZ()) {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        if (isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player && isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
            messages.send(player, "errors.frozen-inventory");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player && isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        if (isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private String getFrozenActionBarMessage(FreezeData freezeData) {
        String status = freezeData.isTemporary()
                ? "Left: " + formatTimeLeft(freezeData.expiryTime)
                : "Permanent";
        return messages.raw("freeze.action-bar-frozen", "status", status);
    }

    private String formatTimeLeft(Long expiryTime) {
        if (expiryTime == null) return "Permanent";
        return MessageUtils.formatDuration(Math.max(0, expiryTime - System.currentTimeMillis()));
    }

    private void sendActionBar(Player player, String message) {
        player.sendActionBar(messages.asComponent(message));
    }
}
