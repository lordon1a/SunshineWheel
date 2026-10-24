package me.guldal.sunshinewheel.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import me.guldal.sunshinewheel.integration.CustomItems;
import me.guldal.sunshinewheel.wheel.ItemSource;
import me.guldal.sunshinewheel.wheel.MotionStyle;
import me.guldal.sunshinewheel.wheel.Reward;
import me.guldal.sunshinewheel.wheel.WheelDefinition;
import me.guldal.sunshinewheel.wheel.WinEffect;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** JSON counterpart of a wheel definition for the web API; validation happens here. */
public final class WheelJson {

    private WheelJson() {
    }

    public static JsonObject toJson(WheelDefinition d) {
        JsonObject o = new JsonObject();
        o.addProperty("id", d.id);
        o.addProperty("name", d.name);
        o.addProperty("subtitle", d.subtitle);
        o.addProperty("enabled", d.enabled);
        o.addProperty("permission", d.permission);
        o.addProperty("broadcastWin", d.broadcastWin);
        o.addProperty("world", d.world);
        o.addProperty("x", d.x);
        o.addProperty("y", d.y);
        o.addProperty("z", d.z);
        o.addProperty("yaw", d.yaw);
        o.addProperty("pixelSize", d.pixelSize);
        o.addProperty("imageSize", d.imageSize);
        o.addProperty("frameColor", String.format("#%06X", d.frameColor & 0xFFFFFF));
        o.addProperty("hubColor", String.format("#%06X", d.hubColor & 0xFFFFFF));
        o.addProperty("pointerColor", String.format("#%06X", d.pointerColor & 0xFFFFFF));
        o.addProperty("artStyle", d.artStyle);
        o.addProperty("weightedSlices", d.weightedSlices);
        o.addProperty("itemsUpright", d.itemsUpright);
        o.addProperty("itemScale", d.itemScale);
        o.addProperty("duration", d.duration);
        o.addProperty("revolutions", d.revolutions);
        o.addProperty("style", d.style.name());
        o.addProperty("ticketItem", d.ticketItem == null ? "" : d.ticketItem.name());
        o.addProperty("ticketAmount", d.ticketAmount);
        o.addProperty("moneyCost", d.moneyCost);
        o.addProperty("xpCost", d.xpCost);
        JsonArray cost = new JsonArray();
        d.costCommands.forEach(cost::add);
        o.add("costCommands", cost);
        o.addProperty("cooldownSeconds", d.cooldownSeconds);
        o.addProperty("showHologram", d.showHologram);
        o.addProperty("hologramOffset", d.hologramOffset);
        o.addProperty("soundStart", d.soundStart);
        o.addProperty("soundTick", d.soundTick);
        o.addProperty("soundLanding", d.soundLanding);
        o.addProperty("soundWin", d.soundWin);
        o.addProperty("particleWin", d.particleWin);
        o.addProperty("particleCount", d.particleCount);
        o.addProperty("spinParticle", d.spinParticle);
        o.addProperty("spinParticleCount", d.spinParticleCount);
        o.addProperty("winEffect", d.winEffect.name());
        JsonArray rewards = new JsonArray();
        for (Reward r : d.rewards) {
            JsonObject reward = new JsonObject();
            reward.addProperty("name", r.name);
            reward.addProperty("weight", r.weight);
            reward.addProperty("color", String.format("#%06X", r.color & 0xFFFFFF));
            reward.addProperty("icon", r.icon == null ? "CHEST" : r.icon.name());
            JsonArray commands = new JsonArray();
            r.commands.forEach(commands::add);
            reward.add("commands", commands);
            reward.addProperty("permission", r.permission);
            reward.addProperty("winMessage", r.winMessage);
            reward.addProperty("winSound", r.winSound);
            reward.addProperty("amount", r.amount);
            reward.addProperty("giveItem", r.giveItem);
            reward.addProperty("glow", r.glow);
            reward.addProperty("enabled", r.enabled);
            JsonArray lore = new JsonArray();
            r.lore.forEach(lore::add);
            reward.add("lore", lore);
            reward.addProperty("customModelData", r.customModelData);
            reward.addProperty("itemSource", r.itemSource == null ? "VANILLA" : r.itemSource.name());
            reward.addProperty("itemBase64", r.itemBase64 == null ? "" : r.itemBase64);
            reward.addProperty("providerId", r.providerId == null ? "" : r.providerId);
            reward.addProperty("itemName", itemName(r));
            rewards.add(reward);
        }
        o.add("rewards", rewards);
        return o;
    }

