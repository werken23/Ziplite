package net.rikkido;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.rikkido.Event.PlayerHandZiplineItemHandler;
import net.rikkido.Event.ZiplineEnterPlayerRangeHandler;

public class ZiplineVisualizeManager implements Listener {

    Zipline _plugin;
    double particlePerBlock = 1.0;
    int stage = 0;
    static Double STAGEMAX = 20.0;

    public ZiplineVisualizeManager(Zipline plugin) {
        _plugin = plugin;
        new BukkitRunnable() {
            @Override
            public void run() {
                stage++;
                if (stage >= STAGEMAX)
                    stage = 0;
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    @EventHandler
    public void onPlayerHandZiplineItem(PlayerHandZiplineItemHandler event) {
        var player = event.getPlayer();
        var handItem = player.getInventory().getItemInMainHand();
        var ziplineMaxRadius = _plugin.config.ziplineConfig.MaxRadius.value;

        if (ziplineMaxRadius > 0)
            if (_plugin.ziplimeitem.isZiplineFlaged(handItem)) {
                var color = TextColor.color(255, 255, 0);
                var distance = _plugin.ziplimeitem.getZiplineFlag(handItem).distance(player.getLocation());
                if (distance > ziplineMaxRadius)
                    color = TextColor.color(255, 0, 0);
                player.sendActionBar(Component
                        .text(String.format("Distance %.1f / %.1f blocks. Cancel by selecting start point again!",
                                distance,
                                ziplineMaxRadius))
                        .color(color));
                return;
            }

        if (_plugin.ziplimeitem.isZiplineFlaged(handItem)) {

            player.sendActionBar(Component
                    .text(String.format("Distance %.1f blocks. Cancel by selecting the starting point again!",
                            _plugin.ziplimeitem.getZiplineFlag(handItem).distance(player.getLocation())))
                    .color(TextColor.color(255, 255, 0)));
            return;
        }

        player.sendActionBar(Component
                .text("Not set")
                .color(TextColor.color(255, 255, 0)));

    }

    @EventHandler
    public void onPlayerEnterRange(ZiplineEnterPlayerRangeHandler event) {
        var slimes = event.getSlimes();
        for (var slime : slimes) {
            var nextLocation = slime.getPathData();
            for (var next : nextLocation) {
                spanwParticleLines(slime.getSlime().getLocation(), next, stage);
            }
        }
    }

    // Spawn Particle Lines between source and destination
    public void spanwParticleLines(Location source, Location destination, int stage) {
        var world = source.getWorld();
        if (world != destination.getWorld())
            return; // Same world only

        // var particlePerBlock = 2.0;
        var vector = source.toVector().subtract(destination.toVector()); // 基準はsource
        var vec = vector.clone().normalize();
        int count = (int) ((vector.length() / vec.length()));

        Particle.DustOptions opt = new Particle.DustOptions(Color.YELLOW, 1.0F);

        var progress = ((STAGEMAX - stage)) / STAGEMAX;// 0-1
        var one = 1 / STAGEMAX;

        Double blockPerProgress = count * progress;

        for (var a = count * (progress - one); a <= blockPerProgress; a += 1 / particlePerBlock) {
            var aa = vec.clone().multiply(a); // ax
            var particlePoint = destination.clone().add(aa); // + b
            world.spawnParticle(Particle.DUST, particlePoint, 1, opt);
        }
    }

}
