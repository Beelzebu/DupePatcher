/*
 * This file is part of DupePatcher.
 *
 * Copyright © 2017 Beelzebu
 * DupePatcher is licensed under the GNU General Public License.
 *
 * DupePatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DupePatcher is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.nifheim.beelzebu.dupepatcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Beelzebu
 */
public class Main extends JavaPlugin implements Listener {

    private static Main instance;
    private final ConsoleCommandSender console = Bukkit.getConsoleSender();
    private final Set<Player> cant = new HashSet<>();

    public static Main getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        if (!new File(getDataFolder(), "config.yml").exists()) {
            getDataFolder().mkdirs();
            copy(getResource("config.yml"), new File(getDataFolder(), "config.yml"));
        }
        if (getConfig().getInt("version") == 1) {
            getConfig().set("Remove overstacked", true);
            getConfig().set("version", 2);
        }
        reloadConfig();

        console.sendMessage(" ");
        console.sendMessage("    §c+==========================+");
        console.sendMessage("    §c| §4DupePatcher §fBy: §7§cBeelzebu |");
        console.sendMessage("    §c|--------------------------|");
        console.sendMessage("    §c|         §4v:§f" + getDescription().getVersion() + "          §c|");
        console.sendMessage("    §c+==========================+");
        console.sendMessage(" ");
    }

    @Override
    public void onDisable() {
        console.sendMessage(" ");
        console.sendMessage("    §c+==========================+");
        console.sendMessage("    §c| §4DupePatcher §fBy: §7§cBeelzebu |");
        console.sendMessage("    §c|--------------------------|");
        console.sendMessage("    §c|         §4v:§f" + getDescription().getVersion() + "          §c|");
        console.sendMessage("    §c+==========================+");
        console.sendMessage(" ");
    }

    public void copy(InputStream in, File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
            in.close();
        } catch (IOException e) {
            Logger.getLogger(Main.class.getName()).log(Level.WARNING, "Can't copy the file " + file.getName() + " to the plugin data folder.", e.getCause());
        }
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent e) {
        add(Bukkit.getPlayer(e.getPlayer().getName()));
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent e) {
        e.getViewers().stream().forEachOrdered((he) -> {
            add(Bukkit.getPlayer(he.getName()));
        });
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent e) {
        if (cant.contains(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            remove(e.getPlayer());
        });
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            remove(e.getEntity());
        });
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (cant.contains(e.getPlayer())) {
            Location from = e.getFrom();
            Location to = e.getTo();
            if (from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ()) {
                InventoryView inv = e.getPlayer().getOpenInventory();
                if (inv.getType().equals(InventoryType.CRAFTING) || inv.getType().equals(InventoryType.WORKBENCH) || (e.getPlayer().getVehicle() != null && e.getPlayer().getVehicle().getType().equals(EntityType.PIG))) {
                    e.getPlayer().getOpenInventory().close();
                }
                int i = new Random().nextInt(40);
                while (i < 20) {
                    return;
                }
                Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> {
                    remove(e.getPlayer());
                }, i);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPortal(EntityPortalEvent e) {
        if (!e.isCancelled()) {
            if (getConfig().getBoolean("PortalFix.Strict Mode")) {
                if (!e.getEntityType().equals(EntityType.PLAYER)) {
                    e.setCancelled(true);
                }
            } else if (e.getEntityType().equals(EntityType.EXPERIENCE_ORB)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        if (getConfig().getBoolean("Remove overstacked", true)) {
            ItemStack clicked = e.getCurrentItem();
            ItemStack cursor = e.getCursor();
            if ((clicked != null && clicked.getType() != Material.AIR)) {
                if (clicked.getAmount() > clicked.getMaxStackSize() || clicked.getAmount() <= 0) {
                    e.setCancelled(true);
                    clicked.setAmount(0);
                    log("Detected an invalid stack size for " + e.getWhoClicked().getName());
                }
            }
            if (cursor != null && cursor.getType() != Material.AIR) {
                if (cursor.getAmount() > cursor.getMaxStackSize() || cursor.getAmount() <= 0) {
                    e.setCancelled(true);
                    cursor.setAmount(0);
                    log("Detected an invalid stack size for " + e.getWhoClicked().getName());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInvMove(InventoryMoveItemEvent e) {
        if (getConfig().getBoolean("Remove overstacked", true)) {
            if (e.getItem() != null && e.getItem().getType() != Material.AIR) {
                if (e.getItem().getAmount() > e.getItem().getMaxStackSize() || e.getItem().getAmount() <= 0) {
                    e.setCancelled(true);
                    e.getItem().setAmount(0);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent e) {
        if (getConfig().getBoolean("Remove overstacked", true)) {
            if (e.getItemDrop().getItemStack() != null && e.getItemDrop().getItemStack().getType() != Material.AIR) {
                if (e.getItemDrop().getItemStack().getAmount() > e.getItemDrop().getItemStack().getMaxStackSize() || e.getItemDrop().getItemStack().getAmount() <= 0) {
                    e.setCancelled(true);
                    e.getItemDrop().getItemStack().setAmount(0);
                    log("Detected an invalid stack size for " + e.getPlayer().getName());
                }
            }
        }
    }

    private void add(Player p) {
        if (!p.getName().equalsIgnoreCase("Beelzebu")) {
            if (!cant.contains(p)) {
                cant.add(p);
            }
        }
    }

    private void remove(Player p) {
        if (cant.contains(p)) {
            cant.remove(p);
        }
    }

    private void log(String msg) {
        console.sendMessage("§8[§cDupePatcher§8] §7" + msg);
    }
}
