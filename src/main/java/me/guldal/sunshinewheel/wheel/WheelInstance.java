package me.guldal.sunshinewheel.wheel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import me.guldal.sunshinewheel.Messages;
import me.guldal.sunshinewheel.api.WheelSpinStartEvent;
import me.guldal.sunshinewheel.api.WheelSpinWinEvent;
import me.guldal.sunshinewheel.render.PixelImage;
import me.guldal.sunshinewheel.render.PixelText;
import me.guldal.sunshinewheel.render.WheelArt;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
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
    private final WheelManager wheels;
    private final NamespacedKey partKey;
    private final RenderSettings settings;
    public final WheelDefinition def;

    private TextDisplay wheel;
    private TextDisplay pointer;
    private TextDisplay title;
    private Interaction hitbox;
    private final List<ItemDisplay> items = new ArrayList<>();

    /** Devam eden çevirmenin ücreti; yarıda kesilirse iade edilir. */
    private UUID spinner;
    private Charge pendingCharge;

    private double angle;
    private boolean spinning;

    WheelInstance(WheelManager wheels, Plugin plugin, NamespacedKey partKey, RenderSettings settings,
            WheelDefinition def) {
        this.wheels = wheels;
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

    public Interaction hitbox() {
        return hitbox;
    }

    Location base() {
        if (def.world == null) {
            return null;
        }
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

        List<Integer> colors = visibleRewards().stream().map(r -> r.color).toList();
        PixelImage wheelImage = WheelArt.wheel(colors, def.imageSize, def.frameColor, def.hubColor);
        Component wheelText = PixelText.encode(wheelImage, settings.padLines(wheelImage.height));
        wheel = world.spawn(base, TextDisplay.class, td -> setupPixelDisplay(td, wheelText, scale));

        Component pointerText = PixelText.encode(
                WheelArt.pointer(POINTER_W, POINTER_H, def.pointerColor), settings.padLines(POINTER_H));
        pointer = world.spawn(base, TextDisplay.class, td -> setupPixelDisplay(td, pointerText, scale));

        for (Reward reward : visibleRewards()) {
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
        refundAbort();
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
        List<Reward> visible = visibleRewards();
        int count = Math.max(1, visible.size());
        double imageAngle = ((-degrees % 360) + 360) % 360;
        return Math.min(count - 1, (int) (imageAngle / (360.0 / count)));
    }

    /** Çarkta görünen (etkin) ödüller; çizim ve seçim bu listeyi kullanır. */
    public List<Reward> visibleRewards() {
        return def.rewards.stream().filter(reward -> reward.enabled).toList();
    }

    /** Oyuncunun kazanabileceği ödüller (etkin + izinli). */
    private List<Reward> eligibleRewards(Player player) {
        return visibleRewards().stream()
                .filter(reward -> reward.permission.isBlank() || player.hasPermission(reward.permission))
                .toList();
    }

    /** Bekleme süresi dolmadıysa kalan saniye, aksi halde 0. */
    public long cooldownLeft(Player player) {
        if (def.cooldownSeconds <= 0) {
            return 0;
        }
        long last = wheels.lastSpinMillis(def.id, player.getUniqueId());
        if (last <= 0) {
            return 0;
        }
        long passed = (System.currentTimeMillis() - last) / 1000;
        return Math.max(0, def.cooldownSeconds - passed);
    }

    public void clearCooldown(UUID playerId) {
        wheels.resetCooldown(def.id, playerId);
    }

    private Economy economy() {
        var registration = Bukkit.getServicesManager().getRegistration(Economy.class);
        return registration == null ? null : registration.getProvider();
    }

    /** Alınan ücreti iade edilebilir şekilde tutar. */
    private record Charge(double money, int xpLevels, ItemStack ticket) {
    }

    /** Tüm maliyetler önce denetlenir, sonra birlikte alınır; yarım ödeme olmaz. */
    private Charge takeCost(Player player) {
        Economy economy = def.moneyCost > 0 ? economy() : null;
        if (def.moneyCost > 0) {
            if (economy == null) {
                player.sendMessage(Messages.mm("economy.missing"));
                return null;
            }
            if (!economy.has(player, def.moneyCost)) {
                player.sendMessage(Messages.mm("economy.need-money", "amount", economy.format(def.moneyCost)));
                return null;
            }
        }
        if (def.xpCost > 0 && player.getLevel() < def.xpCost) {
            player.sendMessage(Messages.mm("economy.need-xp", "amount", String.valueOf(def.xpCost)));
            return null;
        }
        ItemStack ticket = null;
        if (def.ticketItem != null) {
            ticket = new ItemStack(def.ticketItem, Math.max(1, def.ticketAmount));
            if (!player.getInventory().containsAtLeast(ticket, ticket.getAmount())) {
                player.sendMessage(Messages.mm("economy.need-ticket",
                        "amount", String.valueOf(ticket.getAmount()), "item", def.ticketItem.name()));
                return null;
            }
        }
        if (economy != null
                && !economy.withdrawPlayer(player, def.moneyCost).transactionSuccess()) {
            player.sendMessage(Messages.mm("economy.payment-failed"));
            return null;
        }
        if (ticket != null) {
            player.getInventory().removeItem(ticket);
        }
        if (def.xpCost > 0) {
            player.setLevel(player.getLevel() - def.xpCost);
        }
        for (String command : def.costCommands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command
                    .replace("%player%", player.getName()).replace("%wheel%", def.id));
        }
        return new Charge(def.moneyCost, def.xpCost, ticket == null ? null : ticket.clone());
    }

    /** Yarıda kesilen çevirmenin ücretini geri verir. */
    private void refundAbort() {
        if (pendingCharge == null || spinner == null) {
            pendingCharge = null;
            spinner = null;
            return;
        }
        Charge charge = pendingCharge;
        pendingCharge = null;
        UUID spinnerId = spinner;
        spinner = null;
        OfflinePlayer offline = Bukkit.getOfflinePlayer(spinnerId);
        if (charge.money() > 0) {
            Economy economy = economy();
            if (economy != null) {
                economy.depositPlayer(offline, charge.money());
            }
        }
        Player online = offline.getPlayer();
        if (online != null) {
            if (charge.xpLevels() > 0) {
                online.setLevel(online.getLevel() + charge.xpLevels());
            }
            if (charge.ticket() != null) {
                Map<Integer, ItemStack> leftover = online.getInventory().addItem(charge.ticket());
                leftover.values().forEach(item ->
                        online.getWorld().dropItemNaturally(online.getLocation(), item));
            }
            online.sendMessage(Messages.mm("economy.refunded"));
        }
    }

    /** @return başlatıldıysa (ya da engellendiyse) true; çark meşgulse false */
    public boolean spin(Player player) {
        if (spinning || !isSpawned()) {
            return false;
        }
        if (!def.enabled) {
            player.sendActionBar(Messages.mm("spin.disabled"));
            return true;
        }
        if (!def.permission.isBlank() && !player.hasPermission(def.permission)) {
            player.sendActionBar(Messages.mm("spin.no-permission"));
            return true;
        }
        List<Reward> eligible = eligibleRewards(player);
        if (eligible.isEmpty()) {
            player.sendActionBar(Messages.mm("spin.no-rewards"));
            return true;
        }
        long left = cooldownLeft(player);
        if (left > 0) {
            player.sendActionBar(Messages.mm("spin.cooldown", "seconds", String.valueOf(left)));
            return true;
        }
        WheelSpinStartEvent startEvent = new WheelSpinStartEvent(player, this);
        Bukkit.getPluginManager().callEvent(startEvent);
        if (startEvent.isCancelled()) {
            return true;
        }
        boolean free = wheels.useFreeSpin(def.id, player.getUniqueId());
        Charge charge = null;
        if (free) {
            player.sendActionBar(Messages.mm("spin.free-used"));
        } else {
            charge = takeCost(player);
            if (charge == null) {
                return true;
            }
        }
        wheels.recordSpin(def.id, player.getUniqueId());
        spinner = player.getUniqueId();
        pendingCharge = charge;

        List<Reward> visible = visibleRewards();
        Reward reward = pick(eligible);
        int index = Math.max(0, visible.indexOf(reward));
        int count = Math.max(1, visible.size());
        double chance = chanceOf(reward);
        double segment = 360.0 / count;
        double jitter = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.6 * segment;
        double target = ((360.0 - ((index + 0.5) * segment + jitter)) % 360 + 360) % 360;

        double start = ((angle % 360) + 360) % 360;
        double delta = def.revolutions * 360.0 + (((target - start) % 360) + 360) % 360;
        int duration = def.duration;
        int smoothing = Math.max(1, settings.interpolationTicks);
        try {
            Effects.playSound(centerLocation(), def.soundStart);
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.WARNING, "Could not play wheel start sound (" + def.id + ")", e);
        }
        spinning = true;

        int[] tick = {0};
        int[] lastSegment = {segmentAt(start)};
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            try {
                if (!isSpawned()) {
                    spinning = false;
                    refundAbort();
                    task.cancel();
                    return;
                }
                tick[0]++;
                double t = Math.min(1.0, (double) tick[0] / duration);
                angle = start + delta * def.style.progress(t);
                applyAngle(angle, smoothing);

                if (!def.spinParticle.isBlank() && def.spinParticleCount > 0 && tick[0] % 4 == 0) {
                    Effects.spawnParticle(centerLocation(), def.spinParticle, def.spinParticleCount, radius() * 0.7);
                }

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
                        award(player, reward, chance);
                    }, smoothing);
                }
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.SEVERE, "Wheel animation error (" + def.id + ")", e);
                spinning = false;
                refundAbort();
                task.cancel();
            }
        }, 1L, 1L);
        return true;
    }

    private Reward pick(List<Reward> eligible) {
        double total = eligible.stream().mapToDouble(r -> Math.max(0, r.weight)).sum();
        if (total <= 0) {
            return eligible.get(ThreadLocalRandom.current().nextInt(eligible.size()));
        }
        double roll = ThreadLocalRandom.current().nextDouble(total);
        for (Reward reward : eligible) {
            roll -= Math.max(0, reward.weight);
            if (roll < 0) {
                return reward;
            }
        }
        return eligible.get(eligible.size() - 1);
    }

    /** Görünen ödüller arasındaki yüzde. */
    public double chanceOf(Reward reward) {
        List<Reward> visible = visibleRewards();
        double total = visible.stream().mapToDouble(r -> Math.max(0, r.weight)).sum();
        if (total <= 0) {
            return visible.isEmpty() ? 0 : 100.0 / visible.size();
        }
        return Math.max(0, reward.weight) / total * 100.0;
    }

    private void award(Player player, Reward reward, double chance) {
        if (!player.isOnline()) {
            refundAbort();
            return;
        }
        pendingCharge = null;
        spinner = null;
        Location c = centerLocation();
        Effects.spawnParticle(c, def.particleWin, def.particleCount, 0.8);
        Effects.playWinEffect(plugin, c, def.winEffect, def.particleWin);
        Effects.playSound(c, reward.winSound.isBlank() ? def.soundWin : reward.winSound);
        WheelSpinWinEvent win = new WheelSpinWinEvent(player, this, reward);
        Bukkit.getPluginManager().callEvent(win);
        if (win.isCancelled()) {
            return;
        }
        wheels.recordWin(def.id, player.getUniqueId());
        String template = reward.winMessage.isBlank()
                ? Messages.fill("award.default")
                : reward.winMessage;
        String resolved = resolveText(template, player, reward, chance);
        Component rewardName = MM.deserialize(resolveText(reward.name, player, reward, chance));
        player.sendMessage(resolved.contains("<reward>")
                ? MM.deserialize(resolved, Placeholder.component("reward", rewardName))
                : MM.deserialize(resolved));
        if (reward.giveItem) {
            ItemStack gift = buildIcon(player, reward, chance);
            Map<Integer, ItemStack> leftover =
                    player.getInventory().addItem(gift);
            leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
        for (String command : reward.commands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolveText(command, player, reward, chance));
        }
        if (def.broadcastWin) {
            Bukkit.broadcast(MM.deserialize(
                    Messages.fill("award.broadcast", "player", player.getName(), "wheel", stripTags(def.name)),
                    Placeholder.component("reward", rewardName)));
        }
    }

    private String resolveText(String text, Player player, Reward reward, double chance) {
        return text.replace("%player%", player.getName())
                .replace("%reward%", stripTags(reward.name))
                .replace("%wheel%", def.id)
                .replace("<chance>", String.format(Locale.ROOT, "%.2f%%", chance));
    }

    private static String stripTags(String text) {
        return text.replaceAll("<[^>]+>", "");
    }

    private ItemStack buildIcon(Player player, Reward reward, double chance) {
        ItemStack item = new ItemStack(reward.icon, Math.max(1, reward.amount));
        item.editMeta(meta -> {
            meta.displayName(MM.deserialize(resolveText(reward.name, player, reward, chance)));
            if (!reward.lore.isEmpty()) {
                List<Component> lore = new ArrayList<>();
                reward.lore.forEach(line -> lore.add(MM.deserialize(resolveText(line, player, reward, chance))));
                meta.lore(lore);
            }
            if (reward.glow) {
                meta.setEnchantmentGlintOverride(true);
            }
            if (reward.customModelData > 0) {
                meta.setCustomModelData(reward.customModelData);
            }
        });
        return item;
    }

    Location centerLocation() {
        Location base = base();
        return base == null ? null : base.add(0, wheelCenterY(), 0);
    }

    /** Oyuncunun kazanabileceklerini sohbette listeler. */
    public void sendPreview(Player player) {
        List<Reward> visible = visibleRewards();
        player.sendMessage(Messages.mm("preview.header", "wheel", stripTags(def.name)));
        if (visible.isEmpty()) {
            player.sendMessage(Messages.mm("preview.empty"));
            return;
        }
        for (Reward reward : visible) {
            double chance = chanceOf(reward);
            boolean locked = !reward.permission.isBlank() && !player.hasPermission(reward.permission);
            String suffix = locked ? " <red>🔒</red>" : "";
            player.sendMessage(MM.deserialize(Messages.fill("preview.row", "suffix", suffix),
                    Placeholder.component("reward",
                            MM.deserialize(resolveText(reward.name, player, reward, chance))),
                    Placeholder.component("chance",
                            Component.text(String.format(Locale.ROOT, "%.2f%%", chance)))));
        }
    }

    /** Görsel eşleşme testi için: çarkı belirli açıya getirir ve oktaki dilimi döndürür. */
    public int debugSetAngle(double degrees) {
        if (visibleRewards().isEmpty()) {
            return -1;
        }
        angle = degrees;
        applyAngle(angle, 0);
        return segmentAt(angle);
    }
}
