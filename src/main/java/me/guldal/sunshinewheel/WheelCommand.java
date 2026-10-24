package me.guldal.sunshinewheel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import me.guldal.sunshinewheel.integration.CustomItems;
import me.guldal.sunshinewheel.web.WebServer;
import me.guldal.sunshinewheel.wheel.ChanceMenu;
import me.guldal.sunshinewheel.wheel.ItemSource;
import me.guldal.sunshinewheel.wheel.MotionStyle;
import me.guldal.sunshinewheel.wheel.Reward;
import me.guldal.sunshinewheel.wheel.WheelInstance;
import me.guldal.sunshinewheel.wheel.WheelManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Yönetim komutları. Çark ayarları web editörden yapılır. */
final class WheelCommand implements TabExecutor {

    private static final List<String> SUBS = List.of("create", "move", "cancel", "remove", "list", "spin", "set",
            "tune", "angle", "reload", "web", "give", "take", "ticket", "stats", "resetcooldown", "preview",
            "menu", "setitem");
    private static final List<String> TUNE_KEYS = List.of("text-pixel-blocks", "text-top-units", "text-line-units",
            "interpolation-ticks");
    private static final List<String> SET_KEYS = List.of("style", "duration", "revolutions", "pixel-size",
            "image-size", "upright", "item-scale", "weighted-slices");

    private final WheelManager manager;
    private final WebServer web;

