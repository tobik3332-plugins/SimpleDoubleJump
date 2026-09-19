package com.rarehyperion.simpledoublejump;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class SimpleDoubleJump extends JavaPlugin implements CommandExecutor, TabCompleter {
    private RegionManager regionManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.regionManager = new RegionManager(this);

        if (getCommand("sdj") != null) {
            getCommand("sdj").setExecutor(this);
            getCommand("sdj").setTabCompleter(this);
        }

        getServer().getPluginManager().registerEvents(new DoubleJumpManager(this), this);
        getLogger().getLogger().info("SimpleDoubleJump úspěšně spuštěn!");
    }

    @Override
    public void onDisable() {
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sdj.admin")) {
            sender.sendMessage(ChatColor.RED + "Nemáš oprávnění sdj.admin!");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            regionManager.loadAll();
            sender.sendMessage(ChatColor.GREEN + "SimpleDoubleJump konfigurace a oblasti byly reloadnuty!");
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("tparea")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Tento příkaz mohou používat pouze hráči.");
                return true;
            }
            Region reg = regionManager.getRegion(args[1]);
            if (reg == null || reg.getPos1() == null) {
                player.sendMessage(ChatColor.RED + "Oblast neexistuje nebo nemá nastavenou pozici.");
                return true;
            }
            player.teleport(reg.getPos1());
            player.sendMessage(ChatColor.GREEN + "Byl jsi teleportován na oblast " + reg.getName());
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("area")) {
            String subAction = args[1].toLowerCase();
            if (subAction.equals("create") && args.length == 3) {
                String regName = args[2];
                Region pending = regionManager.getPending(regName);
                if (pending == null) {
                    regionManager.startCreation(regName);
                    sender.sendMessage(ChatColor.YELLOW + "Zahájeno vytváření oblasti '" + regName + "'. Nyní nastav pos1 a pos2 a příkaz zopakuj pro potvrzení.");
                } else {
                    regionManager.confirmCreation(regName);
                    sender.sendMessage(ChatColor.GREEN + "Oblast '" + regName + "' byla úspěšně vytvořena a uložena!");
                }
                return true;
            }

            if (subAction.equals("pos1") && args.length == 3) {
                if (!(sender instanceof Player player)) return true;
                String regName = args[2];
                Region reg = regionManager.getPending(regName);
                if (reg == null) reg = regionManager.getRegion(regName);
                if (reg == null) {
                    sender.sendMessage(ChatColor.RED + "Nejprve zadej /sdj area create " + regName);
                    return true;
                }
                reg.setPos1(player.getLocation());
                if (regionManager.getPending(regName) == null) reg.save();
                sender.sendMessage(ChatColor.GREEN + "Pozice pos1 pro oblast " + regName + " byla nastavena.");
                return true;
            }

            if (subAction.equals("pos2") && args.length == 3) {
                if (!(sender instanceof Player player)) return true;
                String regName = args[2];
                Region reg = regionManager.getPending(regName);
                if (reg == null) reg = regionManager.getRegion(regName);
                if (reg == null) {
                    sender.sendMessage(ChatColor.RED + "Nejprve zadej /sdj area create " + regName);
                    return true;
                }
                reg.setPos2(player.getLocation());
                if (regionManager.getPending(regName) == null) reg.save();
                sender.sendMessage(ChatColor.GREEN + "Pozice pos2 pro oblast " + regName + " byla nastavena.");
                return true;
            }

            if (args.length >= 4) {
                String regName = args[2];
                Region reg = regionManager.getRegion(regName);
                if (reg == null) {
                    sender.sendMessage(ChatColor.RED + "Oblast '" + regName + "' nenalezena.");
                    return true;
                }

                try {
                    switch (subAction) {
                        case "jumpvertical":
                            reg.setJumpVertical(Double.parseDouble(args[3]));
                            reg.save();
                            sender.sendMessage(ChatColor.GREEN + "Jump vertical nastaven.");
                            break;
                        case "jumphorizontal":
                            reg.setJumpHorizontal(Double.parseDouble(args[3]));
                            reg.save();
                            sender.sendMessage(ChatColor.GREEN + "Jump horizontal nastaven.");
                            break;
                        case "cooldown":
                            reg.setCooldown(Long.parseLong(args[3]));
                            reg.save();
                            sender.sendMessage(ChatColor.GREEN + "Cooldown nastaven.");
                            break;
                        case "effect":
                            reg.setEffect(Particle.valueOf(args[3].toUpperCase()));
                            reg.save();
                            sender.sendMessage(ChatColor.GREEN + "Efekt nastaven.");
                            break;
                        case "effect-count":
                            reg.setEffectCount(Integer.parseInt(args[3]));
                            reg.save();
                            sender.sendMessage(ChatColor.GREEN + "Počet efektů nastaven.");
                            break;
                        case "effect-spread":
                            reg.setEffectSpread(Double.parseDouble(args[3]));
                            reg.save();
                            sender.sendMessage(ChatColor.GREEN + "Rozptyl efektu nastaven.");
                            break;
                        case "sound":
                            reg.setSound(Sound.valueOf(args[3].toUpperCase()));
                            reg.save();
                            sender.sendMessage(ChatColor.GREEN + "Zvuk nastaven.");
                            break;
                        case "sound-pitch":
                            reg.setSoundPitch(Float.parseFloat(args[3]));
                            reg.save();
                            sender.sendMessage(ChatColor.GREEN + "Pitch zvuku nastaven.");
                            break;
                        case "sound_volume":
                            reg.setSoundVolume(Float.parseFloat(args[3]));
                            reg.save();
                            sender.sendMessage(ChatColor.GREEN + "Volume zvuku nastaven.");
                            break;
                    }
                } catch (Exception e) {
                    sender.sendMessage(ChatColor.RED + "Neplatná hodnota příkazu.");
                }
                return true;
            }
        }

        sender.sendMessage(ChatColor.RED + "Použití: /sdj reload, /sdj tparea <nazev>, nebo /sdj area ...");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List. lesquels = new ArrayList<>();
        List<String> result = new ArrayList<>();

        if (args.length == 1) {
            result.add("reload");
            result.add("area");
            result.add("tparea");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("tparea")) {
            for (Region r : regionManager.getRegions()) {
                result.add(r.getName());
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("area")) {
            result.add("create");
            result.add("pos1");
            result.add("pos2");
            result.add("jumpvertical");
            result.add("jumphorizontal");
            result.add("cooldown");
            result.add("effect");
            result.add("effect-count");
            result.add("effect-spread");
            result.add("sound");
            result.add("sound-pitch");
            result.add("sound_volume");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("area")) {
            for (Region r : regionManager.getRegions()) {
                result.add(r.getName());
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("area")) {
            String sub = args[1].toLowerCase();
            if (sub.equals("effect")) {
                for (Particle p : Particle.values()) {
                    result.add(p.name());
                }
            } else if (sub.equals("sound")) {
                for (Sound s : Sound.values()) {
                    result.add(s.name());
                }
            }
        }

        String current = args[args.length - 1].toLowerCase();
        List<String> finalResult = new ArrayList<>();
        for (String s : result) {
            if (s.toLowerCase().startsWith(current)) {
                finalResult.add(s);
            }
        }
        return finalResult;
    }
}
