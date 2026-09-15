package me.guldal.sunshinewheel.wheel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

/** Bir çarkın kalıcı ayarları (wheels.yml). Editör bu alanları düzenler. */
public final class WheelDefinition {

    public final String id;

    // Genel
    public String name;
    public String subtitle = "<gray>Çevirmek için sağ tıkla";

    // Konum
    public String world;
    public double x;
    public double y;
    public double z;
    public float yaw;

    // Görünüm
    public double pixelSize = 0.03;
    public int imageSize = 128;

    // Ödül eşyaları
    public boolean itemsUpright = true;
    public double itemScale = 0.45;

    // Animasyon
    public int duration = 140;
    public double revolutions = 7.0;
    public MotionStyle style = MotionStyle.DECELERATE;

    // Maliyet ve bilet
    public Material ticketItem;
    public int ticketAmount = 1;
    /** Çevirmeden önce konsolda çalışır; %player% oyuncu adıyla değişir (ör. eco take %player% 500). */
    public final List<String> costCommands = new ArrayList<>();

    // Bekleme süresi (saniye, 0 = kapalı)
    public int cooldownSeconds;

    // Hologramlar
    public boolean showHologram = true;
    public double hologramOffset = 0.35;

    // Sesler ("anahtar ses perde", boş ya da "yok" kapatır)
    public String soundStart = "minecraft:block.note_block.pling 1 1.4";
    public String soundTick = "minecraft:ui.button.click 0.5 1.3";
    public String soundLanding = "minecraft:block.note_block.chime 1 1.2";
    public String soundWin = "minecraft:entity.player.levelup 1 1.1";

    // Parçacıklar
    public String particleWin = "TOTEM_OF_UNDYING";
    public int particleCount = 80;

    public final List<Reward> rewards = new ArrayList<>();

    public WheelDefinition(String id) {
        this.id = id;
        this.name = "<gold><bold>" + id;
    }

    static WheelDefinition load(String id, ConfigurationSection s) {
        WheelDefinition d = new WheelDefinition(id);
        d.name = s.getString("name", d.name);
        d.subtitle = s.getString("subtitle", d.subtitle);
        d.world = s.getString("location.world", "world");
        d.x = s.getDouble("location.x");
        d.y = s.getDouble("location.y");
        d.z = s.getDouble("location.z");
        d.yaw = (float) s.getDouble("location.yaw");
        d.pixelSize = s.getDouble("appearance.pixel-size", d.pixelSize);
        d.imageSize = clamp(s.getInt("appearance.image-size", d.imageSize), 32, 256);
        d.itemsUpright = s.getBoolean("reward-items.upright", d.itemsUpright);
        d.itemScale = s.getDouble("reward-items.scale", d.itemScale);
        d.duration = Math.max(20, s.getInt("animation.duration", d.duration));
        d.revolutions = Math.max(1, s.getDouble("animation.revolutions", d.revolutions));
        try {
            d.style = MotionStyle.valueOf(s.getString("animation.style", d.style.name()));
        } catch (IllegalArgumentException ignored) {
            // Geçersiz değer: varsayılan kalır.
        }
        String ticket = s.getString("cost.ticket-item", "");
        d.ticketItem = ticket == null || ticket.isBlank() ? null : Material.matchMaterial(ticket);
        d.ticketAmount = Math.max(1, s.getInt("cost.ticket-amount", d.ticketAmount));
        d.costCommands.addAll(s.getStringList("cost.commands"));
        d.cooldownSeconds = Math.max(0, s.getInt("cooldown-seconds", d.cooldownSeconds));
        d.showHologram = s.getBoolean("holograms.show", d.showHologram);
        d.hologramOffset = s.getDouble("holograms.offset", d.hologramOffset);
        d.soundStart = s.getString("sounds.start", d.soundStart);
        d.soundTick = s.getString("sounds.tick", d.soundTick);
        d.soundLanding = s.getString("sounds.landing", d.soundLanding);
        d.soundWin = s.getString("sounds.win", d.soundWin);
        d.particleWin = s.getString("particles.win", d.particleWin);
        d.particleCount = Math.max(0, s.getInt("particles.count", d.particleCount));
        for (Map<?, ?> map : s.getMapList("rewards")) {
            d.rewards.add(Reward.fromMap(map));
        }
        return d;
    }

    void save(ConfigurationSection s) {
        s.set("name", name);
        s.set("subtitle", subtitle);
        s.set("location.world", world);
        s.set("location.x", x);
        s.set("location.y", y);
        s.set("location.z", z);
        s.set("location.yaw", yaw);
        s.set("appearance.pixel-size", pixelSize);
        s.set("appearance.image-size", imageSize);
        s.set("reward-items.upright", itemsUpright);
        s.set("reward-items.scale", itemScale);
        s.set("animation.duration", duration);
        s.set("animation.revolutions", revolutions);
        s.set("animation.style", style.name());
        s.set("cost.ticket-item", ticketItem == null ? "" : ticketItem.name());
        s.set("cost.ticket-amount", ticketAmount);
        s.set("cost.commands", new ArrayList<>(costCommands));
        s.set("cooldown-seconds", cooldownSeconds);
        s.set("holograms.show", showHologram);
        s.set("holograms.offset", hologramOffset);
        s.set("sounds.start", soundStart);
        s.set("sounds.tick", soundTick);
        s.set("sounds.landing", soundLanding);
        s.set("sounds.win", soundWin);
        s.set("particles.win", particleWin);
        s.set("particles.count", particleCount);
        List<Map<String, Object>> list = new ArrayList<>();
        rewards.forEach(r -> list.add(r.toMap()));
        s.set("rewards", list);
    }

    /** Örnek ödüller canlı sunucuda test edilirken ekonomiye dokunmaz; sadece oyuncuya mesaj gönderir. */
    static WheelDefinition withSampleRewards(String id) {
        WheelDefinition d = new WheelDefinition(id);
        d.rewards.add(sample("<gold>$500", 40, 0xE0463C, Material.GOLD_INGOT));
        d.rewards.add(sample("<yellow>Altın Elma", 12, 0xF2C230, Material.GOLDEN_APPLE));
        d.rewards.add(sample("<green>32 Tecrübe Şişesi", 10, 0x4FB548, Material.EXPERIENCE_BOTTLE));
        d.rewards.add(sample("<aqua>3 Elmas", 15, 0x3C8FE0, Material.DIAMOND));
        d.rewards.add(sample("<light_purple>$2.000", 20, 0xB04FD0, Material.EMERALD));
        d.rewards.add(sample("<red><bold>BÜYÜK ÖDÜL: Elytra", 3, 0xE8568F, Material.ELYTRA));
        return d;
    }

    private static Reward sample(String name, double weight, int color, Material icon) {
        String plain = name.replaceAll("<[^>]+>", "");
        return new Reward(name, weight, color, icon, List.of("tell %player% [Test] " + plain + " kazandın (gerçek ödül verilmedi)"));
    }

    public String summary() {
        return style.label() + ", " + duration + " tick, " + rewards.size() + " ödül";
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
