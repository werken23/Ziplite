package net.rikkido;

import java.util.ArrayList;
import java.util.List;

import com.destroystokyo.paper.MaterialTags;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LeashHitch;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import net.rikkido.Event.PlayerHandZiplineItemHandler;
import net.rikkido.Event.ZiplineEnterPlayerRangeHandler;

public class ZiplineManager implements Listener {

    private static boolean DEBUG = false;
    private Zipline _plugin;
    static String CUSTOM_NAME = "Rope";

    private Double _ziplineMaxRadius;

    public ZiplineManager(Zipline plugin) {
        _plugin = plugin;
        _ziplineMaxRadius = _plugin.config.ziplineConfig.MaxRadius.value;

        _plugin.eventDispatcher.addDispatcher((p) -> {
            return dispatchZiplineEnterPlayerRange(p);
        });
        _plugin.eventDispatcher.addDispatcher((p) -> {
            return dispatchPlayerHandZipline(p);
        });


    }

    public boolean dispatchPlayerHandZipline(Player player) {
        var handItem = player.getInventory().getItemInMainHand();
        if (handItem.getType() != Material.LEAD)
            return false;
        if (!_plugin.ziplimeitem.isItem(handItem))
            return false;
        var event = new PlayerHandZiplineItemHandler(player);
        _plugin.getServer().getPluginManager().callEvent(event);
        return true;
    }

    public boolean dispatchZiplineEnterPlayerRange(Player a) {
        var slimes = ZiplineManager.getPathSlimes(a.getLocation(), 20f, 20f, 20f);
        if (slimes.size() < 1)
            return false;
        var event = new ZiplineEnterPlayerRangeHandler(a, slimes);
        _plugin.getServer().getPluginManager().callEvent(event);
        return true;
    }

    //TODO replace by config entry
    //public void enableDebugMode(boolean flag) {
    //    DEBUG = flag;
    //}

    private static Chunk ensureChunk(Location loc) {
        var chunk = loc.getChunk();
        if (!chunk.isLoaded()) {
            chunk.load(true);
        }
        return chunk;
    }

    private static PathSlime mergePathSlime(List<PathSlime> slimes) {

        if (slimes.size() <= 1)
            return slimes.get(0);
        var mainSlime = slimes.get(0);
        var data = mainSlime.getPathData();
        // slimes.remove(0);
        for (int idx = 1; idx < slimes.size(); idx++) {
            var slime = slimes.get(idx);
            var sdata = slime.getPathData();
            for (var loc : sdata)
                if (!data.contains(loc))
                    data.add(loc);
            slime.getSlime().remove();
        }
        mainSlime.setPathData(data);
        return mainSlime;

    }

    public PathSlime getPathSlime(Location loc) {
        var path_slime = getPathSlimes(loc, 0.5f, 0.5f, 0.5f);
        if (path_slime.size() < 1) {
            return null;
        }

        return mergePathSlime(path_slime);
    }

    public static List<PathSlime> getPathSlimes(Location loc, Float x, Float y, Float z) {
        var chunk = ensureChunk(loc);
        var cloc = loc.clone();
        cloc.add(0.5, 0.25, 0.5);
        var entities = cloc.getWorld().getNearbyEntities(cloc, x, y, z);
        List<PathSlime> path_slime = new ArrayList<PathSlime>();
        entities.stream().filter(s -> s.getType().equals(EntityType.SLIME))
                .forEach(ent -> path_slime.add(new PathSlime(ent)));
        chunk.unload();
        return path_slime;
    }

    public boolean verifyPath(PathSlime slime) {
        var paths = slime.getPathData();
        var loc = slime.getSlime().getLocation();
        for (var path : paths) {
            var chunk = ensureChunk(path);
            var dSlime = getPathSlime(path);
            if (dSlime == null) {
                chunk.unload();
                return false;
            }
            var result = dSlime.getPathData().stream().filter(f -> f.equals(loc)).toList();
            if (result.size() < 1) {
                chunk.unload();
                return false;
            }
            chunk.unload();
        }
        return true;

    }

