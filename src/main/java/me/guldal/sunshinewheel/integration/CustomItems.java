package me.guldal.sunshinewheel.integration;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import me.guldal.sunshinewheel.wheel.ItemSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

/**
 * Vanilla dışı ödül kaynaklarını çözer: birebir kopyalanmış eşyalar (Base64 NBT) ve
 * eşya eklentisi kimlikleri (Oraxen, ItemsAdder, MMOItems, Nexo, ExecutableItems).
 *
 * <p>Eklentilere doğrudan bağımlılık yok; hepsi yansıma (reflection) ile ve yalnızca
 * eklenti kuruluyken çağrılır. Çözülemeyen kimlikler sessizce null döner (bir kez uyarı basılır).
 */
public final class CustomItems {

    /** Web editörünün listelediği sağlayıcı eşyası. */
    public record Entry(String id, String name) {
    }

    private static final Map<String, Boolean> WARNED = new ConcurrentHashMap<>();
    private static final Map<ItemSource, List<Entry>> CACHE = new ConcurrentHashMap<>();
    private static Plugin plugin;

    private CustomItems() {
    }

    public static void init(Plugin owner) {
        plugin = owner;
    }

    /** Eklenti kurulması/yeniden yüklenmesi sonrası listeleri tazeler. */
    public static void clearCache() {
        CACHE.clear();
    }

    public static boolean available(ItemSource source) {
        if (source == null) {
            return false;
        }
        return switch (source) {
            case VANILLA, EXACT -> true;
            case ORAXEN -> Bukkit.getPluginManager().getPlugin("Oraxen") != null;
            case ITEMSADDER -> Bukkit.getPluginManager().getPlugin("ItemsAdder") != null;
            case MMOITEMS -> Bukkit.getPluginManager().getPlugin("MMOItems") != null;
            case NEXO -> Bukkit.getPluginManager().getPlugin("Nexo") != null;
            case EXECUTABLEITEMS -> Bukkit.getPluginManager().getPlugin("ExecutableItems") != null;
        };
    }

    /**
     * Kaynağa göre eşya üretir. VANILLA için, kaynak çözülemezse ya da eklenti kurulu değilse
     * null döner (çağıran vanilla ikona düşer).
     */
    public static ItemStack sourceItem(ItemSource source, String providerId, String base64, int amount) {
        ItemStack item = switch (source == null ? ItemSource.VANILLA : source) {
            case VANILLA -> null;
            case EXACT -> fromBase64(base64);
            default -> fromProvider(source, providerId);
        };
        if (item != null) {
            item.setAmount(Math.max(1, Math.min(64, amount)));
        }
        return item;
    }

