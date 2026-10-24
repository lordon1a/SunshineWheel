package me.guldal.sunshinewheel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import me.guldal.sunshinewheel.wheel.ChanceMenu;
import me.guldal.sunshinewheel.wheel.WheelInstance;
import me.guldal.sunshinewheel.wheel.WheelManager;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;

final class WheelListener implements Listener {

    /** Sol tık menüsü iki olaydan da gelebilir; çift açılışı önlemek için kısa debounce. */
    private static final long MENU_DEBOUNCE_MS = 250;

    private final Plugin plugin;
    private final WheelManager manager;
    private final Map<UUID, Long> menuOpened = new HashMap<>();

    WheelListener(Plugin plugin, WheelManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    /** Çarka sağ tıklayınca çevirir; eğilerek tıklayınca ödül önizlemesi gösterir. */
    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !(event.getRightClicked() instanceof Interaction)) {
            return;
        }
        WheelInstance wheel = manager.byHitbox(event.getRightClicked().getUniqueId());
        if (wheel == null) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (player.isSneaking()) {
            wheel.sendPreview(player);
            return;
        }
        if (!wheel.spin(player)) {
            player.sendActionBar(Messages.mm("spin.busy"));
        }
    }

    /** Sol tık: şans menüsü. Interaction hitbox'ında damage olayı çoğu sürümde tetiklenmez. */
    @EventHandler
    public void onAttack(PrePlayerAttackEntityEvent event) {
        if (!(event.getAttacked() instanceof Interaction interaction)) {
            return;
        }
        WheelInstance wheel = manager.byHitbox(interaction.getUniqueId());
        if (wheel == null) {
            return;
        }
        event.setCancelled(true);
        openMenu(event.getPlayer(), wheel);
    }

    /** Yedek yol: damage olayı tetiklenen sürümlerde sol tık buradan yakalanır. */
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Interaction interaction)
                || !(event.getDamager() instanceof Player player)) {
            return;
        }
        WheelInstance wheel = manager.byHitbox(interaction.getUniqueId());
        if (wheel == null) {
            return;
        }
        event.setCancelled(true);
        openMenu(player, wheel);
    }

    private void openMenu(Player player, WheelInstance wheel) {
        if (!manager.menuEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = menuOpened.get(player.getUniqueId());
        if (last != null && now - last < MENU_DEBOUNCE_MS) {
            return;
        }
        menuOpened.put(player.getUniqueId(), now);
        ChanceMenu.open(player, wheel);
    }

    /** Menüde eşya alınmasın/taşınmasın; yalnızca sayfa düğmeleri çalışır. */
    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof ChanceMenu menu)) {
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot >= 0 && slot < event.getView().getTopInventory().getSize()) {
            menu.handleClick(slot);
        }
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof ChanceMenu) {
            event.setCancelled(true);
        }
    }

    /** Yerleştirme modundaki sağ tık. */
    @EventHandler
    public void onPlace(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        WheelManager.Placement placement = manager.placement(player.getUniqueId());
        if (placement == null) {
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_AIR) {
            player.sendActionBar(Messages.mm("place.hint"));
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        event.setCancelled(true);
        WheelInstance wheel = manager.place(player, event.getClickedBlock(), event.getBlockFace());
        if (wheel == null) {
            player.sendMessage(Messages.mm("place.fail"));
        } else {
            player.sendMessage(Messages.mm("place.done", "id", wheel.def.id));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.cancelPlacement(event.getPlayer().getUniqueId());
        menuOpened.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        // Kalıcı olmayan parçalar chunk ile birlikte kaybolur; chunk yüklenince bir tick sonra yeniden kur.
        plugin.getServer().getScheduler().runTask(plugin, () -> manager.onChunkLoad(event.getChunk()));
    }
}
