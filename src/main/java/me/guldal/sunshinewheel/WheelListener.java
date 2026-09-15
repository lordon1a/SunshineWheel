package me.guldal.sunshinewheel;

import me.guldal.sunshinewheel.wheel.WheelInstance;
import me.guldal.sunshinewheel.wheel.WheelManager;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;

final class WheelListener implements Listener {

    private final Plugin plugin;
    private final WheelManager manager;

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
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        // Kalıcı olmayan parçalar chunk ile birlikte kaybolur; chunk yüklenince bir tick sonra yeniden kur.
        plugin.getServer().getScheduler().runTask(plugin, () -> manager.onChunkLoad(event.getChunk()));
    }
}
