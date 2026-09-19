package com.rarehyperion.simpledoublejump;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class Region {
    private final String name;
    private String worldName;
    private int x1, y1, z1, x2, y2, z2;
    private double jumpVertical = 1.0;
    private double jumpHorizontal = 1.0;
    private long cooldown = 0; // v sekundách
    private Particle effect = Particle.CAMPFIRE_COSY_SMOKE;
    private int effectCount = 10;
    private double effectSpread = 0.5;
    private Sound sound = Sound.ENTITY_BAT_TAKEOFF;
    private float soundPitch = 1.0f;
    private float soundVolume = 1.0f;

    private final File file;

    public Region(String name, File file) {
        this.name = name;
        this.file = file;
    }

    public boolean contains(Location loc) {
        if (loc.getWorld() == null || !loc.getWorld().getName().equals(worldName)) return false;
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        return x >= Math.min(x1, x2) && x <= Math.max(x1, x2) &&
               y >= Math.min(y1, y2) && y <= Math.max(y1, y2) &&
               z >= Math.min(z1, z2) && z <= Math.max(z1, z2);
    }

    public void save() {
        FileConfiguration config = new YamlConfiguration();
        config.set("world", worldName);
        config.set("x1", x1);
        config.set("y1", y1);
        config.set("z1", z1);
        config.set("x2", x2);
        config.set("y2", y2);
        config.set("z2", z2);
        config.set("jump-vertical", jumpVertical);
        config.set("jump-horizontal", jumpHorizontal);
        config.set("cooldown", cooldown);
        config.set("effect", effect.name());
        config.set("effect-count", effectCount);
        config.set("effect-spread", effectSpread);
        config.set("sound", sound.name());
        config.set("sound-pitch", soundPitch);
        config.set("sound-volume", soundVolume);

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void load() {
        if (!file.exists()) return;
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        worldName = config.getString("world", "world");
        x1 = config.getInt("x1");
        y1 = config.getInt("y1");
        z1 = config.getInt("z1");
        x2 = config.getInt("x2");
        y2 = config.getInt("y2");
        z2 = config.getInt("z2");
        jumpVertical = config.getDouble("jump-vertical", 1.0);
        jumpHorizontal = config.getDouble("jump-horizontal", 1.0);
        cooldown = config.getLong("cooldown", 0);
        
        try {
            effect = Particle.valueOf(config.getString("effect", "CAMPFIRE_COSY_SMOKE"));
        } catch (Exception ignored) {}
        
        effectCount = config.getInt("effect-count", 10);
        effectSpread = config.getDouble("effect-spread", 0.5);
        
        try {
            sound = Sound.valueOf(config.getString("sound", "ENTITY_BAT_TAKEOFF"));
        } catch (Exception ignored) {}
        
        float p = (float) config.getDouble("sound-pitch", 1.0);
        soundPitch = p;
        float v = (float) config.getDouble("sound-volume", 1.0);
        soundVolume = v;
    }

    // Getters and Setters
    public String getName() { return name; }
    public String getWorldName() { return worldName; }
    public void setWorldName(String worldName) { this.worldName = worldName; }
    public void setPos1(Location loc) {
        this.worldName = loc.getWorld().getName();
        this.x1 = loc.getBlockX();
        this.y1 = loc.getBlockY();
        this.z1 = loc.getBlockZ();
    }
    public void setPos2(Location loc) {
        this.x2 = loc.getBlockX();
        this.y2 = loc.getBlockY();
        this.z2 = loc.getBlockZ();
    }
    public Location getPos1() {
        World w = Bukkit.getWorld(worldName);
        return w != null ? new Location(w, x1, y1, z1) : null;
    }
    public double getJumpVertical() { return jumpVertical; }
    public void setJumpVertical(double jumpVertical) { this.jumpVertical = jumpVertical; }
    public double getJumpHorizontal() { return jumpHorizontal; }
    public void setJumpHorizontal(double jumpHorizontal) { this.jumpHorizontal = jumpHorizontal; }
    public long getCooldown() { return cooldown; }
    public void setCooldown(long cooldown) { this.cooldown = cooldown; }
    public Particle getEffect() { return effect; }
    public void setEffect(Particle effect) { this.effect = effect; }
    public int getEffectCount() { return effectCount; }
    public void setEffectCount(int effectCount) { this.effectCount = effectCount; }
    public double getEffectSpread() { return effectSpread; }
    public void setEffectSpread(double effectSpread) { this.effectSpread = effectSpread; }
    public Sound getSound() { return sound; }
    public void setSound(Sound sound) { this.sound = sound; }
    public float getSoundPitch() { return soundPitch; }
    public void setSoundPitch(float soundPitch) { this.soundPitch = soundPitch; }
    public float getSoundVolume() { return soundVolume; }
    public void setSoundVolume(float soundVolume) { this.soundVolume = soundVolume; }
}