    // pathの構成が正しく構成されているかをチェック
    @EventHandler
    public void onPathEnterPlayerRange(ZiplineEnterPlayerRangeHandler e) {
        for (var slime : e.getSlimes()) {
            var slimeLoc = slime.getSlime().getLocation();
            var world = slime.getSlime().getWorld();
            var block = world.getBlockAt(slimeLoc);
            if (MaterialTags.FENCES.isTagged(block.getType()))
                continue;
            destroyPath(block.getLocation());
        }
    }

    // path破壊処理
    @EventHandler
    public void onPlayerBreakPathFence(BlockBreakEvent e) {
        var block = e.getBlock();
        if (!MaterialTags.FENCES.isTagged(block.getType()))
            return;
        if (!destroyPath(block.getLocation())) {
            e.getPlayer().sendMessage("経路の削除に失敗しました。");
        }
    }

    // Destruction by explosion
    @EventHandler
    public void onExplodedEvent(BlockExplodeEvent e) {
        var breakBlock = e.blockList();
        for (Block block : breakBlock) {
            destroyPath(block.getLocation());
        }
    }

    // Destruction due to burnout
    @EventHandler
    public void onBlockBurned(BlockBurnEvent e) {
        var block = e.getBlock();
        destroyPath(block.getLocation());
    }

    public boolean destroyPath(Location location) {

        var chunk = ensureChunk(location);

        if (!chunk.isLoaded())
            return false;

        if (getPathSlimes(location, 0.3f, 0.5f, 0.3f).size() < 1)
            return true;
        var pathSlime = getPathSlime(location);
        var itemAmount = rmPath(pathSlime);

        _plugin.ziplimeitem.dropItem(location, itemAmount);
        chunk.unload();
        return true;
    }

    public int rmPath(PathSlime pathslime) {
        if (pathslime == null)
            return 0;
        List<Location> paths = pathslime.getPathData();
        if (DEBUG)
            _plugin.getLogger().info("path list size: " + paths.size());

        if (paths != null) {
            for (Location location : paths) {
                var connectPathSlime = getPathSlime(location);
                List<Location> connectList = connectPathSlime.getPathData();
                if (DEBUG)
                    _plugin.getLogger().info("bfore list size: " + connectList.size());

                connectList.remove(pathslime.getSlime().getLocation());
                if (DEBUG)
                    _plugin.getLogger().info("after list size: " + connectList.size());

                if (connectList.size() < 1)
                    connectPathSlime.getSlime().remove();
                connectPathSlime.setPathData(connectList);

            }
        }
        pathslime.getSlime().remove();
        return paths.size();
    }

    @EventHandler
    public void onPlayerLeashEntity(PlayerLeashEntityEvent e) {
        var item = e.getPlayer().getInventory().getItemInMainHand();
        if (DEBUG)
            _plugin.getLogger().info("leash: " + e.getEntity().getType());
        if (item.getType() != Material.LEAD)
            return;
        var meta = item.getItemMeta();
        item.setItemMeta(meta);
    }

