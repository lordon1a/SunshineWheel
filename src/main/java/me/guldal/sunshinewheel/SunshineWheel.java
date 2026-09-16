package me.guldal.sunshinewheel;

import java.util.Objects;
import me.guldal.sunshinewheel.papi.WheelPlaceholders;
import me.guldal.sunshinewheel.web.WebServer;
import me.guldal.sunshinewheel.wheel.WheelManager;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SunshineWheel extends JavaPlugin {

    /** Register the plugin at https://bstats.org and put the numeric id here. 0 = disabled. */
    private static final int BSTATS_PLUGIN_ID = 0;

    private WheelManager manager;
    private WebServer web;
    private UpdateChecker updates;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Messages.init(this);
        manager = new WheelManager(this);
        manager.load();

        web = new WebServer(this, manager);
        web.start();

        updates = new UpdateChecker(this);
        updates.start();

        if (BSTATS_PLUGIN_ID > 0) {
            new Metrics(this, BSTATS_PLUGIN_ID);
            getLogger().info("bStats enabled (id " + BSTATS_PLUGIN_ID + ").");
        }

        WheelCommand command = new WheelCommand(manager, web);
        PluginCommand pluginCommand = Objects.requireNonNull(getCommand("wheel"), "plugin.yml'de wheel komutu yok");
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);

        getServer().getPluginManager().registerEvents(new WheelListener(this, manager), this);
        manager.spawnAll();

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new WheelPlaceholders(manager, getPluginMeta().getVersion()).register();
            getLogger().info("PlaceholderAPI hooked: %sunshinewheel_*% active.");
        }
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            getLogger().info("Vault hooked: money costs enabled.");
        }
    }

    @Override
    public void onDisable() {
        if (updates != null) {
            updates.stop();
        }
        if (web != null) {
            web.stop();
        }
        if (manager != null) {
            manager.despawnAll();
        }
    }
}
