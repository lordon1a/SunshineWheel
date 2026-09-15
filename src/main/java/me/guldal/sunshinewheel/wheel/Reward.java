package me.guldal.sunshinewheel.wheel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;

public final class Reward {

    /** MiniMessage biçiminde. */
    public String name;
    public double weight;
    public int color;
    public Material icon;
    /** Konsoldan çalıştırılır; %player% oyuncu adıyla değişir. */
    public final List<String> commands = new ArrayList<>();

    public Reward(String name, double weight, int color, Material icon, List<String> commands) {
        this.name = name;
        this.weight = weight;
        this.color = color;
        this.icon = icon;
        this.commands.addAll(commands);
    }

    static Reward fromMap(Map<?, ?> map) {
        String name = string(map, "name", "Ödül");
        double weight = map.get("weight") instanceof Number n ? n.doubleValue() : 1.0;
        int color = parseColor(string(map, "color", "#808080"));
        Material icon = Material.matchMaterial(string(map, "icon", "CHEST"));
        List<String> commands = new ArrayList<>();
        if (map.get("commands") instanceof List<?> list) {
            list.forEach(o -> commands.add(String.valueOf(o)));
        }
        return new Reward(name, weight, color, icon == null ? Material.CHEST : icon, commands);
    }

    Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("weight", weight);
        map.put("color", String.format("#%06X", color & 0xFFFFFF));
        map.put("icon", icon.name());
        map.put("commands", new ArrayList<>(commands));
        return map;
    }

    private static String string(Map<?, ?> map, String key, String fallback) {
        Object value = map.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    public static int parseColor(String hex) {
        try {
            return Integer.parseInt(hex.replace("#", ""), 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return 0x808080;
        }
    }
}
