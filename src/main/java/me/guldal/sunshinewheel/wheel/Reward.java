package me.guldal.sunshinewheel.wheel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;

public final class Reward {

    /** MiniMessage biçiminde; <chance> yazılırsa o anki yüzdeyle değişir. */
    public String name;
    public double weight;
    public int color;
    public Material icon;
    /** Konsoldan çalıştırılır; %player% %reward% %wheel% değişir. */
    public final List<String> commands = new ArrayList<>();
    /** Boşsa herkes kazanabilir. */
    public String permission = "";
    /** Boşsa çarkın varsayılan kazanma mesajı kullanılır; <chance> desteklenir. */
    public String winMessage = "";
    /** Boşsa çarkın kazanma sesi kullanılır. */
    public String winSound = "";
    /** Eşya olarak verilecekse adet. */
    public int amount = 1;
    /** Açıkken ikon eşyası kazananın envanterine verilir (komutlara ek olarak). */
    public boolean giveItem;
    /** Eşya verilirken parlama efekti eklenir. */
    public boolean glow;
    /** Kapalı ödüller çarkta görünmez ve kazanılamaz. */
    public boolean enabled = true;
    /** Eşya verilirken eklenen açıklama satırları (MiniMessage). */
    public final List<String> lore = new ArrayList<>();
    /** 0 = yok. */
    public int customModelData;

    /** Teslim kaynağı: vanilla malzeme, birebir kopya ya da bir eşya eklentisi kimliği. */
    public ItemSource itemSource = ItemSource.VANILLA;
    /** EXACT kaynağı için yakalanan eşyanın Base64 (NBT) kopyası. */
    public String itemBase64 = "";
    /** Sağlayıcı kaynakları için eşya kimliği (ör. Oraxen id, ItemsAdder namespace:id). */
    public String providerId = "";

    /** Base64 eşya kopyası için üst sınır (karakter). */
    public static final int MAX_ITEM_BASE64 = 32768;
    /** Sağlayıcı kimliği için üst sınır (karakter). */
    public static final int MAX_PROVIDER_ID = 256;

    public Reward(String name, double weight, int color, Material icon, List<String> commands) {
        this.name = name;
        this.weight = weight;
        this.color = color;
        this.icon = icon;
        this.commands.addAll(commands);
    }

    static Reward fromMap(Map<?, ?> map) {
        String name = string(map, "name", "Reward");
        double weight = map.get("weight") instanceof Number n ? n.doubleValue() : 1.0;
        int color = parseColor(string(map, "color", "#808080"));
        Material icon = Material.matchMaterial(string(map, "icon", "CHEST"));
        List<String> commands = new ArrayList<>();
        if (map.get("commands") instanceof List<?> list) {
            list.forEach(o -> commands.add(String.valueOf(o)));
        }
        Reward reward = new Reward(name, weight, color,
                icon == null || !icon.isItem() ? Material.CHEST : icon, commands);
        reward.permission = string(map, "permission", "");
        reward.winMessage = string(map, "win-message", "");
        reward.winSound = string(map, "win-sound", "");
        reward.amount = map.get("amount") instanceof Number n ? Math.min(64, Math.max(1, n.intValue())) : 1;
        reward.giveItem = map.get("give-item") instanceof Boolean b ? b : false;
        reward.glow = map.get("glow") instanceof Boolean g ? g : false;
        reward.enabled = !(map.get("enabled") instanceof Boolean e) || e;
        if (map.get("lore") instanceof List<?> loreList) {
            loreList.forEach(o -> reward.lore.add(String.valueOf(o)));
        }
        reward.customModelData = map.get("custom-model-data") instanceof Number c
                ? Math.max(0, c.intValue()) : 0;
        reward.itemSource = ItemSource.parse(string(map, "item-source", "VANILLA"), ItemSource.VANILLA);
        String base64 = string(map, "item-base64", "");
        reward.itemBase64 = base64.length() <= MAX_ITEM_BASE64 ? base64 : "";
        String provider = string(map, "provider-id", "");
        reward.providerId = provider.length() <= MAX_PROVIDER_ID ? provider : "";
        return reward;
    }

    /** Vanilla dışı bir teslim kaynağı ayarlı mı? */
    public boolean hasCustomItem() {
        return itemSource != null && itemSource != ItemSource.VANILLA;
    }

    /** Hiçbir teslimat tanımlı değil mi (ne eşya ne komut)? */
    public boolean hasNoPayout() {
        return !giveItem && commands.isEmpty();
    }

    Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("weight", weight);
        map.put("color", String.format("#%06X", color & 0xFFFFFF));
        map.put("icon", icon.name());
        map.put("commands", new ArrayList<>(commands));
        map.put("permission", permission);
        map.put("win-message", winMessage);
        map.put("win-sound", winSound);
        map.put("amount", amount);
        map.put("give-item", giveItem);
        map.put("glow", glow);
        map.put("enabled", enabled);
        map.put("lore", new ArrayList<>(lore));
        map.put("custom-model-data", customModelData);
        map.put("item-source", itemSource == null ? ItemSource.VANILLA.name() : itemSource.name());
        map.put("item-base64", itemBase64);
        map.put("provider-id", providerId);
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
