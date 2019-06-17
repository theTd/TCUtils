package me.thetd.tcutils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Sign;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.spigotmc.event.entity.EntityDismountEvent;
import org.totemcraftmc.bukkitplugin.TCBaseLib.util.PluginUtil;

import java.util.Objects;

public class PublicMinecart extends TCUtilsModule implements Listener {
    private final static String NAME = ChatColor.RED + "一次性矿车";

    private final static ItemStack[] DUMMY_CONTENT = new ItemStack[9];

    static {
        ItemStack item = new ItemStack(Material.MINECART);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(NAME);
        item.setItemMeta(meta);
        DUMMY_CONTENT[0] = item;

        for (int i = 1; i < 9; i++) {
            DUMMY_CONTENT[i] = null;
        }
    }

    private boolean realDispense = false;

    public PublicMinecart() {
        super("PublicMinecart");
    }

    @Override
    protected void load() throws Exception {
        Bukkit.getPluginManager().registerEvents(this, getPlugin());
    }

    @Override
    public void unload() {
        PluginUtil.unregisterListener(this);
    }

    private void writeDummyDispenserContent(Block block) {
        ItemStack[] content = new ItemStack[DUMMY_CONTENT.length];
        for (int i = 0; i < DUMMY_CONTENT.length; i++) {
            content[i] = DUMMY_CONTENT[i];
            if (content[i] != null) {
                content[i] = content[i].clone();
            }
        }
        ((InventoryHolder) block.getState()).getInventory().setContents(content);
    }

    @EventHandler
    void onDispenser(BlockDispenseEvent e) {
        if (realDispense) return;

        if (e.getBlock().getType() != Material.DISPENSER) return;

        BlockFace face = null;
        if (e.getBlock().getData() == 8) face = BlockFace.DOWN;
        if (e.getBlock().getData() == 9) face = BlockFace.UP;
        if (e.getBlock().getData() == 10) face = BlockFace.NORTH;
        if (e.getBlock().getData() == 11) face = BlockFace.SOUTH;
        if (e.getBlock().getData() == 12) face = BlockFace.WEST;
        if (e.getBlock().getData() == 13) face = BlockFace.EAST;

        Block target = e.getBlock().getRelative(face);
        if (target.getType() == Material.RAIL ||
                target.getType() == Material.ACTIVATOR_RAIL ||
                target.getType() == Material.DETECTOR_RAIL ||
                target.getType() == Material.POWERED_RAIL) {
            // check activate
            if (isActivatedDispenser(e.getBlock())) {
                // prepare real dispense

                e.setCancelled(true);

                Bukkit.getScheduler().runTask(getPlugin(), () -> {
                    // save inv and fire real dispense
                    ItemStack[] inv = ((InventoryHolder) e.getBlock().getState()).getInventory().getContents();
                    ItemStack[] copied = new ItemStack[inv.length];
                    for (int i = 0; i < inv.length; i++) {
                        copied[i] = inv[i];
                        if (copied[i] != null) {
                            copied[i] = copied[i].clone();
                        }
                    }

                    realDispense = true;
                    writeDummyDispenserContent(e.getBlock());
                    ((Dispenser) e.getBlock().getState()).dispense();
                    realDispense = false;

                    Bukkit.getScheduler().runTask(getPlugin(),
                            () -> ((InventoryHolder) e.getBlock().getState()).getInventory().setContents(copied));
                });
            }
        }
    }

    @EventHandler
    void onCartCreate(EntitySpawnEvent e) {
        if (e.getEntity().getType() == EntityType.MINECART &&
                Objects.equals(e.getEntity().getCustomName(), NAME)) {
            e.getEntity().setCustomNameVisible(true);
        }
    }

    @EventHandler
    void onOffCart(EntityDismountEvent e) {
        if (e.getDismounted().getType() == EntityType.MINECART) {
            if (Objects.equals(e.getDismounted().getCustomName(), NAME)) {
                e.getDismounted().remove();
            }
        }
    }

    @EventHandler
    void onOffline(PlayerQuitEvent e) {
        if (e.getPlayer().getVehicle() != null) {
            if (e.getPlayer().getVehicle().getType() == EntityType.MINECART) {
                if (Objects.equals(e.getPlayer().getVehicle().getCustomName(), NAME)) {
                    if (!e.getPlayer().getVehicle().getPassengers().isEmpty())
                        e.getPlayer().getVehicle().eject();
                    e.getPlayer().getVehicle().remove();
                }
            }
        }
    }

    @EventHandler
    void onCartDestroy(VehicleDamageEvent e) {
        if (e.getVehicle().getType() == EntityType.MINECART &&
                Objects.equals(e.getVehicle().getCustomName(), NAME)) {
            e.setCancelled(true);
            if (!e.getVehicle().getPassengers().isEmpty()) e.getVehicle().eject();
            e.getVehicle().remove();
        }
    }

    @EventHandler
    void onCartDestroy(VehicleDestroyEvent e) {
        if (e.getVehicle().getType() == EntityType.MINECART &&
                Objects.equals(e.getVehicle().getCustomName(), NAME)) {
            e.setCancelled(true);
            if (!e.getVehicle().getPassengers().isEmpty()) e.getVehicle().eject();
            e.getVehicle().remove();
        }
    }

    private boolean isActivatedDispenser(Block block) {
        return isValidSign(block.getRelative(BlockFace.UP)) ||
                isValidSign(block.getRelative(BlockFace.DOWN)) ||
                isValidSign(block.getRelative(BlockFace.EAST)) ||
                isValidSign(block.getRelative(BlockFace.SOUTH)) ||
                isValidSign(block.getRelative(BlockFace.WEST)) ||
                isValidSign(block.getRelative(BlockFace.NORTH));
    }

    private boolean isValidSign(Block block) {
        if (block.getType().name().endsWith("WALL_SIGN") ||
                block.getType().name().endsWith("_SIGN")) {
            Sign blockSign = (Sign) block.getState();
            String line1 = blockSign.getLine(0);
            String line2 = blockSign.getLine(1);
            String line3 = blockSign.getLine(2);
            String line4 = blockSign.getLine(3);
            return isValidSignContent(line1, line2, line3, line4);
        }
        return false;
    }

    private boolean isValidSignContent(String line1, String line2, String line3, String line4) {
        return line1 == null || line1.trim().isEmpty() &&
                Objects.equals("[PublicMinecart]", line2 == null ? null : line2.trim()) &&
                line3 == null || line3.trim().isEmpty() &&
                line4 == null || line4.trim().isEmpty();
    }


}
