package com.extraban.commands;

import com.extraban.Main;
import com.extraban.utils.BanUtils;
import com.extraban.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ActionBarManager {

    private final Main plugin;
    private final MessageUtils messages;
    private final Map<UUID, BukkitTask> pendingActions = new HashMap<>();
    private final Map<UUID, PendingAction> actionData = new HashMap<>();

    public ActionBarManager(Main plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessages();
    }

    public static class PendingAction {
        public final String targetName;
        public final String staffName;
        public final String reason;
        public final String type;
        public final String duration;
        public int countdown;

        public PendingAction(String targetName, String staffName, String reason,
                             String type, String duration, int countdown) {
            this.targetName = targetName;
            this.staffName = staffName;
            this.reason = reason;
            this.type = type;
            this.duration = duration;
            this.countdown = countdown;
        }
    }

    public void startCountdown(Player target, String staffName, String reason, String type, String duration) {
        UUID targetId = target.getUniqueId();
        cancelCountdown(targetId, false);

        int countdownTime = countdownFor(type);
        PendingAction action = new PendingAction(target.getName(), staffName, reason, type, duration, countdownTime);
        actionData.put(targetId, action);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                PendingAction currentAction = actionData.get(targetId);
                if (currentAction == null) {
                    cancel();
                    return;
                }

                if (currentAction.countdown <= 0) {
                    executeAction(target, currentAction);
                    cancelCountdown(targetId, false);
                    return;
                }

                String message = messages.raw(
                        messagePrefix(currentAction.type) + ".action-bar-warning",
                        "time", String.valueOf(currentAction.countdown),
                        "staff", currentAction.staffName);
                sendActionBar(target, message);
                currentAction.countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        pendingActions.put(targetId, task);
    }

    public void cancelCountdown(UUID targetId) {
        cancelCountdown(targetId, true);
    }

    private void cancelCountdown(UUID targetId, boolean notifyCancel) {
        BukkitTask task = pendingActions.remove(targetId);
        if (task != null) {
            task.cancel();
        }

        PendingAction action = actionData.remove(targetId);
        if (notifyCancel && action != null) {
            Player target = Bukkit.getPlayer(targetId);
            if (target != null) {
                sendActionBar(target, messages.raw(messagePrefix(action.type) + ".action-bar-cancelled"));
            }
        }
    }

    private void executeAction(Player target, PendingAction action) {
        sendActionBar(target, messages.raw(messagePrefix(action.type) + ".action-bar-executed"));

        switch (action.type) {
            case "ban" -> executeFinalBan(target, action);
            case "tban" -> executeFinalTempBan(target, action);
            case "kick" -> executeFinalKick(target, action);
            default -> {
            }
        }
    }

    private String messagePrefix(String type) {
        return "kick".equals(type) ? "kick" : "ban";
    }

    private int countdownFor(String type) {
        return "kick".equals(type) ? messages.getKickActionBarCountdown() : messages.getBanActionBarCountdown();
    }

    private void executeFinalBan(Player target, PendingAction action) {
        String discord = messages.getDiscord();
        target.kick(messages.asComponent(messages.raw("ban.kick",
                "reason", action.reason,
                "staff", action.staffName,
                "Discord", discord)));

        BanUtils.ban(target, action.reason, null, action.staffName);

        Player staff = Bukkit.getPlayer(action.staffName);
        if (staff != null) {
            messages.send(staff, "ban.success",
                    "player", target.getName(), "reason", action.reason);
        }

        if (messages.isBanBroadcast()) {
            Bukkit.broadcast(messages.asComponent(messages.msg("ban.broadcast",
                    "player", target.getName(),
                    "staff", action.staffName,
                    "reason", action.reason)));
        }
    }

    private void executeFinalTempBan(Player target, PendingAction action) {
        try {
            long duration = MessageUtils.parseDuration(action.duration);
            Date expiry = new Date(System.currentTimeMillis() + duration);
            String formattedDuration = MessageUtils.formatDuration(duration);
            String formattedExpiry = messages.formatDate(expiry);
            String discord = messages.getDiscord();

            target.kick(messages.asComponent(messages.raw("tempban.kick",
                    "reason", action.reason,
                    "duration", formattedDuration,
                    "staff", action.staffName,
                    "expiry", formattedExpiry,
                    "Discord", discord)));

            BanUtils.ban(target, action.reason, expiry, action.staffName);

            Player staff = Bukkit.getPlayer(action.staffName);
            if (staff != null) {
                messages.send(staff, "tempban.success",
                        "player", target.getName(),
                        "duration", formattedDuration,
                        "reason", action.reason);
            }

            if (messages.isBanBroadcast()) {
                Bukkit.broadcast(messages.asComponent(messages.msg("tempban.broadcast",
                        "player", target.getName(),
                        "staff", action.staffName,
                        "duration", formattedDuration,
                        "reason", action.reason)));
            }
        } catch (IllegalArgumentException e) {
            executeFinalBan(target, action);
        }
    }

    private void executeFinalKick(Player target, PendingAction action) {
        target.kick(messages.asComponent(messages.raw("kick.screen",
                "reason", action.reason,
                "staff", action.staffName)));

        Player staff = Bukkit.getPlayer(action.staffName);
        if (staff != null) {
            messages.send(staff, "kick.success",
                    "player", target.getName(), "reason", action.reason);
        }

        if (messages.isKickBroadcast()) {
            Bukkit.broadcast(messages.asComponent(messages.msg("kick.broadcast",
                    "player", target.getName(),
                    "staff", action.staffName,
                    "reason", action.reason)));
        }
    }

    private void sendActionBar(Player player, String message) {
        player.sendActionBar(messages.asComponent(message));
    }
}
