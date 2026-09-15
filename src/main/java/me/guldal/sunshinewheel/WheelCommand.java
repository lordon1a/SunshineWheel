package me.guldal.sunshinewheel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import me.guldal.sunshinewheel.editor.DialogEditor;
import me.guldal.sunshinewheel.editor.EditorManager;
import me.guldal.sunshinewheel.menu.ChestMenu;
import me.guldal.sunshinewheel.wheel.MotionStyle;
import me.guldal.sunshinewheel.wheel.WheelInstance;
import me.guldal.sunshinewheel.wheel.WheelManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.Material;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Yönetim komutları. Ayarların çoğu /wheel edit ile açılan editörden yapılır. */
final class WheelCommand implements TabExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final List<String> SUBS = List.of("create", "edit", "dialog", "menu", "move", "cancel", "remove",
            "list", "spin", "set", "tune", "ui", "angle", "reload");
    private static final List<String> TUNE_KEYS = List.of("text-pixel-blocks", "text-top-units", "text-line-units",
            "interpolation-ticks", "panel-pixel-blocks", "panel-distance");
    private static final List<String> SET_KEYS = List.of("style", "duration", "revolutions", "pixel-size",
            "image-size", "upright", "item-scale");
    private static final String PLACE_HINT = "<gold>Çarkı koymak istediğin bloğa sağ tıkla.</gold><newline>"
            + "<gray>Zemine tıklarsan bloğun üstünde durur ve sana bakar, duvara tıklarsan duvara asılır. İptal: /wheel cancel";

    private final WheelManager manager;
    private final DialogEditor dialogs;
    private final EditorManager editors;

    WheelCommand(WheelManager manager, DialogEditor dialogs, EditorManager editors) {
        this.manager = manager;
        this.dialogs = dialogs;
        this.editors = editors;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return usage(sender);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create" -> {
                if (!(sender instanceof Player player) || args.length < 2) {
                    return msg(sender, "<red>Kullanım (oyunda): /wheel create <id>");
                }
                if (manager.get(args[1]) != null || manager.isPendingCreate(args[1])) {
                    return msg(sender, "<red>Bu id zaten var.");
                }
                manager.startPlacement(player, args[1], false);
                return msg(sender, PLACE_HINT);
            }
            case "edit" -> {
                WheelInstance wheel = args.length < 2 ? null : manager.get(args[1]);
                if (!(sender instanceof Player player) || wheel == null) {
                    return msg(sender, "<red>Kullanım (oyunda): /wheel edit <id>");
                }
                editors.open(player, wheel);
                return true;
            }
            case "dialog" -> {
                WheelInstance wheel = args.length < 2 ? null : manager.get(args[1]);
                if (!(sender instanceof Player player) || wheel == null) {
                    return msg(sender, "<red>Kullanım (oyunda): /wheel dialog <id>");
                }
                dialogs.open(player, wheel);
                return true;
            }
            case "move" -> {
                if (!(sender instanceof Player player) || args.length < 2 || manager.get(args[1]) == null) {
                    return msg(sender, "<red>Kullanım (oyunda): /wheel move <id>");
                }
                manager.startPlacement(player, args[1], true);
                return msg(sender, PLACE_HINT);
            }
            case "cancel" -> {
                if (!(sender instanceof Player player)) {
                    return msg(sender, "<red>Bu komut oyunda kullanılır.");
                }
                return msg(sender, manager.cancelPlacement(player.getUniqueId())
                        ? "<gray>Yerleştirme iptal edildi." : "<gray>Bekleyen bir yerleştirme yok.");
            }
            case "remove" -> {
                if (args.length < 2) {
                    return msg(sender, "<red>Kullanım: /wheel remove <id>");
                }
                return msg(sender, manager.remove(args[1]) ? "<green>Silindi." : "<red>Böyle bir çark yok.");
            }
            case "list" -> {
                if (manager.ids().isEmpty()) {
                    return msg(sender, "<gray>Hiç çark yok.");
                }
                manager.ids().forEach(id -> msg(sender, "<gold>• <white>" + id + " <gray>" + manager.get(id).def.summary()));
                return true;
            }
            case "spin" -> {
                WheelInstance wheel = args.length < 2 ? null : manager.get(args[1]);
                if (!(sender instanceof Player player) || wheel == null) {
                    return msg(sender, "<red>Kullanım (oyunda): /wheel spin <id>");
                }
                return msg(sender, wheel.spin(player) ? "<gray>Dönüyor..." : "<red>Çark zaten dönüyor ya da ödülü yok.");
            }
            case "set" -> {
                return set(sender, args);
            }
            case "tune" -> {
                return tune(sender, args);
            }
            case "menu" -> {
                if (!(sender instanceof Player player)) {
                    return msg(sender, "<red>Bu komut oyunda kullanılır.");
                }
                openTestMenu(player);
                return msg(sender, "<gray>Test menüsü açıldı. Altın külçeleri çizimdeki kutuların köşelerine denk geliyor mu?");
            }
            case "ui" -> {
                if (args.length < 3) {
                    return msg(sender, "<gray>Panel kaydırması şu an: <white>" + manager.settings().uiOffsetX
                            + ", " + manager.settings().uiOffsetY
                            + "</white><newline><red>Kullanım: /wheel ui <dx> <dy> <gray>(GUI pikseli, eksi değer sol/yukarı)");
                }
                try {
                    manager.settings().uiOffsetX = Integer.parseInt(args[1]);
                    manager.settings().uiOffsetY = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    return msg(sender, "<red>Geçersiz sayı.");
                }
                manager.saveSettings();
                return msg(sender, "<green>Panel kaydırması güncellendi.</green> <gray>Editörü kapatıp tekrar aç.");
            }
            case "angle" -> {
                WheelInstance wheel = args.length < 3 ? null : manager.get(args[1]);
                if (wheel == null) {
                    return msg(sender, "<red>Kullanım: /wheel angle <id> <derece>");
                }
                try {
                    int index = wheel.debugSetAngle(Double.parseDouble(args[2]));
                    return msg(sender, "<gray>Okun altındaki dilim (kod): <white>" + (index + 1) + ". "
                            + wheel.def.rewards.get(index).name + "</white> <gray>— görsel olarak aynı dilim mi?");
                } catch (NumberFormatException e) {
                    return msg(sender, "<red>Geçersiz sayı.");
                }
            }
            case "reload" -> {
                manager.load();
                manager.spawnAll();
                return msg(sender, "<green>wheels.yml yeniden yüklendi.");
            }
            default -> {
                return usage(sender);
            }
        }
    }

    /**
     * Hizalama testi: sandık menüsünü sadece arka plan resmi ve birkaç işaret eşyasıyla açar.
     * Eşyalar çizimdeki kutuların köşelerine denk geliyorsa ölçüler doğru demektir.
     */
    private void openTestMenu(Player player) {
        ChestMenu menu = new ChestMenu(6, ChestMenu.background(
                MM.deserialize("<gold>Hizalama testi"),
                manager.settings().uiOffsetX, manager.settings().uiOffsetY));
        int[][] markers = {{0, 0}, {0, 5}, {2, 0}, {6, 5}, {7, 0}, {8, 4}};
        for (int[] cell : markers) {
            menu.set(cell[0], cell[1], marker(cell[0], cell[1]), viewer -> { });
        }
        menu.open(player);
    }

    private static ItemStack marker(int column, int row) {
        ItemStack item = new ItemStack(Material.GOLD_NUGGET);
        item.editMeta(meta -> meta.displayName(MM.deserialize(
                "<gold>sütun " + column + ", satır " + row + "</gold>")));
        return item;
    }

    private boolean set(CommandSender sender, String[] args) {
        WheelInstance wheel = args.length < 4 ? null : manager.get(args[1]);
        if (wheel == null) {
            return msg(sender, "<red>Kullanım: /wheel set <id> <" + String.join("|", SET_KEYS) + "> <değer>");
        }
        String value = args[3];
        try {
            switch (args[2].toLowerCase(Locale.ROOT)) {
                case "style" -> wheel.def.style = MotionStyle.valueOf(value.toUpperCase(Locale.ROOT));
                case "duration" -> wheel.def.duration = Math.max(20, Integer.parseInt(value));
                case "revolutions" -> wheel.def.revolutions = Math.max(1, Double.parseDouble(value));
                case "pixel-size" -> wheel.def.pixelSize = Double.parseDouble(value);
                case "image-size" -> wheel.def.imageSize = Math.max(32, Math.min(256, Integer.parseInt(value)));
                case "upright" -> wheel.def.itemsUpright = Boolean.parseBoolean(value);
                case "item-scale" -> wheel.def.itemScale = Double.parseDouble(value);
                default -> {
                    return msg(sender, "<red>Bilinmeyen ayar.");
                }
            }
        } catch (IllegalArgumentException e) {
            return msg(sender, "<red>Geçersiz değer: " + value);
        }
        manager.save();
        manager.respawn(wheel);
        return msg(sender, "<green>Güncellendi.");
    }

    private boolean tune(CommandSender sender, String[] args) {
        if (args.length < 3) {
            return msg(sender, "<gray>Şu an: text-pixel-blocks=<white>" + manager.settings().textPixelBlocks
                    + "</white> text-top-units=<white>" + manager.settings().textTopUnits
                    + "</white> text-line-units=<white>" + manager.settings().textLineUnits
                    + "</white> interpolation-ticks=<white>" + manager.settings().interpolationTicks
                    + "</white><newline><red>Kullanım: /wheel tune <" + String.join("|", TUNE_KEYS) + "> <değer>");
        }
        try {
            double value = Double.parseDouble(args[2]);
            switch (args[1].toLowerCase(Locale.ROOT)) {
                case "text-pixel-blocks" -> manager.settings().textPixelBlocks = value;
                case "text-top-units" -> manager.settings().textTopUnits = value;
                case "text-line-units" -> manager.settings().textLineUnits = value;
                case "interpolation-ticks" -> manager.settings().interpolationTicks = Math.max(1, (int) value);
                case "panel-pixel-blocks" -> manager.settings().panelPixelBlocks = value;
                case "panel-distance" -> manager.settings().panelDistance = value;
                default -> {
                    return msg(sender, "<red>Bilinmeyen kalibrasyon değeri.");
                }
            }
        } catch (NumberFormatException e) {
            return msg(sender, "<red>Geçersiz sayı.");
        }
        manager.saveSettings();
        manager.ids().forEach(id -> manager.respawn(manager.get(id)));
        return msg(sender, "<green>Kalibrasyon güncellendi, çarklar yeniden çizildi.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(SUBS);
        } else if (args.length == 2 && List.of("edit", "dialog", "move", "remove", "spin", "set", "angle").contains(args[0].toLowerCase(Locale.ROOT))) {
            options.addAll(manager.ids());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("tune")) {
            options.addAll(TUNE_KEYS);
        } else if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            options.addAll(SET_KEYS);
        } else if (args.length == 4 && args[0].equalsIgnoreCase("set") && args[2].equalsIgnoreCase("style")) {
            for (MotionStyle style : MotionStyle.values()) {
                options.add(style.name());
            }
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }

    private static boolean usage(CommandSender sender) {
        return msg(sender, "<gold>SunshineWheel</gold> <gray>/wheel " + String.join(" | ", SUBS));
    }

    private static boolean msg(CommandSender sender, String miniMessage) {
        sender.sendMessage(MM.deserialize(miniMessage));
        return true;
    }
}