    public static ItemStack fromBase64(String base64) {
        if (base64 == null || base64.isBlank()) {
            return null;
        }
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(base64));
        } catch (RuntimeException e) {
            warnOnce("base64", "A captured reward item could not be read (" + e + ").");
            return null;
        }
    }

    /** Eşyayı NBT baytları olarak Base64'e çevirir; boş eşyada boş metin. */
    public static String base64(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return "";
        }
        try {
            return Base64.getEncoder().encodeToString(item.serializeAsBytes());
        } catch (RuntimeException e) {
            warnOnce("serialize", "An item could not be captured (" + e + ").");
            return "";
        }
    }

    /** Web editörü için sağlayıcı eşya listesi (desteklenmeyen sağlayıcıda boş). */
    public static List<Entry> list(ItemSource source) {
        if (source == null || !source.isProvider() || !available(source)) {
            return List.of();
        }
        return CACHE.computeIfAbsent(source, CustomItems::buildList);
    }

    public static String displayName(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return "";
        }
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                // 1.20.5+: eşya adı item_name bileşeninde tutulur (Oraxen/ItemsAdder bu yolu kullanır).
                if (meta.hasItemName()) {
                    String plain = plain(meta.itemName());
                    if (!plain.isBlank()) {
                        return plain;
                    }
                }
                if (meta.hasDisplayName()) {
                    String plain = plain(meta.displayName());
                    if (!plain.isBlank()) {
                        return plain;
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // Meta okunamazsa malzeme adına düşülür.
        }
        return pretty(item.getType().name());
    }

    private static String plain(Component component) {
        return component == null ? "" : PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static String prettyMaterial(Material material) {
        return material == null ? "" : pretty(material.name());
    }

    public static String pretty(String raw) {
        String[] parts = raw.toLowerCase(Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }

    private static ItemStack fromProvider(ItemSource source, String id) {
        if (id == null || id.isBlank() || !available(source)) {
            return null;
        }
        try {
            return switch (source) {
                case ORAXEN -> oraxen(id.trim());
                case ITEMSADDER -> itemsAdder(id.trim());
                case MMOITEMS -> mmoItems(id.trim());
                case NEXO -> nexo(id.trim());
                case EXECUTABLEITEMS -> executableItems(id.trim());
                default -> null;
            };
        } catch (ReflectiveOperationException | RuntimeException e) {
            warnOnce(source.name() + ":" + id, source.label() + " item could not be built ('" + id + "'): " + e);
            return null;
        }
    }

    private static ItemStack oraxen(String id) throws ReflectiveOperationException {
        Class<?> items = Class.forName("io.th0rgal.oraxen.api.OraxenItems");
        Object exists = items.getMethod("exists", String.class).invoke(null, id);
        if (exists instanceof Boolean present && !present) {
            return null;
        }
        Object builder = items.getMethod("getItemById", String.class).invoke(null, id);
        return buildFrom(builder);
    }

    private static ItemStack itemsAdder(String id) throws ReflectiveOperationException {
        Class<?> customStack = Class.forName("dev.lone.itemsadder.api.CustomStack");
        Object stack = customStack.getMethod("getInstance", String.class).invoke(null, id);
        if (stack == null) {
            return null;
        }
        Method get = findMethod(stack.getClass(), "getItemStack");
        return get == null ? null : itemOf(get.invoke(stack));
    }

    /** Kimlik biçimi: {@code TUR:ID} (ör. SWORD:MYTHIC_BLADE). */
    private static ItemStack mmoItems(String id) throws ReflectiveOperationException {
        int colon = id.indexOf(':');
        if (colon <= 0 || colon == id.length() - 1) {
            return null;
        }
        String type = id.substring(0, colon).trim().toUpperCase(Locale.ROOT);
        String itemId = id.substring(colon + 1).trim().toUpperCase(Locale.ROOT);
        Class<?> items = Class.forName("net.Indyuce.mmoitems.MMOItems");
        for (Method method : items.getMethods()) {
            if (!method.getName().equals("getItem") || method.getParameterCount() != 2) {
                continue;
            }
            if (method.getParameterTypes()[0] != String.class || method.getParameterTypes()[1] != String.class) {
                continue;
            }
            ItemStack stack = itemOf(method.invoke(null, type, itemId));
            if (stack != null) {
                return stack;
            }
        }
        return null;
    }

    private static ItemStack nexo(String id) throws ReflectiveOperationException {
        Class<?> items = Class.forName("com.nexomc.nexo.api.NexoItems");
        Object builder = items.getMethod("itemFromId", String.class).invoke(null, id);
        if (builder instanceof Optional<?> optional) {
            builder = optional.orElse(null);
        }
        return buildFrom(builder);
    }

    private static ItemStack executableItems(String id) throws ReflectiveOperationException {
        Class<?> api = Class.forName("com.ssomar.score.api.executableitems.ExecutableItemsAPI");
        Object manager = api.getMethod("getExecutableItemsManager").invoke(null);
        if (manager == null) {
            return null;
        }
        Method get = findMethod(manager.getClass(), "getExecutableItem", String.class);
        if (get == null) {
            return null;
        }
        Object executable = get.invoke(manager, id);
        if (executable instanceof Optional<?> optional) {
            executable = optional.orElse(null);
        }
        if (executable == null) {
            return null;
        }
        for (Method method : executable.getClass().getMethods()) {
            if (!method.getName().equals("buildItem")) {
                continue;
            }
            Class<?>[] types = method.getParameterTypes();
            try {
                if (types.length == 3 && types[0] == int.class) {
                    return itemOf(method.invoke(executable, 1, null, new String[0]));
                }
                if (types.length == 2 && types[0] == int.class) {
                    return itemOf(method.invoke(executable, 1, null));
                }
                if (types.length == 1 && types[0] == int.class) {
                    return itemOf(method.invoke(executable, 1));
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Sonraki imza denenir.
            }
        }
        return null;
    }

    private static ItemStack buildFrom(Object builder) throws ReflectiveOperationException {
        if (builder instanceof Optional<?> optional) {
            builder = optional.orElse(null);
        }
        if (builder == null) {
            return null;
        }
        Method build = findMethod(builder.getClass(), "build");
        return build == null ? null : itemOf(build.invoke(builder));
    }

    private static ItemStack itemOf(Object value) {
        if (value instanceof ItemStack stack && !stack.getType().isAir()) {
            stack.setAmount(1);
            return stack;
        }
        return null;
    }

    private static List<Entry> buildList(ItemSource source) {
        try {
            return switch (source) {
                case ORAXEN -> oraxenList();
                case ITEMSADDER -> itemsAdderList();
                default -> List.of();
            };
        } catch (ReflectiveOperationException | RuntimeException e) {
            warnOnce("list:" + source.name(), "Could not list " + source.label() + " items: " + e);
            return List.of();
        }
    }

    private static List<Entry> oraxenList() throws ReflectiveOperationException {
        Class<?> items = Class.forName("io.th0rgal.oraxen.api.OraxenItems");
        // Oraxen 1.2xx: id -> ItemBuilder haritası (ItemBuilder'da id getter'ı yok).
        Method entriesAsMap = findMethod(items, "getEntriesAsMap");
        if (entriesAsMap != null && entriesAsMap.invoke(null) instanceof Map<?, ?> map && !map.isEmpty()) {
            List<Entry> list = new ArrayList<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String id) || id.isBlank()) {
                    continue;
                }
                list.add(new Entry(id, nameOfBuilder(entry.getValue(), id)));
            }
            list.sort((a, b) -> a.id().compareToIgnoreCase(b.id()));
            return list;
        }
        if (!(items.getMethod("getItems").invoke(null) instanceof Collection<?> builders)) {
            return List.of();
        }
        Method idOfBuilder = findMethod(items, "getIdByItem", builderType(items));
        List<Entry> list = new ArrayList<>();
        for (Object builder : builders) {
            String id = null;
            if (idOfBuilder != null) {
                Object value = idOfBuilder.invoke(null, builder);
                if (value instanceof String text) {
                    id = text;
                }
            }
            if (id == null) {
                id = textId(builder);
            }
            if (id == null || id.isBlank()) {
                continue;
            }
            list.add(new Entry(id, nameOfBuilder(builder, id)));
        }
        list.sort((a, b) -> a.id().compareToIgnoreCase(b.id()));
        return list;
    }

    private static Class<?> builderType(Class<?> items) {
        for (Method method : items.getMethods()) {
            if (method.getName().equals("getIdByItem") && method.getParameterCount() == 1
                    && !method.getParameterTypes()[0].getName().equals(ItemStack.class.getName())) {
                return method.getParameterTypes()[0];
            }
        }
        return null;
    }

    private static String nameOfBuilder(Object builder, String fallbackId) {
        if (builder == null) {
            return pretty(fallbackId);
        }
        try {
            Method configured = findMethod(builder.getClass(), "getDisplayName");
            if (configured != null && configured.invoke(builder) instanceof String text) {
                String clean = stripFormatting(text);
                if (!clean.isBlank()) {
                    return clean;
                }
            }
            Method build = findMethod(builder.getClass(), "build");
            if (build == null) {
                return pretty(fallbackId);
            }
            String name = displayName(itemOf(build.invoke(builder)));
            return name.isBlank() ? pretty(fallbackId) : name;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return pretty(fallbackId);
        }
    }

    /** Sağlayıcı adlarındaki renk kodlarını ve MiniMessage etiketlerini temizler. */
    private static String stripFormatting(String text) {
        return text.replaceAll("(?i)&[0-9a-fk-or]", "")
                .replaceAll("<[^>]+>", "")
                .replace("§", "")
                .trim();
    }

    private static List<Entry> itemsAdderList() throws ReflectiveOperationException {
        Class<?> customStack = Class.forName("dev.lone.itemsadder.api.CustomStack");
        // 4.x: kayıtlı tüm namespaced id'ler.
        if (customStack.getMethod("getNamespacedIdsInRegistry").invoke(null) instanceof Collection<?> ids) {
            List<Entry> list = new ArrayList<>();
            for (Object raw : ids) {
                if (!(raw instanceof String id) || id.isBlank()) {
                    continue;
                }
                String name = "";
                try {
                    Object stack = customStack.getMethod("getInstance", String.class).invoke(null, id);
                    if (stack != null) {
                        Method named = findMethod(stack.getClass(), "getDisplayName");
                        if (named != null && named.invoke(stack) instanceof String text && !text.isBlank()) {
                            name = text;
                        }
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // Ad çözülemezse id gösterilir.
                }
                list.add(new Entry(id, name.isBlank() ? pretty(id) : name));
            }
            list.sort((a, b) -> a.id().compareToIgnoreCase(b.id()));
            return list;
        }
        return List.of();
    }

    private static String textId(Object target) {
        for (String name : List.of("getItemID", "getNamespacedID", "getID", "getId")) {
            Method method = findMethod(target.getClass(), name);
            if (method == null) {
                continue;
            }
            try {
                Object value = method.invoke(target);
                if (value instanceof String text && !text.isBlank()) {
                    return text;
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Sonraki aday denenir.
            }
        }
        return null;
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... parameters) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Method method = current.getDeclaredMethod(name, parameters);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                // Üst sınıfta aranır.
            }
        }
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == parameters.length) {
                return method;
            }
        }
        return null;
    }

    private static void warnOnce(String key, String message) {
        if (plugin == null || WARNED.putIfAbsent(key, Boolean.TRUE) != null) {
            return;
        }
        plugin.getLogger().warning(message);
    }
}
