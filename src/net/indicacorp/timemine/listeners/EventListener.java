package net.indicacorp.timemine.listeners;

import net.indicacorp.timemine.TimeMine;
import net.indicacorp.timemine.data.Database;
import org.bukkit.*;
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

    private TimeMine plugin = TimeMine.getInstance();
    private static ArrayList<Material> TOOLS = new ArrayList<Material>(){{
        add(Material.DIAMOND_PICKAXE);
        add(Material.IRON_PICKAXE);
        add(Material.STONE_PICKAXE);
        add(Material.GOLDEN_PICKAXE);
    }};

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        final Database database = new Database();

        try {
            Player player = event.getPlayer();
            Block block = event.getBlock();
            int x = block.getX();
            int y = block.getY();
            int z = block.getZ();
            World world = player.getWorld();
            String sql = "SELECT * FROM timemine WHERE x = " + x + " AND y = " + y + " AND z = " + z + " AND world = '" + world.getName() + "' LIMIT 1";
            ResultSet results = database.query(sql);
            if (results == null || !results.first())
                return;

            event.setCancelled(true);
            if (player.getGameMode().equals(GameMode.CREATIVE))
                return;

            Material originalBlock = Material.getMaterial(results.getString("originalBlock"));
            Material dropItem = Material.getMaterial(results.getString("dropItem"));
            int dropItemCount = results.getInt("dropItemCount");
            boolean isMined = results.getBoolean("isMined");

            if (isMined) {
                player.sendMessage("This block can not be mined currently!");
                return;
            }

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
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            database.closeConnection();
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Location loc = event.getLocation();
        String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "";
        final Database database = new Database();
        final Iterator<Block> blocks = event.blockList().iterator();
        try {
            //Using clone as location can be mutated by contained methods
            Block min = loc.clone().getBlock().getRelative(-10, -10, -10);
            Block max = loc.clone().getBlock().getRelative(10, 10, 10);

            //Optimizing results returned with where clause
            final String sql = "SELECT x, y, z, world FROM timemine"
                                    + "WHERE x >= " + min.getX() + " AND x <= " + max.getX()
                                    + "AND   y >= " + min.getY() + " AND y <= " + max.getY()
                                    + "AND   z >= " + min.getZ() + " AND z <= " + max.getZ()
                                    + "AND world = '" + worldName + "'";
            final ResultSet results = database.query(sql);
            final ArrayList<Block> existing = new ArrayList<>();
            if(results == null)
                return;

            while (results.next()) {
                //We can assume (because of our query) that all the results are in the current world
                //Explosions can't happen cross world as far as I know
                final Block block = loc.getWorld()
                        .getBlockAt(results.getInt("x"), results.getInt("y"), results.getInt("z"));
                existing.add(block);
            }
            while (blocks.hasNext()) {
                final Block block = blocks.next();
                if (existing.contains(block)) blocks.remove();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            database.closeConnection();
        }
    }
}
