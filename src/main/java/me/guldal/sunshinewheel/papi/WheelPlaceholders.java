package me.guldal.sunshinewheel.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.guldal.sunshinewheel.wheel.WheelInstance;
import me.guldal.sunshinewheel.wheel.WheelManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Yer tutucular: {@code %sunshinewheel_<id>_spins%}, {@code _wins}, {@code _free},
 * {@code _cooldown} (kalan saniye, yoksa boş).
 */
public final class WheelPlaceholders extends PlaceholderExpansion {

    private final WheelManager wheels;
    private final String version;

    public WheelPlaceholders(WheelManager wheels, String version) {
        this.wheels = wheels;
        this.version = version;
    }

    @Override
    public String getIdentifier() {
        return "sunshinewheel";
    }

    @Override
    public String getAuthor() {
        return "Sunshine Survival";
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) {
            return null;
        }
        int cut = params.lastIndexOf('_');
        if (cut <= 0) {
            return null;
        }
        WheelInstance instance = wheels.get(params.substring(0, cut));
        if (instance == null) {
            return null;
        }
        String metric = params.substring(cut + 1);
        WheelManager.PlayerStats stats = wheels.stats(instance.def.id, player.getUniqueId());
        return switch (metric) {
            case "spins" -> String.valueOf(stats.spins);
            case "wins" -> String.valueOf(stats.wins);
            case "free" -> String.valueOf(stats.freeSpins);
            case "cooldown" -> {
                Player online = player.getPlayer();
                if (online == null) {
                    yield "";
                }
                long left = instance.cooldownLeft(online);
                yield left <= 0 ? "" : String.valueOf(left);
            }
            default -> null;
        };
    }
}
