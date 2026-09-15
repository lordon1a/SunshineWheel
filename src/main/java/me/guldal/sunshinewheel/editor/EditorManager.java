package me.guldal.sunshinewheel.editor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import me.guldal.sunshinewheel.wheel.WheelInstance;
import me.guldal.sunshinewheel.wheel.WheelManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/** Oyuncu başına açık editör panelleri. */
public final class EditorManager {

    private final Plugin plugin;
    private final WheelManager wheels;
    private final DialogEditor dialogs;
    private final Map<UUID, EditorSession> sessions = new HashMap<>();

    public EditorManager(Plugin plugin, WheelManager wheels, DialogEditor dialogs) {
        this.plugin = plugin;
        this.wheels = wheels;
        this.dialogs = dialogs;
    }

    public EditorSession session(UUID playerId) {
        return sessions.get(playerId);
    }

    public EditorSession open(Player player, WheelInstance wheel) {
        close(player.getUniqueId());
        // Yazı gerektiren ayarlar (ad, sesler, maliyet, ödül komutları) dialog ekranında düzenlenir.
        EditorSession session = new EditorSession(plugin, wheels, player, wheel,
                () -> dialogs.open(player, wheel));
        sessions.put(player.getUniqueId(), session);
        session.open();
        return session;
    }

    public boolean close(UUID playerId) {
        EditorSession session = sessions.remove(playerId);
        if (session == null) {
            return false;
        }
        session.close();
        return true;
    }

    public void closeAll() {
        sessions.values().forEach(EditorSession::close);
        sessions.clear();
    }
}
