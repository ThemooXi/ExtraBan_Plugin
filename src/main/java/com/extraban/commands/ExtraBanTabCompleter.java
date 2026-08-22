package com.extraban.commands;

import com.extraban.utils.BanUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tab completion for ExtraBan moderation commands.
 */
public class ExtraBanTabCompleter implements TabCompleter {

    private final List<String> mainCommands = Arrays.asList(
            "ban", "kick", "unban", "tban", "tempban", "freeze", "unfreeze",
            "warn", "unwarn", "banlist", "help", "reload", "version", "update"
    );

    private final List<String> freezeTimes = List.of(
            "30s", "1m", "5m", "10m", "30m", "1h", "2h", "6h", "12h", "1d", "2d", "7d", "permanent"
    );

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], getAvailableCommands(sender), completions);
        }

        switch (args[0].toLowerCase()) {
            case "ban" -> completions = handleBanCompletions(sender, args);
            case "kick" -> completions = handleKickCompletions(sender, args);
            case "unban" -> completions = handleUnbanCompletions(sender, args);
            case "tban", "tempban" -> completions = handleTempBanCompletions(sender, args);
            case "freeze" -> completions = handleFreezeCompletions(sender, args);
            case "unfreeze" -> completions = handleUnfreezeCompletions(sender, args);
            case "warn" -> completions = handleWarnCompletions(sender, args);
            case "unwarn" -> completions = handleUnwarnCompletions(sender, args);
            case "banlist", "bl", "reload", "help", "version" -> {
                return Collections.emptyList();
            }
            default -> {
                return Collections.emptyList();
            }
        }

        return completions;
    }

    private List<String> handleWarnCompletions(CommandSender sender, String[] args) {
        if (!sender.hasPermission("extraban.warn")) {
            return Collections.emptyList();
        }

        return switch (args.length) {
            case 2 -> getOnlinePlayerNames(args[1]);
            case 3 -> StringUtil.copyPartialMatches(args[2], getWarnReasons(), new ArrayList<>());
            default -> Collections.emptyList();
        };
    }

    private List<String> handleUnwarnCompletions(CommandSender sender, String[] args) {
        if (!sender.hasPermission("extraban.warn")) {
            return Collections.emptyList();
        }

        if (args.length == 2) {
            return getOnlinePlayerNames(args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> getWarnReasons() {
        return List.of("Spam", "Swearing", "Disrespect", "Minor_offense", "Chat_violation");
    }

    private List<String> getAvailableCommands(CommandSender sender) {
        List<String> available = new ArrayList<>();

        if (sender.hasPermission("extraban.ban")) available.add("ban");
        if (sender.hasPermission("extraban.kick")) available.add("kick");
        if (sender.hasPermission("extraban.unban")) available.add("unban");

        if (sender.hasPermission("extraban.tempban")) {
            available.add("tban");
            available.add("tempban");
        }

        if (sender.hasPermission("extraban.freeze")) available.add("freeze");
        if (sender.hasPermission("extraban.unfreeze")) available.add("unfreeze");

        if (sender.hasPermission("extraban.warn")) {
            available.add("warn");
            available.add("unwarn");
        }

        if (sender.hasPermission("extraban.banlist")) {
            available.add("banlist");
            available.add("bl");
        }

        if (sender.hasPermission("extraban.reload")) available.add("reload");
        if (sender.hasPermission("extraban.update")) available.add("update");

        available.add("help");
        available.add("version");

        return available;
    }

    private List<String> handleBanCompletions(CommandSender sender, String[] args) {
        if (!sender.hasPermission("extraban.ban")) {
            return Collections.emptyList();
        }

        return switch (args.length) {
            case 2 -> getOnlinePlayerNames(args[1]);
            case 3 -> StringUtil.copyPartialMatches(args[2], getBanReasons(), new ArrayList<>());
            default -> Collections.emptyList();
        };
    }

    private List<String> handleKickCompletions(CommandSender sender, String[] args) {
        if (!sender.hasPermission("extraban.kick")) {
            return Collections.emptyList();
        }

        return switch (args.length) {
            case 2 -> getOnlinePlayerNames(args[1]);
            case 3 -> StringUtil.copyPartialMatches(args[2], getKickReasons(), new ArrayList<>());
            default -> Collections.emptyList();
        };
    }

    private List<String> handleUnbanCompletions(CommandSender sender, String[] args) {
        if (!sender.hasPermission("extraban.unban")) {
            return Collections.emptyList();
        }

        if (args.length == 2) {
            return getBannedPlayerNames(args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> handleTempBanCompletions(CommandSender sender, String[] args) {
        if (!sender.hasPermission("extraban.tempban")) {
            return Collections.emptyList();
        }

        return switch (args.length) {
            case 2 -> getOnlinePlayerNames(args[1]);
            case 3 -> StringUtil.copyPartialMatches(args[2], getTimeSuggestions(), new ArrayList<>());
            case 4 -> StringUtil.copyPartialMatches(args[3], getBanReasons(), new ArrayList<>());
            default -> Collections.emptyList();
        };
    }

    private List<String> handleFreezeCompletions(CommandSender sender, String[] args) {
        if (!sender.hasPermission("extraban.freeze")) {
            return Collections.emptyList();
        }

        return switch (args.length) {
            case 2 -> getOnlinePlayerNames(args[1]);
            case 3 -> StringUtil.copyPartialMatches(args[2], freezeTimes, new ArrayList<>());
            case 4 -> StringUtil.copyPartialMatches(args[3], getFreezeReasons(), new ArrayList<>());
            default -> Collections.emptyList();
        };
    }

    private List<String> handleUnfreezeCompletions(CommandSender sender, String[] args) {
        if (!sender.hasPermission("extraban.unfreeze")) {
            return Collections.emptyList();
        }

        if (args.length == 2) {
            return getOnlinePlayerNames(args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> getOnlinePlayerNames(String partial) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(partial.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> getBannedPlayerNames(String partial) {
        return BanUtils.bannedNames().stream()
                .filter(name -> name.toLowerCase().startsWith(partial.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> getTimeSuggestions() {
        return List.of("30m", "1h", "6h", "12h", "1d", "3d", "7d", "30d", "1w", "2w", "4w");
    }

    private List<String> getBanReasons() {
        return List.of("Cheating", "Hacking", "Exploiting", "Toxicity", "Harassment", "Spam", "Advertising", "Griefing");
    }

    private List<String> getKickReasons() {
        return List.of("Spam", "Toxicity", "Disrespect", "Advertising", "AFK", "Rule_violation");
    }

    private List<String> getFreezeReasons() {
        return List.of("Investigation", "Suspicious_activity", "Client_check", "Rule_violation");
    }
}
