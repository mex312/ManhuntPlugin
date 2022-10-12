package manhuntplugin.manhuntplugin;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ManhuntPlugin extends JavaPlugin {
    private Player speedrunner = null;
    private LinkedList<Player> hunters = new LinkedList<>();
    private final Random rand = new Random();
    private boolean isManhuntActive = false;

    private final BukkitRunnable compassesUpdater = new BukkitRunnable() {
        @Override
        public void run() {
            updateCompasses();
        }
    };

    private void startManhunt() {
        if(speedrunner == null) {
            setSpeedrunner(new ArrayList<Player>(Bukkit.getServer().getOnlinePlayers()).get(rand.nextInt(Bukkit.getServer().getOnlinePlayers().size())));
        }
        for(Player player : Bukkit.getServer().getOnlinePlayers()) {
            player.getInventory().clear();
            if(player != speedrunner) {
                giveCompass(player);
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 5));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 5));
            } else {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 2));
            }
        }
        isManhuntActive = true;
        Bukkit.getServer().broadcastMessage(ChatColor.DARK_GREEN + "Man" + ChatColor.DARK_RED + "hunt" + ChatColor.WHITE + " has started!");
        for(Player player : Bukkit.getServer().getOnlinePlayers()) {
            if(player == speedrunner) {
                player.sendMessage("Kill the " + ChatColor.DARK_PURPLE + "Ender Dragon" + ChatColor.WHITE + " to win!");
            } else {
                player.sendMessage("Kill " + ChatColor.DARK_GREEN + speedrunner.getName() + ChatColor.WHITE + " to win!");
            }
        }
        Bukkit.getServer().getWorlds().forEach(world -> world.setGameRule(GameRule.MOB_GRIEFING, true));
    }

    private void setSpeedrunner(Player player) {
        speedrunner = player;
        hunters = new LinkedList<>(Bukkit.getServer().getOnlinePlayers());
        hunters.remove(player);
        player.getInventory().all(Material.COMPASS).forEach((i, item) -> {
            if(item.getItemMeta().hasCustomModelData()) {
                if (item.getItemMeta().getCustomModelData() == 8800) {
                    player.getInventory().remove(item);
                }
            }
        });
        Bukkit.getServer().broadcastMessage(player.getName() + " is now " + ChatColor.DARK_GREEN + "Speedrunner" + ChatColor.WHITE + "!");
    }

    private void updateCompasses() {
        LinkedList<ItemStack> compassesToUpdate = new LinkedList<>();
        for(Player hunter : hunters) {
            hunter.getInventory().all(Material.COMPASS).forEach((i, item) -> {
                if(item.getItemMeta().hasCustomModelData()) {
                    if (item.getItemMeta().getCustomModelData() == 8800) {
                        compassesToUpdate.add(item);
                    }
                }
            });
        }
        for (ItemStack compass : compassesToUpdate) {
            setCompass(compass);
        }
    }

    private void setCompass(ItemStack compass) {
        try {
            CompassMeta meta = (CompassMeta) compass.getItemMeta();
            if (meta == null) return;
            if (speedrunner != null) {
                Location location = speedrunner.getLocation().add(rand.nextInt(16) - 8, 0, rand.nextInt(16) - 8);
                meta.setLodestoneTracked(false);
                meta.setLodestone(location);
                meta.setLodestoneTracked(false);
            } else {
                meta.setLodestoneTracked(false);
                meta.setLodestone(null);
            }
            compass.setItemMeta(meta);
        } catch (Throwable ignored) {
            Bukkit.getLogger().info("Cannot set a compass");
        }
    }

    private boolean giveCompass(Player player) {
        AtomicBoolean hasCompass = new AtomicBoolean(false);
        player.getInventory().all(Material.COMPASS).forEach((i, item) -> {
            if(item.getItemMeta().getCustomModelData() == 8800) {
                hasCompass.set(true);
            }
        });
        if(!hasCompass.get()) {
            ItemStack compass = new ItemStack(Material.COMPASS);
            CompassMeta meta = (CompassMeta) compass.getItemMeta();
            assert meta != null;
            meta.setCustomModelData(8800);
            meta.setDisplayName(ChatColor.WHITE + "Kill " + ChatColor.DARK_GREEN + "him!");
            compass.setItemMeta(meta);
            player.getInventory().addItem(compass).size();
        }
        return hasCompass.get();
    }

    private class ManhuntCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            switch (command.getName()) {
                case "startmanhunt":
                    startManhunt();
                    break;

                case "stopmanhunt":
                    isManhuntActive = false;
                    Bukkit.getServer().broadcastMessage(ChatColor.DARK_GREEN + "Man" + ChatColor.DARK_RED + "hunt" + ChatColor.WHITE + " has been stopped...");
                    Bukkit.getServer().getWorlds().forEach(world -> world.setGameRule(GameRule.MOB_GRIEFING, false));
                    break;

                case "setspeedrunner":
                    Player player = Bukkit.getServer().getPlayer(args[0]);
                    if (player != null) {
                        setSpeedrunner(player);
                    } else {
                        sender.sendMessage("Can't find player with name '" + args[0] + "'!");
                    }
                    break;

                case "track":
                    if (sender instanceof Player) {
                        if(!isManhuntActive) {
                            sender.sendMessage(ChatColor.DARK_GREEN + "Man" + ChatColor.DARK_RED + "hunt" + ChatColor.WHITE + " hasn't started yet, just wait");
                        } else if(sender == speedrunner) {
                            sender.sendMessage("I don't think it will work...");
                        } else if(giveCompass((Player) sender)) {
                            sender.sendMessage("You already have a compass!");
                        } else {
                            sender.sendMessage("Track him down!");
                        }
                    }
                    break;
            }

            return false;
        }
    }

    private class ManhuntListener implements Listener {
        @EventHandler
        public void onPlayerRespawn(PlayerRespawnEvent e) {
            Bukkit.getLogger().info(e.getEventName());
            if(e.getPlayer() != speedrunner) {
                giveCompass(e.getPlayer());
            }
        }

        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent e) {
            if(speedrunner == null) {
                setSpeedrunner(e.getPlayer());
            } else {
                hunters.add(e.getPlayer());
            }
        }

        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent e) {
            if(e.getPlayer() == speedrunner) {
                speedrunner = null;
            } else {
                hunters.remove(e.getPlayer());
            }
        }

        @EventHandler
        public void onEntityDeath(EntityDeathEvent e) {
            Entity entity = e.getEntity();
            if(entity == speedrunner) {
                Bukkit.getServer().broadcastMessage(ChatColor.DARK_RED + "Hunters" + ChatColor.WHITE + " won!");
            }
            else if(entity.getType() == EntityType.ENDER_DRAGON) {
                Bukkit.getServer().broadcastMessage(ChatColor.DARK_GREEN + "Speedrunner" + ChatColor.WHITE + " won!");
            }
        }

        @EventHandler
        public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
            if(e.getDamager() == speedrunner && e.getEntity() instanceof Player) {
                if(!isManhuntActive) {
                    startManhunt();
                }
            } else if((e.getEntity() instanceof Player || e.getDamager() instanceof Player) && !isManhuntActive) {
                e.setCancelled(true);
            }
        }

        @EventHandler
        public void onEntityDamage(EntityDamageEvent e) {
            if(e.getEntity() instanceof Player && !isManhuntActive) {
                e.setCancelled(true);
            }
        }

        @EventHandler
        public void onBlockBreak(BlockBreakEvent e) {
            if(!isManhuntActive) e.setCancelled(true);
        }

        @EventHandler
        public void onPlayerPickupItem(PlayerPickupItemEvent e) {
            if(e.getPlayer() == speedrunner && e.getItem().getItemStack().getType() == Material.COMPASS && e.getItem().getItemStack().getItemMeta().getCustomModelData() == 8800) {
                e.setCancelled(true);
            }
        }
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        this.getCommand("setspeedrunner").setExecutor(new ManhuntCommand());
        this.getCommand("track").setExecutor(new ManhuntCommand());
        this.getCommand("stopmanhunt").setExecutor(new ManhuntCommand());
        this.getCommand("startmanhunt").setExecutor(new ManhuntCommand());
        Bukkit.getPluginManager().registerEvents(new ManhuntListener(), this);
        compassesUpdater.runTaskTimer(this, 0, 100);
        Bukkit.getLogger().info(Arrays.toString(Bukkit.getWorlds().toArray()));
        Bukkit.getServer().getWorlds().forEach(world -> world.setGameRule(GameRule.MOB_GRIEFING, false));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
