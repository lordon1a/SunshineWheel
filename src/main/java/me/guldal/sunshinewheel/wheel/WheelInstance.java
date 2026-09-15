package me.guldal.sunshinewheel.wheel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import me.guldal.sunshinewheel.render.PixelImage;
import me.guldal.sunshinewheel.render.PixelText;
import me.guldal.sunshinewheel.render.WheelArt;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Dünyada duran bir çarkın varlıkları ve dönüş animasyonu.
 *
 * <p>Tüm parçalar aynı noktada doğar; konumlar Display transformation'ı ile yerel düzlemde verilir
 * (+x sağ, +y yukarı, +z izleyiciye doğru). Çark görüntüsü boş satırlarla kökene ortalanır, böylece
 * dönüş sırasında translation neredeyse sabit kalır ve istemci sadece rotasyonu yumuşatır.
 */
public final class WheelInstance {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int POINTER_W = 13;
    private static final int POINTER_H = 15;

    private final Plugin plugin;
    private final NamespacedKey partKey;
    private final RenderSettings settings;
    public final WheelDefinition def;

    private TextDisplay wheel;
    private TextDisplay pointer;
    private TextDisplay title;
    private Interaction hitbox;
    private final List<ItemDisplay> items = new ArrayList<>();
    private final Map<UUID, Long> lastSpin = new HashMap<>();

    private double angle;
    private boolean spinning;

    WheelInstance(Plugin plugin, NamespacedKey partKey, RenderSettings settings, WheelDefinition def) {
        this.plugin = plugin;
        this.partKey = partKey;
        this.settings = settings;
        this.def = def;
    }

    public boolean isSpawned() {
        return wheel != null && wheel.isValid();
    }

    public boolean isSpinning() {
        return spinning;
    }

    Interaction hitbox() {
        return hitbox;
    }

    Location base() {
        World world = Bukkit.getWorld(def.world);
        return world == null ? null : new Location(world, def.x, def.y, def.z, def.yaw, 0);
    }

    private double scale() {
        return def.pixelSize / settings.textPixelBlocks;
    }

    private double radius() {
        return def.imageSize / 2.0 * def.pixelSize;
    }

    private double wheelCenterY() {
        return settings.imageCenterY(def.imageSize, def.pixelSize);
    }

    public void spawn() {
        Location base = base();
        if (base == null || !base.getWorld().isChunkLoaded(base.getBlockX() >> 4, base.getBlockZ() >> 4)) {
            return;
        }
        despawn();
        World world = base.getWorld();
        float scale = (float) scale();

        List<Integer> colors = def.rewards.stream().map(r -> r.color).toList();
        PixelImage wheelImage = WheelArt.wheel(colors, def.imageSize);
        Component wheelText = PixelText.encode(wheelImage, settings.padLines(wheelImage.height));
        wheel = world.spawn(base, TextDisplay.class, td -> setupPixelDisplay(td, wheelText, scale));

        Component pointerText = PixelText.encode(WheelArt.pointer(POINTER_W, POINTER_H), settings.padLines(POINTER_H));
        pointer = world.spawn(base, TextDisplay.class, td -> setupPixelDisplay(td, pointerText, scale));

        for (Reward reward : def.rewards) {
            items.add(world.spawn(base, ItemDisplay.class, id -> {
                tag(id);
                id.setItemStack(new ItemStack(reward.icon));
                id.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GUI);
                id.setBrightness(new Display.Brightness(15, 15));
            }));
        }

        double center = wheelCenterY();
        if (def.showHologram) {
            Location titleLoc = base.clone().add(0, center + radius() + POINTER_H * def.pixelSize + def.hologramOffset, 0);
            title = world.spawn(titleLoc, TextDisplay.class, td -> {
                tag(td);
                td.text(MM.deserialize(def.name + (def.subtitle.isBlank() ? "" : "<newline>" + def.subtitle)));
                td.setBillboard(Display.Billboard.CENTER);
                td.setAlignment(TextDisplay.TextAlignment.CENTER);
                td.setShadowed(true);
            });
        }

