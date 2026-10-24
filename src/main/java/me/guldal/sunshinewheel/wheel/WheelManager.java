package me.guldal.sunshinewheel.wheel;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class WheelManager {

    private final JavaPlugin plugin;
    private final NamespacedKey partKey;
    private final File file;
    private final File dataFile;
    private final RenderSettings settings;
    private final Map<String, WheelInstance> wheels = new LinkedHashMap<>();
    private final Map<UUID, Placement> placements = new HashMap<>();
    private final Map<String, Map<UUID, PlayerStats>> playerData = new HashMap<>();

    /** Çark başına oyuncu verisi (ücretsiz çevirme, sayaçlar, bekleme). data.yml'de saklanır. */
    public static final class PlayerStats {
        public int freeSpins;
        public int spins;
        public int wins;
        public long lastSpinMillis;
    }

    /** Zemine/tavana konan çarkın blokla arasındaki boşluk. */
    private static final double PLACE_GAP = 0.05;

    public WheelManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.partKey = new NamespacedKey(plugin, "part");
        this.file = new File(plugin.getDataFolder(), "wheels.yml");
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        this.settings = new RenderSettings(
                plugin.getConfig().getDouble("text-pixel-blocks", 0.025),
                plugin.getConfig().getDouble("text-top-units", 3.0),
                plugin.getConfig().getDouble("text-line-units", 10.0),
                plugin.getConfig().getInt("interpolation-ticks", 3));
    }

    public RenderSettings settings() {
        return settings;
    }

    /** Sol tıkla açılan şans menüsü açık mı? (config: menu.enabled) */
    public boolean menuEnabled() {
        return plugin.getConfig().getBoolean("menu.enabled", true);
    }

    public void load() {
        despawnAll();
        wheels.clear();
        boolean firstRun = !file.exists();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("wheels");
        if (root != null) {
            for (String id : root.getKeys(false)) {
                ConfigurationSection section = root.getConfigurationSection(id);
                if (section != null) {
                    String ticketRaw = section.getString("cost.ticket-item", "");
                    if (!ticketRaw.isBlank() && org.bukkit.Material.matchMaterial(ticketRaw) == null) {
                        plugin.getLogger().warning("[" + id + "] cost.ticket-item '" + ticketRaw
                                + "' not recognized; ticket treated as disabled.");
                    }
                    wheels.put(id, new WheelInstance(this, plugin, partKey, settings, WheelDefinition.load(id, section)));
                }
            }
        }
        if (firstRun && wheels.isEmpty()) {
            for (WheelDefinition def : WheelDefinition.samples()) {
                wheels.put(def.id, new WheelInstance(this, plugin, partKey, settings, def));
            }
            save();
            plugin.getLogger().info("Sample wheels created: daily, premium, vote. Place them with /wheel move <id>.");
        }
        loadData();
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        wheels.forEach((id, instance) -> instance.def.save(yaml.createSection("wheels." + id)));
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save wheels.yml", e);
        }
    }

    public void saveSettings() {
        plugin.getConfig().set("text-pixel-blocks", settings.textPixelBlocks);
        plugin.getConfig().set("text-top-units", settings.textTopUnits);
        plugin.getConfig().set("text-line-units", settings.textLineUnits);
        plugin.getConfig().set("interpolation-ticks", settings.interpolationTicks);
        plugin.saveConfig();
    }

    /** Çark+oyuncu verisi (bellekte yoksa oluşturur). */
    public PlayerStats stats(String wheelId, UUID playerId) {
        return playerData.computeIfAbsent(wheelId, key -> new HashMap<>())
                .computeIfAbsent(playerId, key -> new PlayerStats());
    }

    public void addFreeSpins(String wheelId, UUID playerId, int amount) {
        PlayerStats data = stats(wheelId, playerId);
        data.freeSpins = Math.max(0, data.freeSpins + amount);
        saveData();
    }

    /** Ücretsiz çevirme hakkı varsa birini harcar. */
    public boolean useFreeSpin(String wheelId, UUID playerId) {
        PlayerStats data = stats(wheelId, playerId);
        if (data.freeSpins <= 0) {
            return false;
        }
        data.freeSpins--;
        saveData();
        return true;
    }

    public void recordSpin(String wheelId, UUID playerId) {
        PlayerStats data = stats(wheelId, playerId);
        data.spins++;
        data.lastSpinMillis = System.currentTimeMillis();
        saveData();
    }

    public void recordWin(String wheelId, UUID playerId) {
        stats(wheelId, playerId).wins++;
        saveData();
    }

    public long lastSpinMillis(String wheelId, UUID playerId) {
        Map<UUID, PlayerStats> map = playerData.get(wheelId);
        if (map == null) {
            return 0;
        }
        PlayerStats data = map.get(playerId);
        return data == null ? 0 : data.lastSpinMillis;
    }

    /** Oyuncunun bekleme süresini sıfırlar. */
    public boolean resetCooldown(String wheelId, UUID playerId) {
        if (!wheels.containsKey(wheelId)) {
            return false;
        }
        stats(wheelId, playerId).lastSpinMillis = 0;
        saveData();
        return true;
    }

    private void loadData() {
        playerData.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection root = yaml.getConfigurationSection("data");
        if (root == null) {
            return;
        }
        for (String wheelId : root.getKeys(false)) {
            ConfigurationSection wheelSection = root.getConfigurationSection(wheelId);
            if (wheelSection == null) {
                continue;
            }
            Map<UUID, PlayerStats> map = new HashMap<>();
            for (String key : wheelSection.getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(key);
                    ConfigurationSection section = wheelSection.getConfigurationSection(key);
                    if (section == null) {
                        continue;
                    }
                    PlayerStats data = new PlayerStats();
                    data.freeSpins = Math.max(0, section.getInt("free"));
                    data.spins = Math.max(0, section.getInt("spins"));
                    data.wins = Math.max(0, section.getInt("wins"));
                    data.lastSpinMillis = Math.max(0, section.getLong("cooldown"));
                    map.put(playerId, data);
                } catch (IllegalArgumentException ignored) {
                    // Bozuk UUID satırı atlanır.
                }
            }
            playerData.put(wheelId, map);
        }
    }

    private void saveData() {
        YamlConfiguration yaml = new YamlConfiguration();
        playerData.forEach((wheelId, map) -> map.forEach((playerId, data) -> {
            String base = "data." + wheelId + "." + playerId;
            yaml.set(base + ".free", data.freeSpins);
            yaml.set(base + ".spins", data.spins);
            yaml.set(base + ".wins", data.wins);
            yaml.set(base + ".cooldown", data.lastSpinMillis);
        }));
        try {
            yaml.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save data.yml", e);
        }
    }

    /** Komuttan sonra oyuncunun sağ tıklayacağı bloğu bekleyen yerleştirme (yeni çark ya da taşıma). */
    public record Placement(String id, boolean move) {
    }

    public void startPlacement(Player player, String id, boolean move) {
        placements.put(player.getUniqueId(), new Placement(id, move));
    }

    public Placement placement(UUID playerId) {
        return placements.get(playerId);
    }

    public boolean cancelPlacement(UUID playerId) {
        return placements.remove(playerId) != null;
    }

    public boolean isPendingCreate(String id) {
        return placements.values().stream().anyMatch(p -> !p.move() && p.id().equals(id));
    }

    /**
     * Çarkı tıklanan yüzeye yerleştirir: zemine tıklanırsa alt kenarı bloğun üstünde durur ve oyuncuya bakar,
     * duvara tıklanırsa yüzü duvardan dışarı bakacak şekilde asılır, tavana tıklanırsa aşağı sarkar.
     *
     * @return yerleştirilen çark; yerleştirme geçersizse (çark dönüyor, id çakışması) null
     */
    public WheelInstance place(Player player, Block clicked, BlockFace face) {
        Placement placement = placements.remove(player.getUniqueId());
        if (placement == null) {
            return null;
        }
        WheelInstance instance = wheels.get(placement.id());
        WheelDefinition def;
        if (placement.move()) {
            if (instance == null || instance.isSpinning()) {
                return null;
            }
            def = instance.def;
        } else {
            if (instance != null) {
                return null;
            }
            def = WheelDefinition.withStarterRewards(placement.id());
        }

        double radius = def.imageSize / 2.0 * def.pixelSize;
        double centerOffset = settings.imageCenterY(def.imageSize, def.pixelSize);
        double x = clicked.getX() + 0.5;
        double z = clicked.getZ() + 0.5;
        double centerY;
        float yaw;
        switch (face) {
            case DOWN -> {
                centerY = clicked.getY() - PLACE_GAP - radius;
                yaw = snappedFacing(player);
            }
            case NORTH, SOUTH, EAST, WEST -> {
                x += face.getModX() * 0.55;
                z += face.getModZ() * 0.55;
                centerY = clicked.getY() + 0.5;
                yaw = yawOutOf(face);
            }
            default -> {
                centerY = clicked.getY() + 1 + PLACE_GAP + radius;
                yaw = snappedFacing(player);
            }
        }

        def.world = clicked.getWorld().getName();
        def.x = x;
        def.y = centerY - centerOffset;
        def.z = z;
        def.yaw = yaw;

        if (instance == null) {
            instance = new WheelInstance(this, plugin, partKey, settings, def);
            wheels.put(def.id, instance);
        }
        save();
        respawn(instance);
        return instance;
    }

    /** Oyuncuya bakan yön, 90 derecelik açılara hizalı. */
    private static float snappedFacing(Player player) {
        return Math.round((player.getLocation().getYaw() + 180f) / 90f) * 90f;
    }

    /** Çarkın ön yüzü (yerel +z) bu yöne baksın diye gereken yaw. */
    private static float yawOutOf(BlockFace face) {
        return switch (face) {
            case SOUTH -> 0f;
            case WEST -> 90f;
            case NORTH -> 180f;
            default -> -90f;
        };
    }

    /** Web arayüzünden yeni çark: tanımı haritaya ekler, kaydeder ve dünyaya kurar. */
    public WheelInstance create(WheelDefinition def) {
        if (wheels.containsKey(def.id)) {
            return null;
        }
        WheelInstance instance = new WheelInstance(this, plugin, partKey, settings, def);
        wheels.put(def.id, instance);
        save();
        respawn(instance);
        return instance;
    }

    public boolean remove(String id) {
        WheelInstance instance = wheels.remove(id);
        placements.entrySet().removeIf(entry -> entry.getValue().id().equals(id));
        if (instance == null) {
            return false;
        }
        instance.despawn();
        save();
        return true;
    }

    public WheelInstance get(String id) {
        return wheels.get(id);
    }

    public Collection<String> ids() {
        return wheels.keySet();
    }

    public WheelInstance byHitbox(UUID entityId) {
        for (WheelInstance instance : wheels.values()) {
            if (instance.hitbox() != null && instance.hitbox().getUniqueId().equals(entityId)) {
                return instance;
            }
        }
        return null;
    }

    public void respawn(WheelInstance instance) {
        instance.despawn();
        instance.spawn();
    }

    public void spawnAll() {
        removeOrphans();
        wheels.values().forEach(WheelInstance::spawn);
    }

    public void despawnAll() {
        wheels.values().forEach(WheelInstance::despawn);
    }

    public void onChunkLoad(Chunk chunk) {
        for (WheelInstance instance : wheels.values()) {
            Location base = instance.base();
            if (base != null && !instance.isSpawned() && base.getWorld().equals(chunk.getWorld())
                    && base.getBlockX() >> 4 == chunk.getX() && base.getBlockZ() >> 4 == chunk.getZ()) {
                instance.spawn();
            }
        }
    }

    /** Çökme sonrası dünyada kalmış eski çark parçalarını temizler. */
    private void removeOrphans() {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getPersistentDataContainer().has(partKey, PersistentDataType.STRING)) {
                    entity.remove();
                    removed++;
                }
            }
        }
        if (removed > 0) {
            plugin.getLogger().info("Cleaned up " + removed + " leftover wheel part(s).");
        }
    }
}
