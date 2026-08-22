package com.extraban.commands;

import com.extraban.Main;
import com.extraban.utils.BanUtils;
import com.extraban.utils.MessageUtils;
import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.BanEntry;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

public class ExtraBanCommand implements CommandExecutor {

    private final Main plugin;
    private final MessageUtils messages;
    private final ActionBarManager actionBarManager;
    private final FreezeManager freezeManager;
    private final WarnManager warnManager;

    public ExtraBanCommand(Main plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessages();
        this.actionBarManager = new ActionBarManager(plugin);
        this.freezeManager = plugin.getFreezeManager();
        this.warnManager = plugin.getWarnManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "ban" -> handleBan(sender, args);
            case "kick" -> handleKick(sender, args);
            case "unban" -> handleUnban(sender, args);
            case "tban", "tempban" -> handleTempBan(sender, args);
            case "freeze" -> handleFreeze(sender, args);
            case "unfreeze" -> handleUnfreeze(sender, args);
            case "warn" -> handleWarn(sender, args);
            case "unwarn" -> handleUnwarn(sender, args);
            case "reload" -> handleReload(sender);
            case "version", "ver" -> handleVersion(sender);
            case "update" -> handleUpdate(sender);
            case "banlist", "bl" -> handleBanList(sender);
            default -> {
                showHelp(sender);
                yield true;
            }
        };
    }

    private boolean handleBan(CommandSender sender, String[] args) {
        if (!sender.hasPermission("extraban.ban")) {
            messages.send(sender, "errors.no-permission");
            return true;
        }
        if (args.length < 2) {
            messages.send(sender, "usage.ban");
            return true;
        }
        executeBan(sender, args);
        return true;
    }

    private boolean handleKick(CommandSender sender, String[] args) {
        if (!sender.hasPermission("extraban.kick")) {
            messages.send(sender, "errors.no-permission");
            return true;
        }
        if (args.length < 2) {
            messages.send(sender, "usage.kick");
            return true;
        }
        executeKick(sender, args);
        return true;
    }

    private boolean handleWarn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("extraban.warn")) {
            messages.send(sender, "errors.no-permission");
            return true;
        }
        if (args.length < 2) {
            messages.send(sender, "usage.warn");
            return true;
        }
        return warnManager.onCommand(sender, null, "warn", args);
    }

    private boolean handleUnwarn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("extraban.warn")) {
            messages.send(sender, "errors.no-permission");
            return true;
        }
        if (args.length < 2) {
            messages.send(sender, "usage.unwarn");
            return true;
        }

        String[] warnArgs = new String[args.length];
        warnArgs[0] = "unwarn";
        System.arraycopy(args, 1, warnArgs, 1, args.length - 1);
        return warnManager.onCommand(sender, null, "warn", warnArgs);
    }

    private boolean handleUnban(CommandSender sender, String[] args) {
        if (!sender.hasPermission("extraban.unban")) {
            messages.send(sender, "errors.no-permission");
            return true;
        }
        if (args.length < 2) {
            messages.send(sender, "usage.unban");
            return true;
        }
        executeUnban(sender, args[1]);
        return true;
    }

    private boolean handleTempBan(CommandSender sender, String[] args) {
        if (!sender.hasPermission("extraban.tempban")) {
            messages.send(sender, "errors.no-permission");
            return true;
        }
        if (args.length < 3) {
            messages.send(sender, "usage.tban");
            messages.send(sender, "errors.time-examples");
            return true;
        }
        executeTempBan(sender, args);
        return true;
    }

    private boolean handleFreeze(CommandSender sender, String[] args) {
        if (!sender.hasPermission("extraban.freeze")) {
            messages.send(sender, "errors.no-permission");
            return true;
        }
        if (args.length < 2) {
            messages.send(sender, "usage.freeze");
            return true;
        }
        executeFreeze(sender, args);
        return true;
    }

    private boolean handleUnfreeze(CommandSender sender, String[] args) {
        if (!sender.hasPermission("extraban.unfreeze")) {
            messages.send(sender, "errors.no-permission");
            return true;
        }
        if (args.length < 2) {
            messages.send(sender, "usage.unfreeze");
            return true;
        }
        executeUnfreeze(sender, args[1]);
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("extraban.reload")) {
            messages.send(sender, "errors.no-permission");
            return true;
        }
        plugin.reloadPluginConfig();
        messages.send(sender, "system.reload-success");
        return true;
    }

    private boolean handleVersion(CommandSender sender) {
        messages.send(sender, "system.version-info",
                "version", plugin.getPluginMeta().getVersion());
        return true;
    }

    private boolean handleBanList(CommandSender sender) {
        if (!sender.hasPermission("extraban.banlist")) {
            messages.send(sender, "errors.no-permission");
            return true;
        }
        showBanList(sender);
        return true;
    }

    private boolean handleUpdate(CommandSender sender) {
        if (!sender.hasPermission("extraban.update")) {
            messages.send(sender, "errors.no-permission");
            return true;
        }

        if (plugin.getUpdateChecker() == null) {
            messages.send(sender, "errors.update-disabled");
            return true;
        }

        if (sender instanceof Player player) {
            plugin.getUpdateChecker().manualCheck(player);
        } else {
            plugin.getUpdateChecker().checkForUpdates();
            messages.send(sender, "errors.update-check-started");
        }
        return true;
    }

    private void executeBan(CommandSender sender, String[] args) {
        String playerName = args[1];
        String staffName = sender.getName();
        String reason = args.length > 2
                ? Arrays.stream(args).skip(2).collect(Collectors.joining(" "))
                : messages.getDefaultReason();

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            messages.send(sender, "errors.player-not-found", "player", playerName);
            return;
        }

        if (messages.isBanActionBarEnabled()) {
            actionBarManager.startCountdown(target, staffName, reason, "ban", null);
        } else {
            executeFinalBan(sender, playerName, reason);
        }
    }

    private void executeKick(CommandSender sender, String[] args) {
        String playerName = args[1];
        String staffName = sender.getName();
        String reason = args.length > 2
                ? Arrays.stream(args).skip(2).collect(Collectors.joining(" "))
                : messages.getKickDefaultReason();

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            messages.send(sender, "errors.player-not-found", "player", playerName);
            return;
        }

        if (messages.isKickActionBarEnabled()) {
            actionBarManager.startCountdown(target, staffName, reason, "kick", null);
        } else {
            executeFinalKick(sender, target, reason);
        }
    }

    private void executeFinalKick(CommandSender sender, Player target, String reason) {
        String staffName = sender.getName();
        target.kick(messages.asComponent(messages.raw("kick.screen",
                "reason", reason,
                "staff", staffName)));

        messages.send(sender, "kick.success", "player", target.getName(), "reason", reason);
        if (messages.isKickBroadcast()) {
            Bukkit.broadcast(messages.asComponent(messages.msg("kick.broadcast",
                    "player", target.getName(),
                    "staff", staffName,
                    "reason", reason)));
        }
    }

    private void executeTempBan(CommandSender sender, String[] args) {
        String playerName = args[1];
        String timeString = args[2];
        String staffName = sender.getName();
        String reason = args.length > 3
                ? Arrays.stream(args).skip(3).collect(Collectors.joining(" "))
                : messages.getDefaultReason();

        try {
            long duration = MessageUtils.parseDuration(timeString);
            long maxDuration = MessageUtils.parseDuration(messages.getTempBanMaxDuration());
            if (duration > maxDuration) {
                messages.send(sender, "errors.duration-too-long",
                        "duration", messages.getTempBanMaxDuration());
                return;
            }
        } catch (IllegalArgumentException e) {
            messages.send(sender, "errors.invalid-time-format");
            return;
        }

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            messages.send(sender, "errors.player-not-found", "player", playerName);
            return;
        }

        if (messages.isBanActionBarEnabled()) {
            actionBarManager.startCountdown(target, staffName, reason, "tban", timeString);
        } else {
            try {
                executeFinalTempBan(sender, playerName, timeString, reason);
            } catch (IllegalArgumentException e) {
                messages.send(sender, "errors.invalid-time-format");
            }
        }
    }

    private void executeFreeze(CommandSender sender, String[] args) {
        String playerName = args[1];
        String duration = "permanent";
        String staffName = sender.getName();
        String reason = messages.getDefaultReason();

        if (args.length > 2) {
            String arg2 = args[2];
            if (arg2.matches("\\d+[smhdw]")) {
                duration = arg2;
                if (args.length > 3) {
                    reason = Arrays.stream(args).skip(3).collect(Collectors.joining(" "));
                }
            } else {
                reason = Arrays.stream(args).skip(2).collect(Collectors.joining(" "));
            }
        }

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            messages.send(sender, "errors.player-not-found", "player", playerName);
            return;
        }

        if (freezeManager.isFrozen(target.getUniqueId())) {
            messages.send(sender, "errors.already-frozen", "player", playerName);
            return;
        }

        freezeManager.freezePlayer(target, staffName, reason, duration);

        String durationText = duration.equals("permanent") ? "Permanent" : duration;
        messages.send(sender, "freeze.success",
                "player", playerName, "duration", durationText, "reason", reason);
        if (messages.isFreezeBroadcast()) {
            Bukkit.broadcast(messages.asComponent(messages.msg("freeze.broadcast",
                    "player", playerName,
                    "staff", staffName,
                    "duration", durationText,
                    "reason", reason)));
        }
    }

    private void executeUnfreeze(CommandSender sender, String playerName) {
        if (!freezeManager.isFrozen(playerName)) {
            messages.send(sender, "errors.not-frozen", "player", playerName);
            return;
        }

        freezeManager.unfreezePlayer(playerName);
        messages.send(sender, "freeze.unfreeze-success", "player", playerName);
        if (messages.isFreezeBroadcast()) {
            Bukkit.broadcast(messages.asComponent(messages.msg("freeze.unfreeze-broadcast",
                    "player", playerName, "staff", sender.getName())));
        }
    }

    private void executeFinalBan(CommandSender sender, String playerName, String reason) {
        String staffName = sender.getName();
        String discord = messages.getDiscord();

        Player target = Bukkit.getPlayer(playerName);
        if (target != null) {
            target.kick(messages.asComponent(messages.raw("ban.kick",
                    "reason", reason, "staff", staffName, "Discord", discord)));
            BanUtils.ban(target, reason, null, staffName);
        } else {
            BanUtils.ban(playerName, reason, null, staffName);
        }

        messages.send(sender, "ban.success", "player", playerName, "reason", reason);

        if (messages.isBanBroadcast()) {
            Bukkit.broadcast(messages.asComponent(messages.msg("ban.broadcast",
                    "player", playerName, "staff", staffName, "reason", reason)));
        }
    }

    private void executeFinalTempBan(CommandSender sender, String playerName, String timeString, String reason) {
        String staffName = sender.getName();
        String discord = messages.getDiscord();

        long duration = MessageUtils.parseDuration(timeString);
        Date expiry = new Date(System.currentTimeMillis() + duration);
        String formattedDuration = MessageUtils.formatDuration(duration);
        String formattedExpiry = messages.formatDate(expiry);

        Player target = Bukkit.getPlayer(playerName);
        if (target != null) {
            target.kick(messages.asComponent(messages.raw("tempban.kick",
                    "reason", reason,
                    "duration", formattedDuration,
                    "staff", staffName,
                    "expiry", formattedExpiry,
                    "Discord", discord)));
            BanUtils.ban(target, reason, expiry, staffName);
        } else {
            BanUtils.ban(playerName, reason, expiry, staffName);
        }

        messages.send(sender, "tempban.success",
                "player", playerName, "duration", formattedDuration, "reason", reason);

        if (messages.isBanBroadcast()) {
            Bukkit.broadcast(messages.asComponent(messages.msg("tempban.broadcast",
                    "player", playerName,
                    "staff", staffName,
                    "duration", formattedDuration,
                    "reason", reason)));
        }
    }

    private void executeUnban(CommandSender sender, String playerName) {
        if (BanUtils.pardon(playerName)) {
            messages.send(sender, "unban.success", "player", playerName);
            if (messages.isUnbanBroadcast()) {
                Bukkit.broadcast(messages.asComponent(messages.msg("unban.broadcast",
                        "player", playerName,
                        "staff", sender.getName())));
            }
        } else {
            messages.send(sender, "errors.player-not-banned", "player", playerName);
        }
    }

    private void showBanList(CommandSender sender) {
        Collection<BanEntry<PlayerProfile>> bans = BanUtils.entries();

        if (bans.isEmpty()) {
            messages.send(sender, "banlist.empty");
            return;
        }

        int maxDisplay = messages.getBanListMaxDisplay();
        messages.sendRaw(sender, "banlist.header");
        messages.sendRaw(sender, "banlist.total", "count", String.valueOf(bans.size()));
        messages.sendRaw(sender, "banlist.separator");

        int count = 0;
        for (BanEntry<PlayerProfile> ban : bans) {
            count++;
            if (count > maxDisplay) {
                messages.sendRaw(sender, "banlist.more",
                        "count", String.valueOf(bans.size() - maxDisplay));
                break;
            }

            String name = ban.getBanTarget().getName();
            UUID profileId = ban.getBanTarget().getId();
            String playerName = name != null ? name : (profileId != null ? profileId.toString() : "unknown");
            String reason = ban.getReason() != null ? ban.getReason() : "No reason";
            Date expiry = ban.getExpiration();

            if (expiry != null) {
                messages.sendRaw(sender, "banlist.temporary",
                        "player", playerName,
                        "expiry", messages.formatDate(expiry),
                        "reason", reason);
            } else {
                messages.sendRaw(sender, "banlist.permanent",
                        "player", playerName,
                        "reason", reason);
            }
        }

        messages.sendRaw(sender, "banlist.footer");
        if (sender.hasPermission("extraban.unban")) {
            messages.sendRaw(sender, "banlist.tip");
        }
    }

    private void showHelp(CommandSender sender) {
        messages.sendRaw(sender, "help.header");

        boolean canBan = sender.hasPermission("extraban.ban");
        boolean canKick = sender.hasPermission("extraban.kick");
        boolean canUnban = sender.hasPermission("extraban.unban");
        boolean canTempBan = sender.hasPermission("extraban.tempban");
        boolean canFreeze = sender.hasPermission("extraban.freeze");
        boolean canUnfreeze = sender.hasPermission("extraban.unfreeze");
        boolean canWarn = sender.hasPermission("extraban.warn");
        boolean canBanList = sender.hasPermission("extraban.banlist");
        boolean canReload = sender.hasPermission("extraban.reload");
        boolean canUpdate = sender.hasPermission("extraban.update");

        if (canBan || canKick || canUnban || canTempBan) {
            messages.sendRaw(sender, "help.blank");
            messages.sendRaw(sender, "help.section-ban");
            if (canBan) messages.sendRaw(sender, "help.ban");
            if (canKick) messages.sendRaw(sender, "help.kick");
            if (canUnban) messages.sendRaw(sender, "help.unban");
            if (canTempBan) messages.sendRaw(sender, "help.tban");
        }

        if (canFreeze || canUnfreeze) {
            messages.sendRaw(sender, "help.blank");
            messages.sendRaw(sender, "help.section-freeze");
            if (canFreeze) messages.sendRaw(sender, "help.freeze");
            if (canUnfreeze) messages.sendRaw(sender, "help.unfreeze");
        }

        if (canWarn) {
            messages.sendRaw(sender, "help.blank");
            messages.sendRaw(sender, "help.section-warn");
            messages.sendRaw(sender, "help.warn");
            messages.sendRaw(sender, "help.unwarn");
        }

        messages.sendRaw(sender, "help.blank");
        messages.sendRaw(sender, "help.section-utility");
        if (canBanList) messages.sendRaw(sender, "help.banlist");
        if (canReload) messages.sendRaw(sender, "help.reload");
        if (canUpdate) messages.sendRaw(sender, "help.update");
        messages.sendRaw(sender, "help.help");
        messages.sendRaw(sender, "help.version");

        messages.sendRaw(sender, "help.blank");
        if (canTempBan || canFreeze) {
            messages.sendRaw(sender, "help.time-formats");
        }
        messages.sendRaw(sender, "help.aliases");
        messages.sendRaw(sender, "help.footer");
    }
}
