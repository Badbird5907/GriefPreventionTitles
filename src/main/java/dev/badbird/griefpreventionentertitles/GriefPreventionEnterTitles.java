package dev.badbird.griefpreventionentertitles;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GriefPreventionEnterTitles extends JavaPlugin implements Listener {

    private static Map<UUID, Claim> claimMap = new HashMap<>();

    private static MiniMessage miniMessage;

    private static String enterTitle;
    private static String enterSubtitle;
    private static String leaveTitle;
    private static String leaveSubtitle;

    @Override
    public void onEnable() {
        miniMessage = MiniMessage.miniMessage();
        if (!getDataFolder().exists()) getDataFolder().mkdir();
        if (!new File(getDataFolder(), "config.yml").exists()) saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        reloadConfig();
    }

    @Override
    public void reloadConfig() {
        String enterBase = "titles.enter";
        if (getConfig().getBoolean(enterBase + ".enabled")) {
            enterTitle = getConfig().getString(enterBase + ".title");
            enterSubtitle = getConfig().getString(enterBase + ".subtitle");
        }
        String leaveBase = "titles.leave";
        if (getConfig().getBoolean(leaveBase + ".enabled")) {
            leaveTitle = getConfig().getString(leaveBase + ".title");
            leaveSubtitle = getConfig().getString(leaveBase + ".subtitle");
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {

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
        Claim cachedClaim = claimMap.get(player.getUniqueId());
        Claim movingTo = GriefPrevention.instance.dataStore.getClaimAt(from, true, cachedClaim);
        if (cachedClaim == null && movingTo != null) { //Entering a claim
            Component enter = null, sub = null;
            if (enterTitle != null && !enterTitle.isEmpty()) {
                enter = miniMessage.deserialize(enterTitle);
            } else enter = Component.empty();
            if (enterSubtitle != null && !enterSubtitle.isEmpty()) {
                sub = miniMessage.deserialize(enterSubtitle);
            } else sub = Component.empty();
            Title title = Title.title(enter, sub);
            player.showTitle(title);
        } else if (cachedClaim != null && movingTo == null) { //Leaving a claim
            Component leave = null, sub = null;
            if (leaveTitle != null && !leaveTitle.isEmpty()) {
                leave = miniMessage.deserialize(leaveTitle);
            } else leave = Component.empty();
            if (leaveSubtitle != null && !leaveSubtitle.isEmpty()) {
                sub = miniMessage.deserialize(leaveSubtitle);
            } else sub = Component.empty();
            Title title = Title.title(leave, sub);
            player.showTitle(title);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
