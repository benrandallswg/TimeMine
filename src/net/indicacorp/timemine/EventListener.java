package net.indicacorp.timemine;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;

public class EventListener implements Listener {
    TimeMine plugin;

    public EventListener(TimeMine instance) { plugin = instance; }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        final Database database = new Database(plugin);

        try {
            Player player = event.getPlayer();
            Block block = event.getBlock();
            final int x = block.getX();
            final int y = block.getY();
            final int z = block.getZ();
            final World world = player.getWorld();
            String sql = "SELECT * FROM timemine WHERE x = " + x + " AND y = " + y + " AND z = " + z + " AND world = '" + world.getName() + "' LIMIT 1";
            ResultSet results = database.query(sql);
            if (results != null && results.first()) {
                event.setCancelled(true);
                if (player.getGameMode().equals(GameMode.CREATIVE)) return;

                final Material originalBlock = Material.getMaterial(results.getString("originalBlock"));
                final Material dropItem = Material.getMaterial(results.getString("dropItem"));
                final int dropItemCount = results.getInt("dropItemCount");
                final boolean isMined = results.getBoolean("isMined");

                if (isMined) {
                    player.sendMessage("This block can not be mined currently!");
                } else {
                    final ArrayList<Material> TOOLS = new ArrayList<>();
                    TOOLS.add(Material.DIAMOND_PICKAXE);
                    TOOLS.add(Material.IRON_PICKAXE);
                    TOOLS.add(Material.STONE_PICKAXE);
                    TOOLS.add(Material.GOLDEN_PICKAXE);

                    if (TOOLS.contains(player.getInventory().getItemInMainHand().getType())) {
                        block.getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(dropItem, dropItemCount));
                    }
                    sql = "UPDATE timemine SET isMined = 1, minedAt = CURRENT_TIMESTAMP WHERE x = " + x + " AND y = " + y + " AND z = " + z + " AND world = '" + world.getName() + "'";
                    database.insertOrUpdate(sql);
                    block.setType(originalBlock);
                    ItemStack handItem = player.getInventory().getItemInMainHand();
                    ItemMeta meta = handItem.getItemMeta();
                    if(meta instanceof Damageable) {
                        ((Damageable) meta).setDamage(((Damageable) meta).getDamage() + 1);
                        handItem.setItemMeta(meta);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            database.closeConnection();
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        final Database database = new Database(plugin);
        final Iterator<Block> blocks = event.blockList().iterator();
        try {
            final String sql = "SELECT x, y, z, world FROM timemine";
            final ResultSet results = database.query(sql);
            final ArrayList<Block> existing = new ArrayList<>();
            if (results != null) {
                while (results.next()) {
                    final Block block = Bukkit.getServer()
                            .getWorld(results.getString("world"))
                            .getBlockAt(results.getInt("x"), results.getInt("y"), results.getInt("z"));
                    existing.add(block);
                }
                while (blocks.hasNext()) {
                    final Block block = blocks.next();
                    if (existing.contains(block)) blocks.remove();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            database.closeConnection();
        }
    }
}
