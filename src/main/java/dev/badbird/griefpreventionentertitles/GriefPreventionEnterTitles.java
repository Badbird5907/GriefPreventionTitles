package dev.badbird.griefpreventionentertitles;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GriefPreventionEnterTitles extends JavaPlugin implements Listener {

    private static Map<UUID, Claim> claimMap = new HashMap<>();

    private static MiniMessage miniMessage;

    @Override
    public void onEnable() {
        miniMessage = MiniMessage.miniMessage();
        if (!getDataFolder().exists()) getDataFolder().mkdir();
        if (!new File(getDataFolder(), "config.yml").exists()) saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
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

        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
