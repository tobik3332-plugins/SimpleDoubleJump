package com.rarehyperion.simpledoublejump;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DoubleJumpManager implements Listener {
    private final SimpleDoubleJump plugin;
    private final Set<UUID> fallingProtections = new HashSet<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public DoubleJumpManager(SimpleDoubleJump plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
        if (plugin.getConfig().getBoolean("avoid-fly-conflicts", true) && player.isFlying()) return;

        if (player.isOnGround()) {
            if (!player.getAllowFlight()) {
                Region region = plugin.getRegionManager().findRegionAt(player.getLocation());
                if (region != null && hasPermission(player, region)) {
                    player.setAllowFlight(true);
                }
            }
        } else {
            Region region = plugin.getRegionManager().findRegionAt(player.getLocation());
            if (region == null && player.getAllowFlight()) {
                player.setAllowFlight(false);
            }
        }
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        Region region = plugin.getRegionManager().findRegionAt(player.getLocation());
        if (region == null || !hasPermission(player, region)) return;

        event.setCancelled(true);
        player.setAllowFlight(false);
        player.setFlying(false);

        // Cooldown check
        if (region.getCooldown() > 0) {
            long now = System.currentTimeMillis();
            cooldowns.putIfAbsent(player.getUniqueId(), new HashMap<>());
            Map<String, Long> playerCd = cooldowns.get(player.getUniqueId());
            long lastUsed = playerCd.getOrDefault(region.getName().toLowerCase(), 0L);
            if (now - lastUsed < region.getCooldown() * 1000L) {
                return;
            }
            playerCd.put(region.getName().toLowerCase(), now);
        }

        // Apply Double Jump Velocity
        Vector dir = player.getLocation().getDirection().setY(0).normalize();
        if (dir.lengthSquared() == 0) {
            dir = player.getLocation().getDirection();
        }
        Vector velocity = dir.multiply(region.getJumpHorizontal()).setY(region.getJumpVertical());
        player.setVelocity(velocity);

        // Effects & Sounds
        Location loc = player.getLocation();
        if (region.getEffect() != null) {
            loc.getWorld().spawnParticle(region.getEffect(), loc, region.getEffectCount(), region.getEffectSpread(), region.getEffectSpread(), region.getEffectSpread(), 0.05);
        }
        if (region.getSound() != null) {
            loc.getWorld().playSound(loc, region.getSound(), region.getSoundVolume(), region.getSoundPitch());
        }

        if (plugin.getConfig().getBoolean("fall-damage-protection", true)) {
            fallingProtections.add(player.getUniqueId());
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                if (fallingProtections.remove(player.getUniqueId())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private boolean hasPermission(Player player, Region region) {
        if (!player.hasPermission("sdj.use")) return false;
        if (player.hasPermission("sdj.area.use.*")) return true;
        return player.hasPermission("sdj.area.use." + region.getName().toLowerCase());
    }
}
