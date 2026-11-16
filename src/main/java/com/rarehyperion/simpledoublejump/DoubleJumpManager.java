package com.rarehyperion.simpledoublejump;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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
    private final Map<UUID, Integer> airJumpsUsed = new HashMap<>();

    public DoubleJumpManager(SimpleDoubleJump plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
        if (plugin.getConfig().getBoolean("avoid-fly-conflicts", true) && player.isFlying()) return;

        Region region = plugin.getRegionManager().findRegionAt(player.getLocation());

        if (player.isOnGround()) {
            // Po dopadu na zem vynulujeme použité skoky ve vzduchu
            airJumpsUsed.remove(player.getUniqueId());

            if (region != null && hasPermission(player, region)) {
                if (!player.getAllowFlight()) {
                    player.setAllowFlight(true);
                }
            } else {
                if (player.getAllowFlight()) {
                    player.setAllowFlight(false);
                }
            }
        } else {
            // Hráč je ve vzduchu – pokud opustí oblast nebo ztratí oprávnění, okamžitě vypneme možnost dalšího skoku
            if (region == null || !hasPermission(player, region)) {
                if (player.getAllowFlight()) {
                    player.setAllowFlight(false);
                }
                airJumpsUsed.remove(player.getUniqueId());
            } else {
                // Pokud hráč skočil z okraje (nespouštěl double jump, ale je stále v oblasti), povolíme mu skok ve vzduchu
                int used = airJumpsUsed.getOrDefault(player.getUniqueId(), 0);
                int max = region.getMaxAirJumps();
                if ((max == -1 || used < max) && !player.getAllowFlight()) {
                    player.setAllowFlight(true);
                }
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
        player.setFlying(false);

        int used = airJumpsUsed.getOrDefault(player.getUniqueId(), 0);
        int max = region.getMaxAirJumps();

        // Kontrola, zda hráč již nevyčerpal povolený počet skoků ve vzduchu
        if (max != -1 && used >= max) {
            player.setAllowFlight(false);
            return;
        }

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

        // Zvýšíme počítadlo použitých skoků
        used++;
        airJumpsUsed.put(player.getUniqueId(), used);

        // Pokud má hráč k dispozici ještě další skoky (nebo je nastaveno multiple), necháme allowFlight zapnuté
        if (max == -1 || used < max) {
            player.setAllowFlight(true);
        } else {
            player.setAllowFlight(false);
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

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        fallingProtections.remove(uuid);
        airJumpsUsed.remove(uuid);
        cooldowns.remove(uuid);
    }

    private boolean hasPermission(Player player, Region region) {
        if (!player.hasPermission("sdj.use")) return false;
        if (player.hasPermission("sdj.area.use.*")) return true;
        return player.hasPermission("sdj.area.use." + region.getName().toLowerCase());
    }
}
