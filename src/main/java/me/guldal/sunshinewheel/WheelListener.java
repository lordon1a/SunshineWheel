package me.guldal.sunshinewheel;

import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import me.guldal.sunshinewheel.editor.DialogEditor;
import me.guldal.sunshinewheel.editor.EditorManager;
import me.guldal.sunshinewheel.editor.EditorSession;
import me.guldal.sunshinewheel.menu.ChestMenu;
import me.guldal.sunshinewheel.editor.WheelDialogs;
import me.guldal.sunshinewheel.wheel.WheelInstance;
import me.guldal.sunshinewheel.wheel.WheelManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;

final class WheelListener implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Plugin plugin;
    private final WheelManager manager;
    private final DialogEditor dialogs;
    private final EditorManager editors;

    WheelListener(Plugin plugin, WheelManager manager, DialogEditor dialogs, EditorManager editors) {
        this.plugin = plugin;
        this.manager = manager;
        this.dialogs = dialogs;
        this.editors = editors;
    }

    /** Çarka sağ tıklayınca çevirir. */
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
        if (!wheel.spin(event.getPlayer())) {
            event.getPlayer().sendActionBar(MM.deserialize("<red>Çark şu an dönüyor, biraz bekle."));
        }
    }

    /** Sandık menüsündeki tıklamalar; eşya alınmasını da engeller. */
    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        ChestMenu.handleClick(event);
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent event) {
        ChestMenu.handleDrag(event);
    }

    /** Editör ekranındaki butonlar. */
    @EventHandler
    public void onDialogClick(PlayerCustomClickEvent event) {
        if (!event.getIdentifier().namespace().equals(WheelDialogs.NAMESPACE)) {
            return;
        }
        if (!(event.getCommonConnection() instanceof PlayerGameConnection connection)) {
            return;
        }
        Player player = connection.getPlayer();
        String path = event.getIdentifier().value();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            dialogs.handle(player, path, event.getDialogResponseView());
            EditorSession session = editors.session(player.getUniqueId());
            if (session != null) {
                session.redraw();
            }
        });
    }

    /** Editör paneline tıklama ve yerleştirme modundaki sağ tık. */
    @EventHandler(priority = EventPriority.LOW)
    public void onPlace(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        EditorSession session = editors.session(player.getUniqueId());
        if (session != null) {
            event.setCancelled(true);
            session.click();
            return;
        }
        WheelManager.Placement placement = manager.placement(player.getUniqueId());
        if (placement == null) {
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_AIR) {
            player.sendActionBar(MM.deserialize("<gold>Çarkı koymak için bir bloğa sağ tıkla <gray>(iptal: /wheel cancel)"));
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        event.setCancelled(true);
        WheelInstance wheel = manager.place(player, event.getClickedBlock(), event.getBlockFace());
        if (wheel == null) {
            player.sendMessage(MM.deserialize("<red>Çark buraya konamadı (çark dönüyor olabilir ya da bu id artık kullanılıyor)."));
        } else {
            player.sendMessage(MM.deserialize("<green>Çark yerleştirildi: <white>" + wheel.def.id
                    + "</white>. <gray>Düzenlemek için /wheel edit " + wheel.def.id));
        }
    }

    /** Shift editörü kapatır (panelde klavye kısayolu okuyamıyoruz). */
    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking() && editors.close(event.getPlayer().getUniqueId())) {
            event.getPlayer().sendActionBar(MM.deserialize("<gray>Editör kapatıldı."));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        editors.close(event.getPlayer().getUniqueId());
        dialogs.forget(event.getPlayer().getUniqueId());
        manager.cancelPlacement(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        // Kalıcı olmayan parçalar chunk ile birlikte kaybolur; chunk yüklenince bir tick sonra yeniden kur.
        plugin.getServer().getScheduler().runTask(plugin, () -> manager.onChunkLoad(event.getChunk()));
    }
}