    WheelCommand(WheelManager manager, WebServer web) {
        this.manager = manager;
        this.web = web;
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
                    return msg(sender, Messages.mm("command.create-usage"));
                }
                String id = args[1].toLowerCase(Locale.ROOT);
                if (!id.matches("[a-z0-9_-]{1,32}")) {
                    return msg(sender, Messages.mm("command.bad-id"));
                }
                if (manager.get(id) != null || manager.isPendingCreate(id)) {
                    return msg(sender, Messages.mm("command.id-taken"));
                }
                manager.startPlacement(player, id, false);
                return msg(sender, Messages.mm("place.hint"));
            }
            case "move" -> {
                if (!(sender instanceof Player player) || args.length < 2 || manager.get(args[1]) == null) {
                    return msg(sender, Messages.mm("command.move-usage"));
                }
                manager.startPlacement(player, args[1], true);
                return msg(sender, Messages.mm("place.hint"));
            }
            case "cancel" -> {
                if (!(sender instanceof Player player)) {
                    return msg(sender, Messages.mm("command.only-ingame"));
                }
                return msg(sender, manager.cancelPlacement(player.getUniqueId())
                        ? Messages.mm("command.cancelled") : Messages.mm("command.no-placement"));
            }
            case "remove" -> {
                if (args.length < 2) {
                    return msg(sender, Messages.mm("command.remove-usage"));
                }
                WheelInstance wheel = manager.get(args[1]);
                if (wheel == null) {
                    return msg(sender, Messages.mm("command.no-wheel"));
                }
                if (wheel.isSpinning()) {
                    return msg(sender, Messages.mm("command.spinning-wait"));
                }
                return msg(sender, manager.remove(args[1])
                        ? Messages.mm("command.deleted") : Messages.mm("command.no-wheel"));
            }
            case "list" -> {
                if (manager.ids().isEmpty()) {
                    return msg(sender, Messages.mm("command.list-empty"));
                }
                manager.ids().forEach(id -> msg(sender, Messages.mm("command.list-line",
                        "id", id, "summary", manager.get(id).def.summary())));
                return true;
            }
            case "spin" -> {
                WheelInstance wheel = args.length < 2 ? null : manager.get(args[1]);
                if (!(sender instanceof Player player) || wheel == null) {
                    return msg(sender, Messages.mm("command.spin-usage"));
                }
                return msg(sender, wheel.spin(player)
                        ? Messages.mm("command.spin-start") : Messages.mm("command.spin-fail"));
            }
            case "set" -> {
                return set(sender, args);
            }
            case "tune" -> {
                return tune(sender, args);
            }
            case "angle" -> {
                WheelInstance wheel = args.length < 3 ? null : manager.get(args[1]);
                if (wheel == null) {
                    return msg(sender, Messages.mm("command.angle-usage"));
                }
                try {
                    int index = wheel.debugSetAngle(Double.parseDouble(args[2]));
                    if (index < 0 || index >= wheel.visibleRewards().size()) {
                        return msg(sender, Messages.mm("command.angle-none"));
                    }
                    return msg(sender, Messages.mm("command.angle-result",
                            "n", String.valueOf(index + 1), "name", wheel.visibleRewards().get(index).name));
                } catch (NumberFormatException e) {
                    return msg(sender, Messages.mm("command.bad-number"));
                }
            }
            case "reload" -> {
                CustomItems.clearCache();
                manager.load();
                manager.spawnAll();
                Messages.reload();
                return msg(sender, Messages.mm("command.reloaded"));
            }
            case "web" -> {
                if (web == null || !web.isRunning()) {
                    return msg(sender, Messages.mm("command.web-off"));
                }
                Player player = sender instanceof Player p ? p : null;
                String token = web.createRawToken(player);
                String base = web.publicUrl();
                Component message = base.isBlank()
                        ? Messages.mm("command.web-manual", "port", String.valueOf(web.port()), "token", token)
                        : Messages.mm("command.web-link", "link", base + "#token=" + token);
                String guide = web.guideUrl();
                if (!guide.isBlank()) {
                    message = message.append(Component.newline())
                            .append(Messages.mm("command.web-guide", "guide", guide));
                }
                return msg(sender, message);
            }
            case "give" -> {
                if (args.length < 4) {
                    return msg(sender, Messages.mm("command.give-usage"));
                }
                return freeSpins(sender, args[1], args[2], args[3], true);
            }
            case "take" -> {
                if (args.length < 4) {
                    return msg(sender, Messages.mm("command.take-usage"));
                }
                return freeSpins(sender, args[1], args[2], args[3], false);
            }
            case "ticket" -> {
                if (args.length < 5
                        || !(args[1].equalsIgnoreCase("give") || args[1].equalsIgnoreCase("take"))) {
                    return msg(sender, Messages.mm("command.ticket-usage"));
                }
                return ticket(sender, args[1].equalsIgnoreCase("give"), args[2], args[3], args[4]);
            }
            case "stats" -> {
                OfflinePlayer target = args.length >= 2 ? Bukkit.getOfflinePlayer(args[1])
                        : (sender instanceof Player player ? player : null);
                if (target == null) {
                    return msg(sender, Messages.mm("command.stats-usage"));
                }
                msg(sender, Messages.mm("command.stats-header", "player",
                        target.getName() == null ? "?" : target.getName()));
                manager.ids().forEach(id -> {
                    WheelManager.PlayerStats data = manager.stats(id, target.getUniqueId());
                    msg(sender, Messages.mm("command.stats-line", "wheel", id,
                            "spins", String.valueOf(data.spins),
                            "wins", String.valueOf(data.wins),
                            "free", String.valueOf(data.freeSpins)));
                });
                return true;
            }
            case "resetcooldown" -> {
                if (args.length < 3) {
                    return msg(sender, Messages.mm("command.reset-usage"));
                }
                UUID target = Bukkit.getOfflinePlayer(args[1]).getUniqueId();
                return msg(sender, manager.resetCooldown(args[2], target)
                        ? Messages.mm("command.reset-done") : Messages.mm("command.no-wheel"));
            }
            case "preview" -> {
                WheelInstance wheel = args.length < 2 ? null : manager.get(args[1]);
                if (!(sender instanceof Player player) || wheel == null) {
                    return msg(sender, Messages.mm("command.preview-usage"));
                }
                wheel.sendPreview(player);
                return true;
            }
            case "menu" -> {
                WheelInstance wheel = args.length < 2 ? null : manager.get(args[1]);
                if (!(sender instanceof Player player) || wheel == null) {
                    return msg(sender, Messages.mm("command.menu-usage"));
                }
                if (!manager.menuEnabled()) {
                    return msg(sender, Messages.mm("command.menu-off"));
                }
                ChanceMenu.open(player, wheel);
                return true;
            }
            case "setitem" -> {
                return setItem(sender, args);
            }
            default -> {
                return usage(sender);
            }
        }
    }

    private boolean freeSpins(CommandSender sender, String playerName, String wheelId, String amountText,
            boolean give) {
        Player target = Bukkit.getPlayer(playerName);
        WheelInstance wheel = manager.get(wheelId);
        int amount;
        try {
            amount = Integer.parseInt(amountText);
        } catch (NumberFormatException e) {
            return msg(sender, Messages.mm("command.bad-number"));
        }
        if (target == null || wheel == null || amount <= 0) {
            return msg(sender, Messages.mm(give ? "command.give-usage" : "command.take-usage"));
        }
        manager.addFreeSpins(wheel.def.id, target.getUniqueId(), give ? amount : -amount);
        int left = manager.stats(wheel.def.id, target.getUniqueId()).freeSpins;
        String verb = Messages.fill(give ? "verb.added" : "verb.taken");
        msg(target, Messages.mm("command.free-added", "amount", String.valueOf(amount),
                "verb", verb, "wheel", wheel.def.id, "left", String.valueOf(left)));
        return msg(sender, Messages.mm("command.free-done", "player", target.getName(),
                "left", String.valueOf(left), "wheel", wheel.def.id));
    }

    private boolean ticket(CommandSender sender, boolean give, String playerName, String wheelId,
            String amountText) {
        Player target = Bukkit.getPlayer(playerName);
        WheelInstance wheel = manager.get(wheelId);
        int amount;
        try {
            amount = Integer.parseInt(amountText);
        } catch (NumberFormatException e) {
            return msg(sender, Messages.mm("command.bad-number"));
        }
        if (target == null || wheel == null || amount <= 0) {
            return msg(sender, Messages.mm("command.ticket-usage"));
        }
        if (wheel.def.ticketItem == null) {
            return msg(sender, Messages.mm("command.no-ticket"));
        }
        ItemStack stack = new ItemStack(wheel.def.ticketItem, amount);
        if (give) {
            Map<Integer, ItemStack> leftover = target.getInventory().addItem(stack);
            leftover.values().forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));
        } else {
            target.getInventory().removeItem(stack);
        }
        String verb = Messages.fill(give ? "verb.given" : "verb.taken");
        msg(target, Messages.mm("command.ticket-done-player", "amount", String.valueOf(amount),
                "verb", verb, "wheel", wheel.def.id));
        return msg(sender, Messages.mm("command.ticket-done"));
    }

    /** Ödülün teslim eşyasını ayarlar: eldeki eşya (birebir kopya) ya da sağlayıcı kimliği. */
    private boolean setItem(CommandSender sender, String[] args) {
        WheelInstance wheel = args.length < 3 ? null : manager.get(args[1]);
        if (!(sender instanceof Player player) || wheel == null) {
            return msg(sender, Messages.mm("command.setitem-usage"));
        }
        if (wheel.isSpinning()) {
            return msg(sender, Messages.mm("command.spinning-wait"));
        }
        int index;
        try {
            index = Integer.parseInt(args[2]) - 1;
        } catch (NumberFormatException e) {
            return msg(sender, Messages.mm("command.setitem-bad-index"));
        }
        if (index < 0 || index >= wheel.def.rewards.size()) {
            return msg(sender, Messages.mm("command.setitem-bad-index"));
        }
        Reward reward = wheel.def.rewards.get(index);
        if (args.length >= 4) {
            String spec = args[3];
            int colon = spec.indexOf(':');
            ItemSource source = colon > 0 ? ItemSource.parse(spec.substring(0, colon), null) : null;
            if (source == null || !source.isProvider() || colon >= spec.length() - 1) {
                return msg(sender, Messages.mm("command.setitem-bad-source"));
            }
            reward.itemSource = source;
            reward.providerId = spec.substring(colon + 1).trim();
            reward.itemBase64 = "";
        } else {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.getType().isAir()) {
                return msg(sender, Messages.mm("command.setitem-empty-hand"));
            }
            String base64 = CustomItems.base64(hand);
            if (base64.isBlank()) {
                return msg(sender, Messages.mm("command.setitem-failed"));
            }
            reward.itemSource = ItemSource.EXACT;
            reward.itemBase64 = base64;
            reward.providerId = "";
            reward.amount = Math.min(64, Math.max(1, hand.getAmount()));
        }
        manager.save();
        manager.respawn(wheel);
        return msg(sender, Messages.mm("command.setitem-done",
                "n", String.valueOf(index + 1), "item", wheel.payoutLabel(reward)));
    }

    private boolean set(CommandSender sender, String[] args) {
        WheelInstance wheel = args.length < 4 ? null : manager.get(args[1]);
        if (wheel == null) {
            return msg(sender, Messages.mm("command.set-usage", "keys", String.join("|", SET_KEYS)));
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
                case "weighted-slices" -> wheel.def.weightedSlices = Boolean.parseBoolean(value);
                default -> {
                    return msg(sender, Messages.mm("command.unknown-setting"));
                }
            }
        } catch (IllegalArgumentException e) {
            return msg(sender, Messages.mm("command.bad-value", "value", value));
        }
        manager.save();
        manager.respawn(wheel);
        return msg(sender, Messages.mm("command.updated"));
    }

    private boolean tune(CommandSender sender, String[] args) {
        if (args.length < 3) {
            return msg(sender, Messages.mm("command.tune-current",
                    "a", String.valueOf(manager.settings().textPixelBlocks),
                    "b", String.valueOf(manager.settings().textTopUnits),
                    "c", String.valueOf(manager.settings().textLineUnits),
                    "d", String.valueOf(manager.settings().interpolationTicks))
                    .append(Component.newline())
                    .append(Messages.mm("command.tune-usage", "keys", String.join("|", TUNE_KEYS))));
        }
        try {
            double value = Double.parseDouble(args[2]);
            switch (args[1].toLowerCase(Locale.ROOT)) {
                case "text-pixel-blocks" -> manager.settings().textPixelBlocks = value;
                case "text-top-units" -> manager.settings().textTopUnits = value;
                case "text-line-units" -> manager.settings().textLineUnits = value;
                case "interpolation-ticks" -> manager.settings().interpolationTicks = Math.max(1, (int) value);
                default -> {
                    return msg(sender, Messages.mm("command.tune-unknown"));
                }
            }
        } catch (NumberFormatException e) {
            return msg(sender, Messages.mm("command.tune-bad-number"));
        }
        manager.saveSettings();
        manager.ids().forEach(id -> manager.respawn(manager.get(id)));
        return msg(sender, Messages.mm("command.tune-done"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(SUBS, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            if (List.of("move", "remove", "spin", "set", "angle", "preview", "menu", "setitem").contains(sub)) {
                return filter(manager.ids(), args[1]);
            }
            if (List.of("give", "take", "stats", "resetcooldown").contains(sub)) {
                return null;
            }
            if (sub.equals("ticket")) {
                return filter(List.of("give", "take"), args[1]);
            }
            if (sub.equals("tune")) {
                return filter(TUNE_KEYS, args[1]);
            }
            return List.of();
        }
        if (args.length == 3) {
            if (List.of("give", "take", "resetcooldown").contains(sub)) {
                return filter(manager.ids(), args[2]);
            }
            if (sub.equals("ticket")) {
                return null;
            }
            if (sub.equals("set")) {
                return filter(SET_KEYS, args[2]);
            }
            if (sub.equals("setitem")) {
                WheelInstance wheel = manager.get(args[1]);
                if (wheel == null) {
                    return List.of();
                }
                List<String> indexes = new ArrayList<>();
                for (int i = 1; i <= wheel.def.rewards.size(); i++) {
                    indexes.add(String.valueOf(i));
                }
                return filter(indexes, args[2]);
            }
            return List.of();
        }
        if (args.length == 4) {
            if (sub.equals("ticket")) {
                return filter(manager.ids(), args[3]);
            }
            if (sub.equals("set") && args[2].equalsIgnoreCase("style")) {
                List<String> styles = new ArrayList<>();
                for (MotionStyle style : MotionStyle.values()) {
                    styles.add(style.name());
                }
                return filter(styles, args[3]);
            }
            if (sub.equals("setitem")) {
                List<String> sources = new ArrayList<>();
                for (ItemSource source : ItemSource.values()) {
                    if (source.isProvider()) {
                        sources.add(source.prefix() + ":");
                    }
                }
                return filter(sources, args[3]);
            }
            return List.of();
        }
        return List.of();
    }

    private static List<String> filter(Iterable<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        options.forEach(option -> {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(option);
            }
        });
        return result;
    }

    private static boolean usage(CommandSender sender) {
        return msg(sender, Messages.mm("command.usage-line", "subs", String.join(" | ", SUBS)));
    }

    private static boolean msg(CommandSender sender, Component message) {
        sender.sendMessage(message);
        return true;
    }
}
