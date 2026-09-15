package me.guldal.sunshinewheel;

import java.util.Objects;
import me.guldal.sunshinewheel.editor.DialogEditor;
import me.guldal.sunshinewheel.editor.EditorManager;
import me.guldal.sunshinewheel.wheel.WheelManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SunshineWheel extends JavaPlugin {

    private WheelManager manager;
    private EditorManager editors;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        manager = new WheelManager(this);
        manager.load();
        DialogEditor dialogs = new DialogEditor(manager);
        editors = new EditorManager(this, manager, dialogs);

        WheelCommand command = new WheelCommand(manager, dialogs, editors);
        PluginCommand pluginCommand = Objects.requireNonNull(getCommand("wheel"), "plugin.yml'de wheel komutu yok");
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);

        getServer().getPluginManager().registerEvents(new WheelListener(this, manager, dialogs, editors), this);
        manager.spawnAll();
    }

    @Override
    public void onDisable() {
        if (editors != null) {
            editors.closeAll();
        }
        if (manager != null) {
            manager.despawnAll();
        }
    }
}