        double diameter = radius() * 2;
        hitbox = world.spawn(base.clone().add(0, center - radius(), 0), Interaction.class, it -> {
            tag(it);
            it.setInteractionWidth((float) diameter);
            it.setInteractionHeight((float) diameter);
            it.setResponsive(true);
        });

        placePointer();
        applyAngle(angle, 0);
    }

    private void setupPixelDisplay(TextDisplay td, Component text, float scale) {
        tag(td);
        td.text(text);
        td.setLineWidth(Integer.MAX_VALUE / 2);
        td.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        td.setShadowed(false);
        td.setSeeThrough(false);
        td.setBillboard(Display.Billboard.FIXED);
        td.setAlignment(TextDisplay.TextAlignment.CENTER);
        td.setBrightness(new Display.Brightness(15, 15));
        td.setViewRange(1.5f);
        td.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(scale), new Quaternionf()));
    }

    private void tag(Entity entity) {
        entity.setPersistent(false);
        entity.getPersistentDataContainer().set(partKey, PersistentDataType.STRING, def.id);
    }

    public void despawn() {
        for (Entity e : new Entity[] {wheel, pointer, title, hitbox}) {
            if (e != null) {
                e.remove();
            }
        }
        items.forEach(Entity::remove);
        items.clear();
        wheel = pointer = title = null;
        hitbox = null;
    }

    /** Ok çarkla dönmez; konumu doğarken bir kez verilir. */
    private void placePointer() {
        double pointerOwnCenter = settings.imageCenterY(POINTER_H, def.pixelSize);
        double pointerTargetY = wheelCenterY() + radius() + POINTER_H * def.pixelSize * 0.2;
        transform(pointer, new Vector3f(0, (float) (pointerTargetY - pointerOwnCenter), 0.04f),
                new Quaternionf(), (float) scale(), 0);
    }

    private void applyAngle(double degrees, int interpolationTicks) {
        if (!isSpawned()) {
            return;
        }
        Vector3f c = new Vector3f(0, (float) wheelCenterY(), 0);
        Quaternionf spin = new Quaternionf().rotateZ((float) Math.toRadians(-degrees));

        // Görüntü kökene ortalı olduğundan c küçüktür; bu translation dönüş boyunca neredeyse sabit kalır.
        Vector3f wheelTranslation = new Vector3f(c).sub(new Vector3f(c).rotate(spin));
        transform(wheel, wheelTranslation, spin, (float) scale(), interpolationTicks);

        int count = items.size();
        double segment = 360.0 / Math.max(1, count);
        double itemRing = radius() * 0.58;
        for (int i = 0; i < count; i++) {
            double a = Math.toRadians((i + 0.5) * segment);
            Vector3f offset = new Vector3f((float) (Math.sin(a) * itemRing), (float) (Math.cos(a) * itemRing), 0);
            Vector3f position = new Vector3f(c).add(offset.rotate(spin));
            position.z = 0.03f;
            Quaternionf itemRotation = def.itemsUpright ? new Quaternionf() : new Quaternionf(spin);
            transform(items.get(i), position, itemRotation, (float) def.itemScale, interpolationTicks);
        }
    }

    private static void transform(Display display, Vector3f translation, Quaternionf rotation, float scale, int ticks) {
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(ticks);
        display.setTransformation(new Transformation(translation, rotation, new Vector3f(scale), new Quaternionf()));
    }

    /** Ok (üstte) altındaki dilim; θ saat yönünde dönüş olduğundan görüntü açısı -θ. */
    private int segmentAt(double degrees) {
        int count = Math.max(1, def.rewards.size());
        double imageAngle = ((-degrees % 360) + 360) % 360;
        return Math.min(count - 1, (int) (imageAngle / (360.0 / count)));
    }

    /** Bekleme süresi dolmadıysa kalan saniye, aksi halde 0. */
    public long cooldownLeft(Player player) {
        if (def.cooldownSeconds <= 0) {
            return 0;
        }
        Long last = lastSpin.get(player.getUniqueId());
        if (last == null) {
            return 0;
        }
        long passed = (System.currentTimeMillis() - last) / 1000;
        return Math.max(0, def.cooldownSeconds - passed);
    }

    private boolean takeCost(Player player) {
        if (def.ticketItem != null) {
            ItemStack ticket = new ItemStack(def.ticketItem, Math.max(1, def.ticketAmount));
            if (!player.getInventory().containsAtLeast(ticket, ticket.getAmount())) {
                player.sendMessage(MM.deserialize("<red>Bunun için " + ticket.getAmount() + "x "
                        + def.ticketItem.name() + " gerekiyor."));
                return false;
            }
            player.getInventory().removeItem(ticket);
        }
        for (String command : def.costCommands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
        }
        return true;
    }

    /** @return başlatıldıysa true */
    public boolean spin(Player player) {
        if (spinning || def.rewards.isEmpty() || !isSpawned()) {
            return false;
        }
        long left = cooldownLeft(player);
        if (left > 0) {
            player.sendActionBar(MM.deserialize("<red>Bu çarkı tekrar çevirmek için " + left + " saniye bekle."));
            return true;
        }
        if (!takeCost(player)) {
            return true;
        }
        lastSpin.put(player.getUniqueId(), System.currentTimeMillis());

        int index = pickIndex();
        Reward reward = def.rewards.get(index);
        int count = def.rewards.size();
        double segment = 360.0 / count;
        double jitter = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.6 * segment;
        double target = ((360.0 - ((index + 0.5) * segment + jitter)) % 360 + 360) % 360;

        double start = ((angle % 360) + 360) % 360;
        double delta = def.revolutions * 360.0 + (((target - start) % 360) + 360) % 360;
        int duration = def.duration;
        int smoothing = Math.max(1, settings.interpolationTicks);
        spinning = true;
        Effects.playSound(centerLocation(), def.soundStart);

        int[] tick = {0};
        int[] lastSegment = {segmentAt(start)};
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!isSpawned()) {
                spinning = false;
                task.cancel();
                return;
            }
            tick[0]++;
            double t = Math.min(1.0, (double) tick[0] / duration);
            angle = start + delta * def.style.progress(t);
            applyAngle(angle, smoothing);

            int now = segmentAt(angle);
            if (now != lastSegment[0]) {
                lastSegment[0] = now;
                Effects.playSound(centerLocation(), def.soundTick);
            }

            if (tick[0] >= duration) {
                task.cancel();
                angle = ((angle % 360) + 360) % 360;
                // İstemci son güncellemeyi 'smoothing' tick'te tamamlar; ödülü çark gözle durunca ver.
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    spinning = false;
                    Effects.playSound(centerLocation(), def.soundLanding);
                    award(player, reward);
                }, smoothing);
            }
        }, 1L, 1L);
        return true;
    }

    private int pickIndex() {
        double total = def.rewards.stream().mapToDouble(r -> Math.max(0, r.weight)).sum();
        if (total <= 0) {
            return ThreadLocalRandom.current().nextInt(def.rewards.size());
        }
        double roll = ThreadLocalRandom.current().nextDouble(total);
        for (int i = 0; i < def.rewards.size(); i++) {
            roll -= Math.max(0, def.rewards.get(i).weight);
            if (roll < 0) {
                return i;
            }
        }
        return def.rewards.size() - 1;
    }

    private void award(Player player, Reward reward) {
        Location c = centerLocation();
        Effects.spawnParticle(c, def.particleWin, def.particleCount, 0.8);
        Effects.playSound(c, def.soundWin);
        if (!player.isOnline()) {
            return;
        }
        player.sendMessage(MM.deserialize("<gold>☀</gold> <gray>Çarktan kazandın:</gray> <reward>",
                Placeholder.component("reward", MM.deserialize(reward.name))));
        for (String command : reward.commands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
        }
    }

    Location centerLocation() {
        Location base = base();
        return base == null ? null : base.add(0, wheelCenterY(), 0);
    }

    /** Görsel eşleşme testi için: çarkı belirli açıya getirir ve oktaki dilimi döndürür. */
    public int debugSetAngle(double degrees) {
        angle = degrees;
        applyAngle(angle, 0);
        return segmentAt(angle);
    }
}