    /**
     * Missing fields keep their current value; the live definition is untouched until
     * validation fully passes. An invalid field throws {@link IllegalArgumentException}
     * and nothing is applied.
     */
    public static void apply(WheelDefinition target, JsonObject o) {
        WheelDefinition copy = copy(target);
        applyFields(copy, o);
        copyInto(copy, target);
    }

    private static void applyFields(WheelDefinition d, JsonObject o) {
        if (o.has("name")) {
            d.name = text(o, "name", d.name);
        }
        if (o.has("subtitle")) {
            d.subtitle = text(o, "subtitle", d.subtitle);
        }
        if (o.has("enabled")) {
            d.enabled = bool(o, "enabled", d.enabled);
        }
        if (o.has("permission")) {
            d.permission = text(o, "permission", d.permission).trim();
        }
        if (o.has("broadcastWin")) {
            d.broadcastWin = bool(o, "broadcastWin", d.broadcastWin);
        }
        if (o.has("world")) {
            String world = text(o, "world", d.world);
            if (Bukkit.getWorld(world) == null) {
                throw new IllegalArgumentException("Unknown world: " + world);
            }
            d.world = world;
        }
        if (o.has("x")) {
            d.x = number(o, "x", d.x);
        }
        if (o.has("y")) {
            d.y = number(o, "y", d.y);
        }
        if (o.has("z")) {
            d.z = number(o, "z", d.z);
        }
        if (o.has("yaw")) {
            d.yaw = (float) number(o, "yaw", d.yaw);
        }
        if (o.has("pixelSize")) {
            d.pixelSize = clamp(number(o, "pixelSize", d.pixelSize), 0.005, 0.2);
        }
        if (o.has("imageSize")) {
            d.imageSize = (int) clamp((long) number(o, "imageSize", d.imageSize), 32, 256);
        }
        if (o.has("frameColor")) {
            d.frameColor = Reward.parseColor(text(o, "frameColor", "#D08A33"));
        }
        if (o.has("hubColor")) {
            d.hubColor = Reward.parseColor(text(o, "hubColor", "#E2A44A"));
        }
        if (o.has("pointerColor")) {
            d.pointerColor = Reward.parseColor(text(o, "pointerColor", "#F2B544"));
        }
        if (o.has("artStyle")) {
            d.artStyle = WheelDefinition.normalizeStyle(text(o, "artStyle", d.artStyle));
        }
        if (o.has("weightedSlices")) {
            d.weightedSlices = bool(o, "weightedSlices", d.weightedSlices);
        }
        if (o.has("itemsUpright")) {
            d.itemsUpright = bool(o, "itemsUpright", d.itemsUpright);
        }
        if (o.has("itemScale")) {
            d.itemScale = clamp(number(o, "itemScale", d.itemScale), 0.05, 2.0);
        }
        if (o.has("duration")) {
            d.duration = (int) clamp((long) number(o, "duration", d.duration), 20, 600);
        }
        if (o.has("revolutions")) {
            d.revolutions = clamp(number(o, "revolutions", d.revolutions), 1, 30);
        }
        if (o.has("style")) {
            try {
                d.style = MotionStyle.valueOf(text(o, "style", d.style.name()).toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid motion style.");
            }
        }
        if (o.has("ticketItem")) {
            String ticket = text(o, "ticketItem", "").trim();
            if (ticket.isEmpty()) {
                d.ticketItem = null;
            } else {
                Material material = Material.matchMaterial(ticket);
                if (material == null || !material.isItem()) {
                    throw new IllegalArgumentException("Invalid ticket item: " + ticket);
                }
                d.ticketItem = material;
            }
        }
        if (o.has("ticketAmount")) {
            d.ticketAmount = (int) clamp((long) number(o, "ticketAmount", d.ticketAmount), 1, 64);
        }
        if (o.has("moneyCost")) {
            d.moneyCost = clamp(number(o, "moneyCost", d.moneyCost), 0, 1000000000);
        }
        if (o.has("xpCost")) {
            d.xpCost = (int) clamp((long) number(o, "xpCost", d.xpCost), 0, 10000);
        }
        if (o.has("costCommands")) {
            d.costCommands.clear();
            d.costCommands.addAll(strings(o, "costCommands"));
        }
        if (o.has("cooldownSeconds")) {
            d.cooldownSeconds = (int) clamp((long) number(o, "cooldownSeconds", d.cooldownSeconds), 0, 86400);
        }
        if (o.has("showHologram")) {
            d.showHologram = bool(o, "showHologram", d.showHologram);
        }
        if (o.has("hologramOffset")) {
            d.hologramOffset = clamp(number(o, "hologramOffset", d.hologramOffset), -5, 5);
        }
        if (o.has("soundStart")) {
            d.soundStart = sound(o, "soundStart", d.soundStart);
        }
        if (o.has("soundTick")) {
            d.soundTick = sound(o, "soundTick", d.soundTick);
        }
        if (o.has("soundLanding")) {
            d.soundLanding = sound(o, "soundLanding", d.soundLanding);
        }
        if (o.has("soundWin")) {
            d.soundWin = sound(o, "soundWin", d.soundWin);
        }
        if (o.has("particleWin")) {
            d.particleWin = text(o, "particleWin", d.particleWin);
        }
        if (o.has("particleCount")) {
            d.particleCount = (int) clamp((long) number(o, "particleCount", d.particleCount), 0, 500);
        }
        if (o.has("spinParticle")) {
            d.spinParticle = text(o, "spinParticle", d.spinParticle);
        }
        if (o.has("spinParticleCount")) {
            d.spinParticleCount = (int) clamp((long) number(o, "spinParticleCount", d.spinParticleCount), 0, 500);
        }
        if (o.has("winEffect")) {
            try {
                d.winEffect = WinEffect.valueOf(text(o, "winEffect", d.winEffect.name()).toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid win effect.");
            }
        }
        if (o.has("rewards")) {
            JsonArray array = array(o, "rewards");
            if (array.size() > 64) {
                throw new IllegalArgumentException("At most 64 rewards allowed.");
            }
            List<Reward> parsed = new ArrayList<>();
            for (JsonElement element : array) {
                if (!element.isJsonObject()) {
                    throw new IllegalArgumentException("Reward list is invalid.");
                }
                parsed.add(reward(element.getAsJsonObject()));
            }
            d.rewards.clear();
            d.rewards.addAll(parsed);
        }
    }

    /** Ses biçimi: "anahtar ses perde"; anahtar geçerli bir ad alanı anahtarı olmalı. */
    private static String sound(JsonObject o, String key, String fallback) {
        String spec = text(o, key, fallback);
        if (spec == null || spec.isBlank() || spec.trim().equalsIgnoreCase("yok")) {
            return spec;
        }
        String name = spec.trim().split("\\s+")[0];
        try {
            net.kyori.adventure.key.Key.key(name);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid sound key in '" + key + "'.");
        }
        return spec;
    }

    private static Reward reward(JsonObject o) {
        String name = o.has("name") ? text(o, "name", "Reward") : "Reward";
        double weight = o.has("weight") ? clamp(number(o, "weight", 1), 0, 100000) : 1;
        int color = o.has("color") ? Reward.parseColor(text(o, "color", "#808080")) : 0x808080;
        Material icon = o.has("icon") ? Material.matchMaterial(text(o, "icon", "")) : null;
        if (icon == null || !icon.isItem()) {
            throw new IllegalArgumentException("Invalid reward icon: "
                    + (o.has("icon") ? text(o, "icon", "") : ""));
        }
        List<String> commands = o.has("commands") ? strings(o, "commands") : new ArrayList<>();
        if (commands.size() > 64) {
            throw new IllegalArgumentException("Too many commands (max 64).");
        }
        Reward reward = new Reward(name, weight, color, icon, commands);
        reward.permission = o.has("permission") ? text(o, "permission", "").trim() : "";
        reward.winMessage = o.has("winMessage") ? text(o, "winMessage", "") : "";
        reward.winSound = o.has("winSound") ? sound(o, "winSound", "") : "";
        reward.amount = o.has("amount") ? (int) clamp((long) number(o, "amount", 1), 1, 64) : 1;
        reward.giveItem = o.has("giveItem") && bool(o, "giveItem", false);
        reward.glow = o.has("glow") && bool(o, "glow", false);
        reward.enabled = !o.has("enabled") || bool(o, "enabled", true);
        if (o.has("lore")) {
            List<String> lore = strings(o, "lore");
            if (lore.size() > 32) {
                throw new IllegalArgumentException("Too many lore lines (max 32).");
            }
            reward.lore.addAll(lore);
        }
        reward.customModelData = o.has("customModelData")
                ? (int) clamp((long) number(o, "customModelData", 0), 0, 999999) : 0;
        reward.itemSource = o.has("itemSource")
                ? ItemSource.parse(text(o, "itemSource", "VANILLA"), null) : ItemSource.VANILLA;
        if (reward.itemSource == null) {
            throw new IllegalArgumentException("Invalid item source: " + text(o, "itemSource", ""));
        }
        String base64 = o.has("itemBase64") ? text(o, "itemBase64", "") : "";
        if (base64.length() > Reward.MAX_ITEM_BASE64) {
            throw new IllegalArgumentException("Captured item data is too large.");
        }
        if (!base64.isBlank()) {
            try {
                Base64.getDecoder().decode(base64);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Captured item data is not valid.");
            }
        }
        reward.itemBase64 = base64;
        String providerId = o.has("providerId") ? text(o, "providerId", "").trim() : "";
        if (providerId.length() > Reward.MAX_PROVIDER_ID) {
            throw new IllegalArgumentException("Provider item id is too long.");
        }
        reward.providerId = providerId;
        return reward;
    }

    /** Web editöründe göstermek için çözümlenmiş custom eşya adı (yoksa boş). */
    private static String itemName(Reward reward) {
        if (reward.itemSource == null || reward.itemSource == ItemSource.VANILLA) {
            return "";
        }
        ItemStack item = CustomItems.sourceItem(reward.itemSource, reward.providerId, reward.itemBase64, 1);
        return item == null ? "" : CustomItems.displayName(item);
    }

    private static WheelDefinition copy(WheelDefinition d) {
        WheelDefinition c = new WheelDefinition(d.id);
        c.name = d.name;
        c.subtitle = d.subtitle;
        c.enabled = d.enabled;
        c.permission = d.permission;
        c.broadcastWin = d.broadcastWin;
        c.world = d.world;
        c.x = d.x;
        c.y = d.y;
        c.z = d.z;
        c.yaw = d.yaw;
        c.pixelSize = d.pixelSize;
        c.imageSize = d.imageSize;
        c.frameColor = d.frameColor;
        c.hubColor = d.hubColor;
        c.pointerColor = d.pointerColor;
        c.artStyle = d.artStyle;
        c.weightedSlices = d.weightedSlices;
        c.itemsUpright = d.itemsUpright;
        c.itemScale = d.itemScale;
        c.duration = d.duration;
        c.revolutions = d.revolutions;
        c.style = d.style;
        c.ticketItem = d.ticketItem;
        c.ticketAmount = d.ticketAmount;
        c.moneyCost = d.moneyCost;
        c.xpCost = d.xpCost;
        c.costCommands.addAll(d.costCommands);
        c.cooldownSeconds = d.cooldownSeconds;
        c.showHologram = d.showHologram;
        c.hologramOffset = d.hologramOffset;
        c.soundStart = d.soundStart;
        c.soundTick = d.soundTick;
        c.soundLanding = d.soundLanding;
        c.soundWin = d.soundWin;
        c.particleWin = d.particleWin;
        c.particleCount = d.particleCount;
        c.spinParticle = d.spinParticle;
        c.spinParticleCount = d.spinParticleCount;
        c.winEffect = d.winEffect;
        for (Reward r : d.rewards) {
            Reward copy = new Reward(r.name, r.weight, r.color, r.icon, r.commands);
            copy.permission = r.permission;
            copy.winMessage = r.winMessage;
            copy.winSound = r.winSound;
            copy.amount = r.amount;
            copy.giveItem = r.giveItem;
            copy.glow = r.glow;
            copy.enabled = r.enabled;
            copy.lore.addAll(r.lore);
            copy.customModelData = r.customModelData;
            copy.itemSource = r.itemSource;
            copy.itemBase64 = r.itemBase64;
            copy.providerId = r.providerId;
            c.rewards.add(copy);
        }
        return c;
    }

    private static void copyInto(WheelDefinition src, WheelDefinition dst) {
        dst.name = src.name;
        dst.subtitle = src.subtitle;
        dst.enabled = src.enabled;
        dst.permission = src.permission;
        dst.broadcastWin = src.broadcastWin;
        dst.world = src.world;
        dst.x = src.x;
        dst.y = src.y;
        dst.z = src.z;
        dst.yaw = src.yaw;
        dst.pixelSize = src.pixelSize;
        dst.imageSize = src.imageSize;
        dst.frameColor = src.frameColor;
        dst.hubColor = src.hubColor;
        dst.pointerColor = src.pointerColor;
        dst.artStyle = src.artStyle;
        dst.weightedSlices = src.weightedSlices;
        dst.itemsUpright = src.itemsUpright;
        dst.itemScale = src.itemScale;
        dst.duration = src.duration;
        dst.revolutions = src.revolutions;
        dst.style = src.style;
        dst.ticketItem = src.ticketItem;
        dst.ticketAmount = src.ticketAmount;
        dst.moneyCost = src.moneyCost;
        dst.xpCost = src.xpCost;
        dst.costCommands.clear();
        dst.costCommands.addAll(src.costCommands);
        dst.cooldownSeconds = src.cooldownSeconds;
        dst.showHologram = src.showHologram;
        dst.hologramOffset = src.hologramOffset;
        dst.soundStart = src.soundStart;
        dst.soundTick = src.soundTick;
        dst.soundLanding = src.soundLanding;
        dst.soundWin = src.soundWin;
        dst.particleWin = src.particleWin;
        dst.particleCount = src.particleCount;
        dst.spinParticle = src.spinParticle;
        dst.spinParticleCount = src.spinParticleCount;
        dst.winEffect = src.winEffect;
        dst.rewards.clear();
        dst.rewards.addAll(src.rewards);
    }

    // --- typed readers: wrong types produce readable errors ---

    private static String text(JsonObject o, String key, String fallback) {
        JsonElement element = o.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("'" + key + "' must be text.");
        }
        return element.getAsString();
    }

    private static double number(JsonObject o, String key, double fallback) {
        JsonElement element = o.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        try {
            return element.getAsDouble();
        } catch (NumberFormatException | UnsupportedOperationException e) {
            throw new IllegalArgumentException("'" + key + "' must be a number.");
        }
    }

    private static boolean bool(JsonObject o, String key, boolean fallback) {
        JsonElement element = o.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException("'" + key + "' must be true or false.");
        }
        return element.getAsBoolean();
    }

    private static JsonArray array(JsonObject o, String key) {
        JsonElement element = o.get(key);
        if (element == null || !element.isJsonArray()) {
            throw new IllegalArgumentException("'" + key + "' must be a list.");
        }
        return element.getAsJsonArray();
    }

    private static List<String> strings(JsonObject o, String key) {
        List<String> result = new ArrayList<>();
        for (JsonElement element : array(o, key)) {
            if (!element.isJsonPrimitive()) {
                throw new IllegalArgumentException("'" + key + "' contains a non-text value.");
            }
            result.add(element.getAsString());
        }
        return result;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }
}
