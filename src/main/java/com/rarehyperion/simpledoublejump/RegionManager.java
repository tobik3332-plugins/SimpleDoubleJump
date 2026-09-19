package com.rarehyperion.simpledoublejump;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RegionManager {
    private final JavaPlugin plugin;
    private final Map<String, Region> regions = new HashMap<>();
    private final Map<String, Region> pendingCreation = new HashMap<>();

    public RegionManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadAll();
    }

    public void loadAll() {
        regions.clear();
        File folder = new File(plugin.getDataFolder(), "areas");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String name = file.getName().replace(".yml", "");
                Region region = new Region(name, file);
                region.load();
                regions.put(name.toLowerCase(), region);
            }
        }
    }

    public Region getRegion(String name) {
        return regions.get(name.toLowerCase());
    }

    public Collection<Region> getRegions() {
        return regions.values();
    }

    public void startCreation(String name) {
        File file = new File(new File(plugin.getDataFolder(), "areas"), name + ".yml");
        pendingCreation.put(name.toLowerCase(), new Region(name, file));
    }

    public Region getPending(String name) {
        return pendingCreation.get(name.toLowerCase());
    }

    public void confirmCreation(String name) {
        Region reg = pendingCreation.remove(name.toLowerCase());
        if (reg != null) {
            reg.save();
            regions.put(name.toLowerCase(), reg);
        }
    }

    public Region findRegionAt(org.bukkit.Location loc) {
        for (Region reg : regions.values()) {
            if (reg.contains(loc)) {
                return reg;
            }
        }
        return null;
    }
}