    // Slime death nullification
    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        var entity = e.getEntity();
        if (entity.getType() != EntityType.SLIME)
            return;
        if (!(new PathSlime(entity)).hasPathData())
            return;
        e.setCancelled(true);
    }

    private Object[] spawnHitches(PathSlime[] slimes) {
        var res = new ArrayList<LeashHitch>();
        // If they're going to disappear anyway, make sure they're generated in the same place as the fence.
        for (PathSlime slime : slimes) {
            var hitch = spawnHitch(slime);
            res.add(hitch);
            slime.getSlime().setLeashHolder(hitch);
        }
        return res.toArray();
    }

    public LeashHitch spawnHitch(PathSlime slime) {
        var world = slime.getSlime().getWorld();
        var hithes = world.getNearbyEntities(slime.getSlime().getLocation(), 1, 1, 1).stream()
                .filter(s -> s.getType() == EntityType.LEASH_KNOT).toList();
        if (hithes.size() > 0) {
            return (LeashHitch) hithes.get(0);
        }
        var hitch = world.spawnEntity(slime.getSlime().getLocation(), EntityType.LEASH_KNOT);
        return (LeashHitch) hitch;

    }

    private PathSlime[] spawnSlimes(Location spawnLocation, Location destLocation) {

        var src = spawnSlime(spawnLocation);
        var dst = spawnSlime(destLocation);

        List<Location> src_data = new ArrayList<Location>();
        if (src.hasPathData())
            src_data = src.getPathData();
        if (!src_data.contains(destLocation))
            src_data.add(destLocation);
        src.setPathData(src_data);

        List<Location> dst_data = new ArrayList<Location>();
        if (dst.hasPathData())
            dst_data = dst.getPathData();
        if (!dst_data.contains(spawnLocation))
            dst_data.add(spawnLocation);
        dst.setPathData(dst_data);

        return new PathSlime[]{ src, dst };
    }

    public PathSlime spawnSlime(Location spawnLoc) {
        var chunk = ensureChunk(spawnLoc);
        var slime = getPathSlime(spawnLoc);
        if (slime == null) {
            slime = new PathSlime(spawnLoc);
        }
        chunk.unload();
        return slime;
    }

    @EventHandler
    public void onInteractByZiplineItem(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        var clicked_block = event.getClickedBlock();
        if (!MaterialTags.FENCES.isTagged(clicked_block.getType()))
            return;
        var player = event.getPlayer();
        var items = player.getInventory().getItemInMainHand();
        if (items.getType() != Material.LEAD)
            return;
        setUpZiplines(clicked_block, player);
    }

    @EventHandler
    public void onPathSlimeUnleash(HangingBreakByEntityEvent e) {
        var entity = e.getEntity();
        if (entity.getType() != EntityType.LEASH_KNOT)
            return;
        if (entity.getCustomName() == null)
            return;
        if (!(new PathSlime(entity)).hasPathData())
            return;
        e.setCancelled(true);
    }

    public void setUpZiplines(Block clicked_block, Player player) {
        var items = player.getInventory().getItemInMainHand();
        if (items.getType() != Material.LEAD)
            return;
        if (!_plugin.ziplimeitem.isItem(items)) {
            player.sendMessage("The recipe has been changed due to an upgrade.\nAt the crafting table, the item can be created by clamping the lead between iron ingots");
            return;
        }

        var world = player.getWorld();
        var dst_loc = clicked_block.getLocation().add(0.5, 0.25, 0.5);

        // 一回目
        // 座標データを格納
        if (!_plugin.ziplimeitem.isZiplineFlaged(items)) {
            _plugin.ziplimeitem.setZiplineFlag(items, dst_loc);
            if (DEBUG)
                _plugin.getLogger().info("container wrote");

            return;
        }

        // After the second time
        var src_loc = _plugin.ziplimeitem.getZiplineFlag(items);
        if (DEBUG)
            _plugin.getLogger().info("container readed");

        var diff = src_loc.toVector().subtract(dst_loc.toVector());
        // Lines at the same location are canceled.
        if (src_loc.equals(dst_loc)) {
            _plugin.ziplimeitem.removeZiplineFlag(items);
            return;
        }

        if (diff.length() <= 3.0f) {
            player.sendMessage("Short distance connections are not possible.");
            return;
        }

        var maxRadius = _ziplineMaxRadius;
        if (diff.length() >= maxRadius && maxRadius > 0) {
            player.sendMessage(String.format("Lines cannot be installed for distances greater than %.3f blocks.", maxRadius));
            return;
        }

        if (!MaterialTags.FENCES.isTagged(world.getBlockAt(src_loc).getType())) {
            player.sendMessage("Something has happened at the starting point. Delete the connection.");
            _plugin.ziplimeitem.removeZiplineFlag(items);
            return;
        }

        var path = getPathSlime(src_loc);
        if (path != null)
            if (path.getPathData().contains(dst_loc)) {
                player.sendMessage("No double dipping, I'll erase the route.");
                _plugin.ziplimeitem.removeZiplineFlag(items);
                return;
            }

        var slimes = spawnSlimes(src_loc, dst_loc);
        spawnHitches(slimes);

        // Consuming lead
        _plugin.ziplimeitem.setAmount(items, items.getAmount() - 1);
    }

}
