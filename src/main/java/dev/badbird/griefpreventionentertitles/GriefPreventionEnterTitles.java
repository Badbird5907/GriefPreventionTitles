package dev.badbird.griefpreventionentertitles;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.World;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class GriefPreventionEnterTitles extends JavaPlugin implements Listener {

    private static Map<UUID, Claim> claimMap = new HashMap<>();

    private static MiniMessage miniMessage;

    private static String enterTitle;
    private static String enterSubtitle;
    private static String leaveTitle;
    private static String leaveSubtitle;
    private static String enterActionbar;
    private static String leaveActionbar;
    private FileConfiguration config;

    private BukkitAudiences adventure;

    public BukkitAudiences adventure() {
        if(this.adventure == null) {
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        }
        return this.adventure;
    }

    @Override
    public void onEnable() {
        adventure = BukkitAudiences.create(this);
        miniMessage = MiniMessage.miniMessage();
        if (!getDataFolder().exists()) getDataFolder().mkdir();
        if (!new File(getDataFolder() +  "/config.yml").exists()) saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
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
        System.out.println("Player moved");
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
        if (cachedClaim == null && movingTo != null) { //Entering a claim
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
            adventure().player(player).showTitle(title);

            if (enterActionbar != null && !enterActionbar.isEmpty()) {
                // player.sendActionBar(miniMessage.deserialize(enterActionbar.replace("%player%", movingTo.getOwnerName())));
                adventure().player(player).sendActionBar(miniMessage.deserialize(enterActionbar.replace("%player%", movingTo.getOwnerName())));
            }
        } else if (cachedClaim != null && movingTo == null) { //Leaving a claim
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
            adventure().player(player).showTitle(title);

            if (leaveActionbar != null && !leaveActionbar.isEmpty()) {
                // player.sendActionBar(miniMessage.deserialize(leaveActionbar.replace("%player%", cachedClaim.getOwnerName())));
                adventure().player(player).sendActionBar(miniMessage.deserialize(leaveActionbar.replace("%player%", cachedClaim.getOwnerName())));
            }
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
