package me.guldal.sunshinewheel.wheel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import me.guldal.sunshinewheel.Messages;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * Sol tıkla açılan şans menüsü: çarktaki ödüller ikon + şans yüzdesi + teslimat özetiyle listelenir.
 * Menü sayfalar arası gezinir; tıklamalar dinleyici tarafından iptal edilir.
 */
public final class ChanceMenu implements InventoryHolder {

    private static final int ROWS = 6;
    private static final int PAGE_SIZE = 45;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_PAGE = 49;
    private static final int SLOT_NEXT = 53;
    private static final int SLOT_EMPTY = 22;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final WheelInstance wheel;
    private final Player viewer;
    private final Inventory inventory;
    private int page;

    private ChanceMenu(WheelInstance wheel, Player viewer) {
        this.wheel = wheel;
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, ROWS * 9,
                Messages.mm("menu.title", "wheel", stripTags(wheel.def.name)));
        render();
    }

    /** Menüyü oyuncuya açar. */
    public static void open(Player viewer, WheelInstance wheel) {
        if (viewer == null || wheel == null || !viewer.isOnline()) {
            return;
        }
        viewer.openInventory(new ChanceMenu(wheel, viewer).inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public WheelInstance wheel() {
        return wheel;
    }

    /** Menü içi tıklama; yalnızca sayfa düğmeleri iş yapar. */
    public void handleClick(int rawSlot) {
        int pages = pageCount();
        if (rawSlot == SLOT_PREV && page > 0) {
            page--;
            render();
        } else if (rawSlot == SLOT_NEXT && page + 1 < pages) {
            page++;
            render();
        }
    }

    private int pageCount() {
        return Math.max(1, (wheel.visibleRewards().size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private void render() {
        inventory.clear();
        List<Reward> visible = wheel.visibleRewards();
        if (visible.isEmpty()) {
            inventory.setItem(SLOT_EMPTY, named(Material.BARRIER, "menu.empty"));
            return;
        }
        int pages = pageCount();
        page = Math.max(0, Math.min(page, pages - 1));
        int start = page * PAGE_SIZE;
        int end = Math.min(visible.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            inventory.setItem(i - start, itemFor(visible.get(i)));
        }
        if (pages > 1) {
            inventory.setItem(SLOT_PREV, named(Material.ARROW, "menu.prev"));
            inventory.setItem(SLOT_NEXT, named(Material.ARROW, "menu.next"));
            inventory.setItem(SLOT_PAGE, named(Material.PAPER, "menu.page",
                    "page", String.valueOf(page + 1), "pages", String.valueOf(pages)));
        }
    }

    private ItemStack itemFor(Reward reward) {
        ItemStack item = wheel.displayItem(reward);
        double chance = wheel.chanceFor(viewer, reward);
        item.editMeta(meta -> {
            meta.displayName(MM.deserialize(
                    wheel.resolveRewardText(reward.name, viewer, reward, chance)));
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            if (!wheel.canWin(viewer, reward)) {
                lore.add(Messages.mm("menu.locked", "permission", reward.permission));
            } else {
                lore.add(Messages.mm("menu.chance", "chance", String.format(Locale.ROOT, "%.2f%%", chance)));
            }
            lore.add(Messages.mm("menu.weight", "weight", formatWeight(reward.weight)));
            if (reward.hasNoPayout()) {
                lore.add(Messages.mm("menu.payout-none"));
            } else {
                if (reward.giveItem) {
                    lore.add(Messages.mm("menu.payout-item", "item", wheel.payoutLabel(reward)));
                }
                if (!reward.commands.isEmpty()) {
                    lore.add(Messages.mm("menu.payout-commands", "count", String.valueOf(reward.commands.size())));
                }
            }
            meta.lore(lore);
        });
        return item;
    }

    private static ItemStack named(Material material, String key, String... placeholders) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> meta.displayName(Messages.mm(key, placeholders)));
        return item;
    }

    private static String formatWeight(double weight) {
        if (weight == Math.rint(weight)) {
            return String.valueOf((long) weight);
        }
        return String.format(Locale.ROOT, "%.2f", weight);
    }

    private static String stripTags(String text) {
        return text == null ? "" : text.replaceAll("<[^>]+>", "");
    }
}
