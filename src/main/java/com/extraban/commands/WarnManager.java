package com.extraban.commands;

import com.extraban.Main;
import com.extraban.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;

public class WarnManager implements CommandExecutor, TabCompleter, Listener {

    private final Map<UUID, List<Warning>> playerWarnings = new HashMap<>();
    private final Main plugin;
    private final MessageUtils messages;
    private File warningsFile;
    private FileConfiguration warningsConfig;

    public WarnManager(Main plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessages();
        setupWarningsFile();
        loadWarnings();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("extraban.warn")) {
            messages.send(sender, "errors.no-permission");
            return true;
        }

        if (args.length < 2) {
            messages.send(sender, "usage.warn");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);

        switch (subCommand) {
            case "warn" -> {
                if (target == null || !target.isOnline()) {
                    messages.send(sender, "errors.warn-offline");
                    return true;
                }
                handleWarn(sender, target, args);
            }
            case "unwarn" -> handleUnwarn(sender, playerName);
            case "list" -> handleListWarnings(sender, playerName);
            default -> messages.send(sender, "usage.warn");
        }

        return true;
    }

    private void handleWarn(CommandSender sender, Player target, String[] args) {
        String reason = args.length > 2
                ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length))
                : messages.getDefaultReason();

        UUID playerId = target.getUniqueId();
        Warning warning = new Warning(reason, sender.getName(), new Date());
        playerWarnings.computeIfAbsent(playerId, k -> new ArrayList<>()).add(warning);
        saveWarningsAsync();

        if (messages.isWarnBarrierEnabled()) {
            addWarningBarrier(target, warning);
        }

        messages.send(sender, "warn.success",
                "player", target.getName(), "reason", reason, "staff", sender.getName());
        messages.send(target, "warn.notify",
                "player", target.getName(), "reason", reason, "staff", sender.getName());

        if (messages.isWarnBroadcast()) {
            Bukkit.broadcast(messages.asComponent(messages.msg("warn.broadcast",
                    "player", target.getName(),
                    "staff", sender.getName(),
                    "reason", reason)));
        }
    }

    private void handleUnwarn(CommandSender sender, String playerName) {
        UUID playerId = getPlayerUUID(playerName);
        if (playerId == null || !playerWarnings.containsKey(playerId)) {
            messages.send(sender, "errors.warn-none", "player", playerName);
            return;
        }

        List<Warning> warnings = playerWarnings.get(playerId);
        if (warnings.isEmpty()) {
            playerWarnings.remove(playerId);
            messages.send(sender, "errors.warn-none", "player", playerName);
            return;
        }

        Warning removedWarning = warnings.remove(warnings.size() - 1);
        Player target = Bukkit.getPlayer(playerId);
        if (target != null && target.isOnline()) {
            removeWarningBarrier(target);
            messages.send(target, "warn.unwarn-notify",
                    "player", playerName, "staff", sender.getName());
        }

        if (warnings.isEmpty()) {
            playerWarnings.remove(playerId);
        }

        saveWarningsAsync();
        messages.send(sender, "warn.unwarn-success",
                "player", playerName, "staff", sender.getName());
    }

    private void handleListWarnings(CommandSender sender, String playerName) {
        UUID playerId = getPlayerUUID(playerName);
        if (playerId == null || !playerWarnings.containsKey(playerId) || playerWarnings.get(playerId).isEmpty()) {
            messages.send(sender, "warn.list-empty", "player", playerName);
            return;
        }

        List<Warning> warnings = playerWarnings.get(playerId);
        messages.sendRaw(sender, "warn.list-header",
                "player", playerName, "count", String.valueOf(warnings.size()));

        for (int i = 0; i < warnings.size(); i++) {
            Warning warning = warnings.get(i);
            messages.sendRaw(sender, "warn.list-entry",
                    "count", String.valueOf(i + 1),
                    "reason", warning.getReason(),
                    "staff", warning.getWarner(),
                    "expiry", messages.formatDate(warning.getDate()),
                    "id", warning.getId().toString().substring(0, 8));
        }

        messages.sendRaw(sender, "warn.list-footer", "count", String.valueOf(warnings.size()));
    }

    private void addWarningBarrier(Player player, Warning warning) {
        ItemStack barrier = createWarningBarrier(warning, player.getUniqueId());
        int emptySlot = player.getInventory().firstEmpty();
        if (emptySlot != -1) {
            player.getInventory().setItem(emptySlot, barrier);
        } else {
            player.getInventory().addItem(barrier);
        }
        player.updateInventory();
    }

    private ItemStack createWarningBarrier(Warning warning, UUID playerUUID) {
        ItemStack barrier = new ItemStack(Material.BARRIER);
        ItemMeta meta = barrier.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("WARNING")
                    .color(NamedTextColor.RED)
                    .decorate(TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            List<Warning> warnings = playerWarnings.get(playerUUID);
            int warningNumber = warnings != null ? warnings.indexOf(warning) + 1 : 1;

            List<Component> lore = new ArrayList<>();
            lore.add(plainGray("Warning #" + warningNumber));
            lore.add(plainGray("Reason: ").append(Component.text(warning.getReason(), NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false)));
            lore.add(plainGray("By: ").append(Component.text(warning.getWarner(), NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false)));
            lore.add(plainGray("Date: ").append(Component.text(messages.formatDate(warning.getDate()), NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.empty());
            lore.add(Component.text("CANNOT BE MOVED OR DROPPED")
                    .color(NamedTextColor.RED)
                    .decorate(TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(plainGray("Removed when this warning is cleared.").color(NamedTextColor.YELLOW));

            meta.lore(lore);

            CustomModelDataComponent modelData = meta.getCustomModelDataComponent();
            modelData.setFloats(List.of((float) messages.getWarnCustomModelData()));
            meta.setCustomModelDataComponent(modelData);
            meta.setUnbreakable(true);
            barrier.setItemMeta(meta);
        }
        return barrier;
    }

    private boolean isProtectedBarrier(ItemStack item) {
        if (item == null || item.getType() != Material.BARRIER || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        Component displayName = meta.displayName();
        if (displayName == null) {
            return false;
        }
        String plainName = PlainTextComponentSerializer.plainText().serialize(displayName);
        if (!plainName.contains("WARNING")) {
            return false;
        }
        CustomModelDataComponent modelData = meta.getCustomModelDataComponent();
        List<Float> floats = modelData.getFloats();
        return !floats.isEmpty() && floats.getFirst().intValue() == messages.getWarnCustomModelData();
    }

    private Component plainGray(String text) {
        return Component.text(text, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    }

    private void removeWarningBarrier(Player player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (isProtectedBarrier(inventory.getItem(i))) {
                inventory.setItem(i, null);
                break;
            }
        }
        player.updateInventory();
    }

    private UUID getPlayerUUID(String playerName) {
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            return onlinePlayer.getUniqueId();
        }

        for (UUID uuid : playerWarnings.keySet()) {
            String offlineName = Bukkit.getOfflinePlayer(uuid).getName();
            if (offlineName != null && offlineName.equalsIgnoreCase(playerName)) {
                return uuid;
            }
        }
        return null;
    }

    private void setupWarningsFile() {
        warningsFile = new File(plugin.getDataFolder(), "warnings.yml");
        if (!warningsFile.exists()) {
            try {
                warningsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create warnings file: " + e.getMessage());
            }
        }
        warningsConfig = YamlConfiguration.loadConfiguration(warningsFile);
    }

    private void loadWarnings() {
        playerWarnings.clear();
        if (!warningsConfig.contains("warnings") || warningsConfig.getConfigurationSection("warnings") == null) {
            return;
        }

        for (String playerUUID : warningsConfig.getConfigurationSection("warnings").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(playerUUID);
                List<Warning> warnings = new ArrayList<>();
                if (warningsConfig.getConfigurationSection("warnings." + playerUUID) == null) continue;

                for (String warningId : warningsConfig.getConfigurationSection("warnings." + playerUUID).getKeys(false)) {
                    String path = "warnings." + playerUUID + "." + warningId + ".";
                    Warning warning = new Warning(
                            UUID.fromString(warningId),
                            warningsConfig.getString(path + "reason"),
                            warningsConfig.getString(path + "warner"),
                            new Date(warningsConfig.getLong(path + "date")));
                    warnings.add(warning);
                }

                if (!warnings.isEmpty()) {
                    playerWarnings.put(uuid, warnings);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().severe("Error loading warning for UUID: " + playerUUID);
            }
        }

        plugin.getLogger().info("Loaded " + getTotalWarnings() + " warnings for " + playerWarnings.size() + " players.");
    }

    private void saveWarnings() {
        warningsConfig.set("warnings", null);
        for (Map.Entry<UUID, List<Warning>> entry : playerWarnings.entrySet()) {
            String playerUUID = entry.getKey().toString();
            for (Warning warning : entry.getValue()) {
                String path = "warnings." + playerUUID + "." + warning.getId() + ".";
                warningsConfig.set(path + "reason", warning.getReason());
                warningsConfig.set(path + "warner", warning.getWarner());
                warningsConfig.set(path + "date", warning.getDate().getTime());
            }
        }
        try {
            warningsConfig.save(warningsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save warnings: " + e.getMessage());
        }
    }

    public void saveWarningsAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveWarnings);
    }

    public int getTotalWarnings() {
        return playerWarnings.values().stream().mapToInt(List::size).sum();
    }

    private void removeAllWarningBarriers(Player player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (isProtectedBarrier(inventory.getItem(i))) {
                inventory.setItem(i, null);
            }
        }
        player.updateInventory();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (isProtectedBarrier(event.getCurrentItem()) || isProtectedBarrier(event.getCursor())) {
            event.setCancelled(true);
            player.updateInventory();
            return;
        }

        if (event.getHotbarButton() != -1) {
            ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
            if (isProtectedBarrier(hotbarItem)) {
                event.setCancelled(true);
                player.updateInventory();
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        for (ItemStack item : event.getNewItems().values()) {
            if (isProtectedBarrier(item)) {
                event.setCancelled(true);
                ((Player) event.getWhoClicked()).updateInventory();
                return;
            }
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        if (isProtectedBarrier(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            messages.send(event.getPlayer(), "errors.warn-barrier-drop");
            event.getPlayer().updateInventory();
        }
    }

    @EventHandler
    public void onHandSwap(PlayerSwapHandItemsEvent event) {
        if (isProtectedBarrier(event.getMainHandItem()) || isProtectedBarrier(event.getOffHandItem())) {
            event.setCancelled(true);
            messages.send(event.getPlayer(), "errors.warn-barrier-swap");
            event.getPlayer().updateInventory();
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getItem() != null && isProtectedBarrier(event.getItem())) {
            event.setCancelled(true);
            event.getPlayer().updateInventory();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        removeAllWarningBarriers(player);

        if (messages.isWarnBarrierEnabled()
                && playerWarnings.containsKey(playerId)
                && !playerWarnings.get(playerId).isEmpty()) {
            for (Warning warning : playerWarnings.get(playerId)) {
                addWarningBarrier(player, warning);
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0],
                    List.of("warn", "unwarn", "list"), new ArrayList<>());
        }
        if (args.length == 2) {
            List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
            return StringUtil.copyPartialMatches(args[1], playerNames, new ArrayList<>());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("warn")) {
            List<String> reasons = plugin.getConfig().getStringList("settings.warn.suggested-reasons");
            if (reasons.isEmpty()) {
                reasons = List.of("Spam", "Swearing", "Disrespect", "Minor_offense", "Chat_violation");
            }
            return StringUtil.copyPartialMatches(args[2], reasons, new ArrayList<>());
        }
        return Collections.emptyList();
    }

    public static class Warning {
        private final UUID id;
        private final String reason;
        private final String warner;
        private final Date date;

        public Warning(UUID id, String reason, String warner, Date date) {
            this.id = id;
            this.reason = reason;
            this.warner = warner;
            this.date = date;
        }

        public Warning(String reason, String warner, Date date) {
            this(UUID.randomUUID(), reason, warner, date);
        }

        public UUID getId() { return id; }
        public String getReason() { return reason; }
        public String getWarner() { return warner; }
        public Date getDate() { return date; }
    }
}
