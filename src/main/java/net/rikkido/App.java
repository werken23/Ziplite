package net.rikkido;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class App extends JavaPlugin implements Listener, CommandExecutor {

    List<MovePlayer> mplayer;

    Boolean DEBUG = false;

    public ZipLineManager ziplineManager;
    public PlayerZippingManager zippingManager;
    public ZipLineVisualizeManager visualManger;
    public ItemManager itemManager;
    public Debugger debugger;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1)
            return false;
        var arg = args[0];
        if (arg == null)
            return false;
        if (arg.equals("DEBUG")) {
            var p = (Player) sender;
            itemManager.dropDebugStick(p.getLocation(), 1);
        }

        if (arg.equals(("delete"))) {
            var p = (Player) sender;
            var slimes = p.getWorld().getNearbyEntities(p.getLocation(), 1, 1, 1);
            for (var s : slimes) {
                p.sendMessage(String.format("delete: ", s.getLocation()));
                s.remove();
            }
        }
        return true;
    }

    @Override
    public void onEnable() {
        ziplineManager = new ZipLineManager(this);
        zippingManager = new PlayerZippingManager(this);
        visualManger = new ZipLineVisualizeManager(this);
        itemManager = new ItemManager(this);
        debugger = new Debugger(this);

        Bukkit.getPluginManager().registerEvents(ziplineManager, this);
        Bukkit.getPluginManager().registerEvents(zippingManager, this);
        Bukkit.getPluginManager().registerEvents(visualManger, this);
        Bukkit.getPluginManager().registerEvents(debugger, this);
        DataManager.setValues(this);

    }
}
