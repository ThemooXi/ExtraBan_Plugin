package com.extraban.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.ban.BanListType;
import org.bukkit.BanEntry;
import org.bukkit.Bukkit;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Profile-based ban helpers for Minecraft 26.2 / Paper BanListType.PROFILE.
 */
public final class BanUtils {

    private BanUtils() {
    }

    public static ProfileBanList list() {
        return Bukkit.getBanList(BanListType.PROFILE);
    }

    public static PlayerProfile profile(Player player) {
        return player.getPlayerProfile();
    }

    public static PlayerProfile profile(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online.getPlayerProfile();
        }
        return Bukkit.createProfile(name);
    }

    public static void ban(Player player, String reason, Date expiry, String source) {
        Instant expires = expiry == null ? null : expiry.toInstant();
        list().addBan(profile(player), reason, expires, source);
    }

    public static void ban(String playerName, String reason, Date expiry, String source) {
        Player online = Bukkit.getPlayerExact(playerName);
        if (online != null) {
            ban(online, reason, expiry, source);
            return;
        }
        Instant expires = expiry == null ? null : expiry.toInstant();
        list().addBan(profile(playerName), reason, expires, source);
    }

    public static boolean isBanned(String playerName) {
        return findEntry(playerName) != null;
    }

    public static boolean pardon(String playerName) {
        BanEntry<PlayerProfile> entry = findEntry(playerName);
        if (entry == null) {
            return false;
        }
        list().pardon(entry.getBanTarget());
        return true;
    }

    public static BanEntry<PlayerProfile> findEntry(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return null;
        }
        String needle = playerName.toLowerCase(Locale.ROOT);
        for (BanEntry<PlayerProfile> entry : entries()) {
            String name = entry.getBanTarget().getName();
            if (name != null && name.toLowerCase(Locale.ROOT).equals(needle)) {
                return entry;
            }
        }
        return null;
    }

    public static Collection<BanEntry<PlayerProfile>> entries() {
        return list().getEntries();
    }

    public static Set<String> bannedNames() {
        Set<String> names = new HashSet<>();
        for (BanEntry<PlayerProfile> entry : entries()) {
            String name = entry.getBanTarget().getName();
            if (name != null) {
                names.add(name);
            }
        }
        return names;
    }
}
