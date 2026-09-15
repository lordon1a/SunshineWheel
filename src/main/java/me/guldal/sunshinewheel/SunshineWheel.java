package me.guldal.sunshinewheel;

import java.util.Objects;
import me.guldal.sunshinewheel.papi.WheelPlaceholders;
import me.guldal.sunshinewheel.web.WebServer;
import me.guldal.sunshinewheel.wheel.WheelManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SunshineWheel extends JavaPlugin {

    private WheelManager manager;
    private WebServer web;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Messages.init(this);
        manager = new WheelManager(this);
        manager.load();

        web = new WebServer(this, manager);
        web.start();

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
        if (web != null) {
            web.stop();
        }
        if (manager != null) {
            manager.despawnAll();
        }
    }
}
