package me.guldal.sunshinewheel.editor;

import io.papermc.paper.dialog.DialogResponseView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.guldal.sunshinewheel.wheel.RenderSettings;
import me.guldal.sunshinewheel.wheel.Reward;
import me.guldal.sunshinewheel.wheel.WheelInstance;
import me.guldal.sunshinewheel.wheel.WheelManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Hangi oyuncunun hangi çarkı düzenlediğini tutar ve ekran tıklamalarını uygular. */
public final class DialogEditor {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final WheelManager wheels;
    private final RenderSettings settings;
    private final Map<UUID, String> editing = new HashMap<>();

    public DialogEditor(WheelManager wheels) {
        this.wheels = wheels;
        this.settings = wheels.settings();
    }

    public void open(Player player, WheelInstance wheel) {
        editing.put(player.getUniqueId(), wheel.def.id);
        player.showDialog(WheelDialogs.menu(wheel.def, settings));
    }

    public void forget(UUID playerId) {
        editing.remove(playerId);
    }

    private WheelInstance current(Player player) {
        String id = editing.get(player.getUniqueId());
        return id == null ? null : wheels.get(id);
    }

    /**
     * Ekrandan gelen tıklamayı işler.
     *
     * @param path anahtarın değeri, ör. {@code apply/animation/menu}
     */
    public void handle(Player player, String path, DialogResponseView view) {
        WheelInstance wheel = current(player);
        if (wheel == null) {
            return;
        }
        String[] parts = path.split("/");
        switch (parts[0]) {
            case "open" -> {
                if (parts.length > 1) {
                    player.showDialog(WheelDialogs.tab(wheel.def, parts[1], settings));
                }
            }
            case "apply" -> {
                if (parts.length > 2) {
                    WheelDialogs.applyTab(wheel.def, parts[1], view);
                    save(wheel);
                    navigate(player, wheel, parts[1], parts[2]);
                }
            }
            case "reward" -> reward(player, wheel, parts, view);
            case "spin" -> {
                if (!wheel.spin(player)) {
                    player.sendMessage(MM.deserialize("<red>Çark şu an dönüyor ya da ödülü yok."));
                }
            }
            case "move" -> {
                wheels.startPlacement(player, wheel.def.id, true);
                player.sendMessage(MM.deserialize("<gold>Çarkı koymak istediğin bloğa sağ tıkla.</gold> <gray>İptal: /wheel cancel"));
            }
            case "close" -> forget(player.getUniqueId());
            default -> {
                // bilinmeyen anahtar: yok say
            }
        }
    }

    private void reward(Player player, WheelInstance wheel, String[] parts, DialogResponseView view) {
        if (parts.length < 2) {
            return;
        }
        switch (parts[1]) {
            case "open" -> {
                int index = index(parts, 2);
                if (index >= 0 && index < wheel.def.rewards.size()) {
                    player.showDialog(WheelDialogs.reward(wheel.def, index, settings));
                }
            }
            case "apply" -> {
                int index = index(parts, 2);
                WheelDialogs.applyReward(wheel.def, index, view);
                save(wheel);
                if (parts.length > 3 && parts[3].equals("list")) {
                    player.showDialog(WheelDialogs.tab(wheel.def, "rewards", settings));
                } else if (index >= 0 && index < wheel.def.rewards.size()) {
                    player.showDialog(WheelDialogs.reward(wheel.def, index, settings));
                }
            }
            case "delete" -> {
                int index = index(parts, 2);
                if (index >= 0 && index < wheel.def.rewards.size() && wheel.def.rewards.size() > 1) {
                    wheel.def.rewards.remove(index);
                    save(wheel);
                } else {
                    player.sendMessage(MM.deserialize("<red>Son ödül silinemez."));
                }
                player.showDialog(WheelDialogs.tab(wheel.def, "rewards", settings));
            }
            case "add" -> {
                wheel.def.rewards.add(new Reward("<white>Yeni ödül", 10, 0x8A8A8A, Material.CHEST,
                        List.of("tell %player% [Test] yeni ödül")));
                save(wheel);
                player.showDialog(WheelDialogs.reward(wheel.def, wheel.def.rewards.size() - 1, settings));
            }
            default -> {
                // yok say
            }
        }
    }

    private void navigate(Player player, WheelInstance wheel, String tab, String next) {
        switch (next) {
            case "menu" -> player.showDialog(WheelDialogs.menu(wheel.def, settings));
            case "close" -> forget(player.getUniqueId());
            default -> player.showDialog(WheelDialogs.tab(wheel.def, tab, settings));
        }
    }

    private void save(WheelInstance wheel) {
        wheels.save();
        wheels.respawn(wheel);
    }

    private static int index(String[] parts, int position) {
        if (parts.length <= position) {
            return -1;
        }
        try {
            return Integer.parseInt(parts[position]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
