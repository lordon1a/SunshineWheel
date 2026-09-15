package me.guldal.sunshinewheel;

import java.io.File;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Localized messages. English (messages.yml) is the default; set "language: tr"
 * in config.yml for Turkish (messages_tr.yml). Placeholders look like {name}.
 */
public final class Messages {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static JavaPlugin plugin;
    private static YamlConfiguration messages = new YamlConfiguration();

    private Messages() {
    }

    public static void init(JavaPlugin plugin) {
        Messages.plugin = plugin;
        reload();
    }

    public static void reload() {
        if (plugin == null) {
            return;
        }
        String lang = plugin.getConfig().getString("language", "en").toLowerCase(Locale.ROOT);
        String file = lang.equals("tr") ? "messages_tr.yml" : "messages.yml";
        plugin.saveResource(file, false);
        messages = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), file));
    }

    /** Raw template with {placeholders} filled, MiniMessage tags untouched. */
    public static String fill(String key, String... pairs) {
        String text = messages.getString(key);
        if (text == null) {
            return "<red>Missing message: " + key;
        }
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            text = text.replace("{" + pairs[i] + "}", pairs[i + 1]);
        }
        return text;
    }

    public static Component mm(String key, String... pairs) {
        return MM.deserialize(fill(key, pairs));
    }

    public static Component mm(String key) {
        return MM.deserialize(fill(key));
    }
}
