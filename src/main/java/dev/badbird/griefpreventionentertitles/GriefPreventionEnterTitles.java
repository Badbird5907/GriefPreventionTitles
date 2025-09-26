package dev.badbird.griefpreventionentertitles;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class GriefPreventionEnterTitles extends JavaPlugin implements Listener, CommandExecutor {

    private static Map<UUID, Claim> claimMap = new HashMap<>();
    private static Set<UUID> disabledUsers = new HashSet<>();
    private File disabledDirectory;

    private static MiniMessage miniMessage;

    private static String enterTitle;
    private static String enterSubtitle;
    private static String leaveTitle;
    private static String leaveSubtitle;
    private static String enterActionbar;
    private static String leaveActionbar;
    private FileConfiguration config;

    @Override
    public void onEnable() {
        miniMessage = MiniMessage.miniMessage();
        if (!getDataFolder().exists()) getDataFolder().mkdir();
        if (!new File(getDataFolder() + "/config.yml").exists()) saveDefaultConfig();
        
        // Create disabled directory and load existing disabled users
        disabledDirectory = new File(getDataFolder(), "disabled");
        if (!disabledDirectory.exists()) {
            disabledDirectory.mkdirs();
        }
        loadDisabledUsers();
        
        getServer().getPluginManager().registerEvents(this, this);
        
        // Register command executor
        getCommand("toggleclaimtitles").setExecutor(this);
        
        String enterBase = "titles.enter";
        if (getConfig().getBoolean(enterBase + ".enabled")) {
            enterTitle = getConfig().getString(enterBase + ".title");
            enterSubtitle = getConfig().getString(enterBase + ".subtitle");
            enterActionbar = getConfig().getString(enterBase + ".actionbar");
        }
        String leaveBase = "titles.exit";
        if (getConfig().getBoolean(leaveBase + ".enabled")) {
            leaveTitle = getConfig().getString(leaveBase + ".title");
            leaveSubtitle = getConfig().getString(leaveBase + ".subtitle");
            leaveActionbar = getConfig().getString(leaveBase + ".actionbar");
        }
    }

    @Override
    public @NotNull FileConfiguration getConfig() {
        if (config == null) {
            config = YamlConfiguration.loadConfiguration(new File(getDataFolder() + "/config.yml"));
        }
        return config;
    }

    @Override
    public void reloadConfig() {

    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        // System.out.println("Player moved");
        onMove(event.getPlayer(), event.getFrom(), event.getTo());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        claimMap.put(e.getPlayer().getUniqueId(), GriefPrevention.instance.dataStore.getClaimAt(e.getPlayer().getLocation(), true, null));
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        claimMap.remove(event.getPlayer().getUniqueId());
    }

    private void onMove(Player player, Location from, Location to) {
        World fromWorld = from.getWorld();
        World toWorld = to.getWorld();

        boolean switchedDimensions = !fromWorld.getName().equals(toWorld.getName());
        if (switchedDimensions && !getConfig().getBoolean("show-on-dimension-switch", true)) return;
        if (getConfig().getBoolean("watched-worlds.enable", false)) {
            List<String> worlds = getConfig().getStringList("watched-worlds.worlds");
            if (!worlds.contains(toWorld.getName())) return;
        }

        Claim cachedClaim = claimMap.get(player.getUniqueId());
        Claim movingTo = GriefPrevention.instance.dataStore.getClaimAt(from, true, cachedClaim);
        if (cachedClaim == null && movingTo != null) { // Entering a claim
            if (movingTo.isAdminClaim() && !getConfig().getBoolean("show-on-admin-claim", true)) {
                return;
            }
            
            // Check if player has disabled titles
            if (isTitlesDisabled(player.getUniqueId())) {
                claimMap.put(player.getUniqueId(), movingTo); // Still update the claim map
                return;
            }
            
            // System.out.println("x: " + to.getBlockX() + " y: " + to.getBlockY() + " z: " + to.getBlockZ() + " | " + movingTo + " vs " + cachedClaim);

            // System.out.println("Entering a claim");
            Component enter = null, sub = null, action = null;
            if (enterTitle != null && !enterTitle.isEmpty()) {
                enter = miniMessage.deserialize(enterTitle.replace("%player%", movingTo.getOwnerName()));
            } else enter = Component.empty();
            if (enterSubtitle != null && !enterSubtitle.isEmpty()) {
                sub = miniMessage.deserialize(enterSubtitle.replace("%player%", movingTo.getOwnerName()));
            } else sub = Component.empty();

            claimMap.put(player.getUniqueId(), movingTo);
            Title title = Title.title(enter, sub);
            // System.out.println("Title: " + title);
            // player.showTitle(title);
            player.showTitle(title);

            if (enterActionbar != null && !enterActionbar.isEmpty()) {
                // player.sendActionBar(miniMessage.deserialize(enterActionbar.replace("%player%", movingTo.getOwnerName())));
                player.sendActionBar(miniMessage.deserialize(enterActionbar.replace("%player%", movingTo.getOwnerName())));
            }
        } else if (cachedClaim != null && movingTo == null) { // Leaving a claim
            if (cachedClaim.isAdminClaim() && !getConfig().getBoolean("show-on-admin-claim", true)) {
                return;
            }
            
            // Check if player has disabled titles
            if (isTitlesDisabled(player.getUniqueId())) {
                claimMap.remove(player.getUniqueId()); // Still update the claim map
                return;
            }
            
            // System.out.println("x: " + to.getBlockX() + " y: " + to.getBlockY() + " z: " + to.getBlockZ() + " | " + movingTo + " vs " + cachedClaim);

            // System.out.println("Leaving a claim");
            Component leave = null, sub = null;
            if (leaveTitle != null && !leaveTitle.isEmpty()) {
                leave = miniMessage.deserialize(leaveTitle.replace("%player%", cachedClaim.getOwnerName()));
            } else leave = Component.empty();
            if (leaveSubtitle != null && !leaveSubtitle.isEmpty()) {
                sub = miniMessage.deserialize(leaveSubtitle.replace("%player%", cachedClaim.getOwnerName()));
            } else sub = Component.empty();
            claimMap.remove(player.getUniqueId());
            //System.out.println("Title: " + leave + " | " + sub);
            Title title = Title.title(leave, sub);
            // player.showTitle(title);
            player.showTitle(title);

            if (leaveActionbar != null && !leaveActionbar.isEmpty()) {
                // player.sendActionBar(miniMessage.deserialize(leaveActionbar.replace("%player%", cachedClaim.getOwnerName())));
                player.sendActionBar(miniMessage.deserialize(leaveActionbar.replace("%player%", cachedClaim.getOwnerName())));
            }
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
    
    /**
     * Load existing disabled users from the disabled directory
     */
    private void loadDisabledUsers() {
        disabledUsers.clear();
        File[] files = disabledDirectory.listFiles();
        if (files != null) {
            for (File file : files) {
                try {
                    UUID uuid = UUID.fromString(file.getName());
                    disabledUsers.add(uuid);
                } catch (IllegalArgumentException e) {
                    // Invalid UUID filename, ignore
                }
            }
        }
    }
    
    /**
     * Check if a player has titles disabled
     * @param playerUuid The player's UUID
     * @return true if titles are disabled for this player
     */
    private boolean isTitlesDisabled(UUID playerUuid) {
        return disabledUsers.contains(playerUuid);
    }
    
    /**
     * Toggle titles for a player
     * @param playerUuid The player's UUID
     * @return true if titles are now enabled, false if now disabled
     */
    private boolean toggleTitles(UUID playerUuid) {
        File userFile = new File(disabledDirectory, playerUuid.toString());
        
        if (disabledUsers.contains(playerUuid)) {
            // Currently disabled, enable titles
            disabledUsers.remove(playerUuid);
            if (userFile.exists()) {
                userFile.delete();
            }
            return true; // Now enabled
        } else {
            // Currently enabled, disable titles
            disabledUsers.add(playerUuid);
            try {
                userFile.createNewFile(); // Create empty file
            } catch (IOException e) {
                getLogger().warning("Could not create disabled file for player " + playerUuid + ": " + e.getMessage());
            }
            return false; // Now disabled
        }
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("gptitles.toggle")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        if (command.getName().equalsIgnoreCase("toggleclaimtitles")) {
            boolean nowEnabled = toggleTitles(player.getUniqueId());
            if (nowEnabled) {
                player.sendMessage(ChatColor.GREEN + "Claim titles are now enabled!");
            } else {
                player.sendMessage(ChatColor.YELLOW + "Claim titles are now disabled!");
            }
            return true;
        }
        
        return false;
    }
}
