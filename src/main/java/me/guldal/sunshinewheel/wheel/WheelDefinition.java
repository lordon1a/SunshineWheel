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
    public String subtitle = "<gray>Right-click to spin";
    public boolean enabled = true;
    /** Boşsa herkes çevirebilir. */
    public String permission = "";
    /** Açıkken kazanan herkese duyurulur. */
    public boolean broadcastWin;

    // Konum
    public String world;
    public double x;
    public double y;
    public double z;
    public float yaw;

    // Görünüm
    public double pixelSize = 0.03;
    public int imageSize = 128;
    public int frameColor = 0xD08A33;
    public int hubColor = 0xE2A44A;
    public int pointerColor = 0xF2B544;
    /** Çizim stili: "deluxe" (süslü, varsayılan) veya "classic" (eski düz çizim). */
    public String artStyle = "deluxe";
    /** Açıkken dilim boyutları ödül ağırlığıyla orantılı çizilir; görsel şansla eşleşir. */
    public boolean weightedSlices = true;

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
    /** Para maliyeti; 0 = yok (Vault gerekir). */
    public double moneyCost;
    /** Seviye maliyeti; 0 = yok. */
    public int xpCost;
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
    /** Dönüş sırasındaki ortam parçacığı; boş = yok. */
    public String spinParticle = "";
    public int spinParticleCount;
    public WinEffect winEffect = WinEffect.NONE;

    public final List<Reward> rewards = new ArrayList<>();

    public WheelDefinition(String id) {
        this.id = id;
        this.name = "<gold><bold>" + id;
    }

    static WheelDefinition load(String id, ConfigurationSection s) {
        WheelDefinition d = new WheelDefinition(id);
        d.name = s.getString("name", d.name);
        if (d.name == null) {
            d.name = "<gold><bold>" + id;
        }
        d.subtitle = s.getString("subtitle", d.subtitle);
        if (d.subtitle == null) {
            d.subtitle = "";
        }
        d.enabled = s.getBoolean("enabled", d.enabled);
        d.permission = s.getString("permission", d.permission);
        d.broadcastWin = s.getBoolean("broadcast-win", d.broadcastWin);
        d.world = s.getString("location.world", "world");
        d.x = s.getDouble("location.x");
        d.y = s.getDouble("location.y");
        d.z = s.getDouble("location.z");
        d.yaw = (float) s.getDouble("location.yaw");
        d.pixelSize = s.getDouble("appearance.pixel-size", d.pixelSize);
        d.imageSize = clamp(s.getInt("appearance.image-size", d.imageSize), 32, 256);
        d.frameColor = Reward.parseColor(s.getString("appearance.frame-color", "#D08A33"));
        d.hubColor = Reward.parseColor(s.getString("appearance.hub-color", "#E2A44A"));
        d.pointerColor = Reward.parseColor(s.getString("appearance.pointer-color", "#F2B544"));
        d.artStyle = normalizeStyle(s.getString("appearance.art-style", d.artStyle));
        d.weightedSlices = s.getBoolean("appearance.weighted-slices", d.weightedSlices);
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
        d.moneyCost = Math.max(0, s.getDouble("cost.money", d.moneyCost));
        d.xpCost = Math.max(0, s.getInt("cost.xp-levels", d.xpCost));
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
        d.spinParticle = s.getString("particles.spin", d.spinParticle);
        d.spinParticleCount = Math.max(0, s.getInt("particles.spin-count", d.spinParticleCount));
        try {
            d.winEffect = WinEffect.valueOf(s.getString("effects.win", d.winEffect.name()));
        } catch (IllegalArgumentException ignored) {
            // Geçersiz değer: varsayılan kalır.
        }
        for (Map<?, ?> map : s.getMapList("rewards")) {
            d.rewards.add(Reward.fromMap(map));
        }
        return d;
    }

    void save(ConfigurationSection s) {
        s.set("name", name);
        s.set("subtitle", subtitle);
        s.set("enabled", enabled);
        s.set("permission", permission);
        s.set("broadcast-win", broadcastWin);
        s.set("location.world", world);
        s.set("location.x", x);
        s.set("location.y", y);
        s.set("location.z", z);
        s.set("location.yaw", yaw);
        s.set("appearance.pixel-size", pixelSize);
        s.set("appearance.image-size", imageSize);
        s.set("appearance.frame-color", String.format("#%06X", frameColor & 0xFFFFFF));
        s.set("appearance.hub-color", String.format("#%06X", hubColor & 0xFFFFFF));
        s.set("appearance.pointer-color", String.format("#%06X", pointerColor & 0xFFFFFF));
        s.set("appearance.art-style", artStyle);
        s.set("appearance.weighted-slices", weightedSlices);
        s.set("reward-items.upright", itemsUpright);
        s.set("reward-items.scale", itemScale);
        s.set("animation.duration", duration);
        s.set("animation.revolutions", revolutions);
        s.set("animation.style", style.name());
        s.set("cost.ticket-item", ticketItem == null ? "" : ticketItem.name());
        s.set("cost.ticket-amount", ticketAmount);
        s.set("cost.money", moneyCost);
        s.set("cost.xp-levels", xpCost);
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
        s.set("particles.spin", spinParticle);
        s.set("particles.spin-count", spinParticleCount);
        s.set("effects.win", winEffect.name());
        List<Map<String, Object>> list = new ArrayList<>();
        rewards.forEach(r -> list.add(r.toMap()));
        s.set("rewards", list);
    }

    /** /wheel create ile açılan yeni çarkların başlangıç ödülleri (gerçek, mütevazı). */
    static WheelDefinition withStarterRewards(String id) {
        WheelDefinition d = new WheelDefinition(id);
        d.rewards.add(prize("<gray>16x Iron Ingot", 40, 0xC0C0C0, Material.IRON_INGOT, 16));
        d.rewards.add(prize("<gold>8x Gold Ingot", 25, 0xF2C230, Material.GOLD_INGOT, 8));
        d.rewards.add(prize("<aqua>2x Diamond", 10, 0x3C8FE0, Material.DIAMOND, 2));
        return d;
    }

    /**
     * İlk kurulumda hazır gelen örnek çarklar. Konumsuzdurlar (çark kurulmaz);
     * yönetici /wheel move ile yerleştirir. Sadece vanilla komutlar kullanılır.
     */
    static List<WheelDefinition> samples() {
        WheelDefinition daily = new WheelDefinition("daily");
        daily.name = "<gold><bold>Daily Wheel";
        daily.cooldownSeconds = 86400;
        daily.rewards.add(prize("<gray>16x Iron Ingot", 30, 0xC0C0C0, Material.IRON_INGOT, 16));
        daily.rewards.add(prize("<gold>8x Gold Ingot", 20, 0xF2C230, Material.GOLD_INGOT, 8));
        daily.rewards.add(prize("<aqua>5x Diamond", 10, 0x3C8FE0, Material.DIAMOND, 5));
        daily.rewards.add(xpPrize("<green>10 Levels", 15, 0x4FB548, 10));
        daily.rewards.add(prize("<yellow>Enchanted Golden Apple", 3, 0xF2E230, Material.ENCHANTED_GOLDEN_APPLE, 1));

        WheelDefinition premium = new WheelDefinition("premium");
        premium.name = "<light_purple><bold>Premium Wheel";
        premium.ticketItem = Material.NETHER_STAR;
        premium.ticketAmount = 1;
        premium.cooldownSeconds = 3600;
        premium.winEffect = WinEffect.FIREWORK;
        premium.frameColor = 0xB04FD0;
        premium.hubColor = 0xE08AE0;
        premium.rewards.add(prize("<light_purple>Elytra", 2, 0xE8568F, Material.ELYTRA, 1));
        premium.rewards.add(prize("<gray>2x Netherite Ingot", 8, 0x6B6B6B, Material.NETHERITE_INGOT, 2));
        premium.rewards.add(prize("<aqua>16x Diamond", 15, 0x3C8FE0, Material.DIAMOND, 16));
        premium.rewards.add(prize("<yellow>Totem of Undying", 5, 0xF2E230, Material.TOTEM_OF_UNDYING, 1));
        premium.rewards.add(prize("<gold>8x Golden Apple", 20, 0xF2C230, Material.GOLDEN_APPLE, 8));

        WheelDefinition vote = new WheelDefinition("vote");
        vote.name = "<aqua><bold>Vote Wheel";
        vote.cooldownSeconds = 43200;
        vote.rewards.add(prize("<aqua>5x Diamond", 15, 0x3C8FE0, Material.DIAMOND, 5));
        vote.rewards.add(prize("<green>8x Emerald", 20, 0x4FB548, Material.EMERALD, 8));
        vote.rewards.add(xpPrize("<green>15 Levels", 15, 0x4FB548, 15));
        vote.rewards.add(prize("<light_purple>8x Ender Pearl", 20, 0xB04FD0, Material.ENDER_PEARL, 8));
        vote.rewards.add(prize("<gray>Name Tag", 10, 0xC0C0C0, Material.NAME_TAG, 1));
        return List.of(daily, premium, vote);
    }

    /** İkonu doğrudan eşya olarak veren ödül. */
    private static Reward prize(String name, double weight, int color, Material icon, int amount) {
        Reward reward = new Reward(name, weight, color, icon, List.of());
        reward.amount = amount;
        reward.giveItem = true;
        return reward;
    }

    /** Seviye veren ödül (vanilla xp komutuyla). */
    private static Reward xpPrize(String name, double weight, int color, int levels) {
        Reward reward = new Reward(name, weight, color, Material.EXPERIENCE_BOTTLE,
                List.of("xp add %player% " + levels + " levels"));
        reward.amount = 1;
        return reward;
    }

    public String summary() {
        return me.guldal.sunshinewheel.Messages.fill("command.summary", "style", style.label(),
                "duration", String.valueOf(duration), "rewards", String.valueOf(rewards.size()));
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Bilinmeyen stil adları varsayılana ("deluxe") düşer. */
    public static String normalizeStyle(String value) {
        return "classic".equalsIgnoreCase(value == null ? "" : value.trim()) ? "classic" : "deluxe";
    }
}
