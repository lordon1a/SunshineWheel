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
    private final RenderSettings settings;
    private final Map<String, WheelInstance> wheels = new LinkedHashMap<>();
    private final Map<UUID, Placement> placements = new HashMap<>();

    /** Zemine/tavana konan çarkın blokla arasındaki boşluk. */
    private static final double PLACE_GAP = 0.05;

    public WheelManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.partKey = new NamespacedKey(plugin, "part");
        this.file = new File(plugin.getDataFolder(), "wheels.yml");
        this.settings = new RenderSettings(
                plugin.getConfig().getDouble("text-pixel-blocks", 0.025),
                plugin.getConfig().getDouble("text-top-units", 3.0),
                plugin.getConfig().getDouble("text-line-units", 10.0),
                plugin.getConfig().getInt("interpolation-ticks", 3),
                plugin.getConfig().getDouble("panel-pixel-blocks", 0.018),
                plugin.getConfig().getDouble("panel-distance", 2.6),
                plugin.getConfig().getInt("ui-offset-x", 0),
                plugin.getConfig().getInt("ui-offset-y", 0));
    }

    public RenderSettings settings() {
        return settings;
    }

    public void load() {
        despawnAll();
        wheels.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("wheels");
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section != null) {
                wheels.put(id, new WheelInstance(plugin, partKey, settings, WheelDefinition.load(id, section)));
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        wheels.forEach((id, instance) -> instance.def.save(yaml.createSection("wheels." + id)));
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "wheels.yml kaydedilemedi", e);
        }
    }

    public void saveSettings() {
        plugin.getConfig().set("text-pixel-blocks", settings.textPixelBlocks);
        plugin.getConfig().set("text-top-units", settings.textTopUnits);
        plugin.getConfig().set("text-line-units", settings.textLineUnits);
        plugin.getConfig().set("interpolation-ticks", settings.interpolationTicks);
        plugin.getConfig().set("panel-pixel-blocks", settings.panelPixelBlocks);
        plugin.getConfig().set("panel-distance", settings.panelDistance);
        plugin.getConfig().set("ui-offset-x", settings.uiOffsetX);
        plugin.getConfig().set("ui-offset-y", settings.uiOffsetY);
        plugin.saveConfig();
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
            def = WheelDefinition.withSampleRewards(placement.id());
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
            instance = new WheelInstance(plugin, partKey, settings, def);
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

    public boolean remove(String id) {
        WheelInstance instance = wheels.remove(id);
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
            plugin.getLogger().info(removed + " eski çark parçası temizlendi.");
        }
    }
}
