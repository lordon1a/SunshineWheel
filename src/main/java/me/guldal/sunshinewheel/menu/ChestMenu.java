package me.guldal.sunshinewheel.menu;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import me.guldal.sunshinewheel.render.UiCanvas;
import me.guldal.sunshinewheel.render.UiSprite;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * Sandık penceresine dayanan özel menü.
 *
 * <p>Neden sandık: dialog ekranında butonlar ayrı bir bölgede duruyor ve arka plan onların arkasına
 * geçemiyor. Sandıkta ise tıklanan yerler sabit karelerdir, arka plan da başlık yazısına çizilen
 * resimdir; ikisi birlikte kayar, yani hizalama bozulmaz.
 *
 * <p>Ölçüler paketin kendi {@code generic_54.png} dokusundan ölçüldü: kare ızgarası (8, 18)
 * noktasından başlar, kareler 18 piksel aralıklıdır, başlık yazısı pencerenin (8, 6) noktasına çizilir.
 *
 * <p>Bu sınıf çarka özel değildir; sonraki menülerde de aynen kullanılabilir.
 */
public final class ChestMenu implements InventoryHolder {

    public static final int COLUMNS = 9;
    public static final int SLOT_ORIGIN_X = 8;
    public static final int SLOT_ORIGIN_Y = 18;
    public static final int SLOT_PITCH = 18;

    /** Başlık yazısının pencere sol üst köşesine göre konumu; resim bu kadar geri kaydırılır. */
    private static final int TITLE_OFFSET_X = -8;
    private static final int TITLE_OFFSET_Y = -6;

    private final Inventory inventory;
    private final Map<Integer, Consumer<Player>> actions = new HashMap<>();

    public ChestMenu(int rows, Component title) {
        this.inventory = Bukkit.createInventory(this, Math.max(1, Math.min(6, rows)) * COLUMNS, title);
    }

    /**
     * Menünün arka plan resmini başlıkla birleştirir.
     *
     * @param title başlığın görünen yazısı (resmin üstüne, pencerenin sol üst köşesine düşer)
     */
    public static Component background(Component title, int offsetX, int offsetY) {
        Component art = new UiCanvas(offsetX, offsetY)
                .draw(UiSprite.MENU_BG, TITLE_OFFSET_X, TITLE_OFFSET_Y)
                .build();
        return Component.empty().append(art).append(title);
    }

    /** Bir karenin sol üst köşesinin pencere içindeki piksel konumu (çizim yaparken işe yarar). */
    public static int[] slotPixel(int column, int row) {
        return new int[] {SLOT_ORIGIN_X + column * SLOT_PITCH, SLOT_ORIGIN_Y + row * SLOT_PITCH};
    }

    public static int slot(int column, int row) {
        return row * COLUMNS + column;
    }

    public ChestMenu set(int slot, ItemStack icon, Consumer<Player> action) {
        if (slot >= 0 && slot < inventory.getSize()) {
            inventory.setItem(slot, icon);
            if (action == null) {
                actions.remove(slot);
            } else {
                actions.put(slot, action);
            }
        }
        return this;
    }

    public ChestMenu set(int column, int row, ItemStack icon, Consumer<Player> action) {
        return set(slot(column, row), icon, action);
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /** @return olay bu menüye aitse true (çağıran iptal etmelidir) */
    public static boolean handleClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ChestMenu menu)) {
            return false;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == menu.inventory && event.getWhoClicked() instanceof Player player) {
            Consumer<Player> action = menu.actions.get(event.getSlot());
            if (action != null) {
                action.accept(player);
            }
        }
        return true;
    }

    /** Menüde eşya sürüklemeyi engeller. */
    public static boolean handleDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof ChestMenu)) {
            return false;
        }
        event.setCancelled(true);
        return true;
    }
}
