package me.guldal.sunshinewheel.editor;

import me.guldal.sunshinewheel.render.PixelText;
import me.guldal.sunshinewheel.wheel.RenderSettings;
import me.guldal.sunshinewheel.wheel.WheelInstance;
import me.guldal.sunshinewheel.wheel.WheelManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Bir oyuncunun açık editör paneli.
 *
 * <p>Panel, oyuncunun önünde duran ve ona dönük tek bir TextDisplay'dir (billboard CENTER).
 * İmleç, oyuncunun bakış ışınının panel düzlemiyle kesişiminden bulunur ve oyuncunun kendi ekran
 * eksenlerine göre piksele çevrilir; panel de oyuncuya döndüğü için ikisi her zaman uyumludur.
 * Yani fareyi oynattıkça imleç panelde gezer, tıklayınca buton çalışır.
 */
public final class EditorSession {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Plugin plugin;
    private final WheelManager wheels;
    private final Player player;
    private final WheelInstance wheel;
    private final RenderSettings settings;
    private final EditorPanel panel;
    private final Location anchor;

    private TextDisplay display;
    private TextDisplay previewDisplay;
    private BukkitTask task;
    private int hovered = -1;
    private int cursorX = -1;
    private int cursorY = -1;

    public EditorSession(Plugin plugin, WheelManager wheels, Player player, WheelInstance wheel, Runnable onTextTab) {
        this.plugin = plugin;
        this.wheels = wheels;
        this.player = player;
        this.wheel = wheel;
        this.settings = wheels.settings();
        this.panel = new EditorPanel(wheel.def, this::applyChange, onTextTab, this::spin, this::close);

        Location eye = player.getEyeLocation();
        Vector forward = eye.getDirection().setY(0);
        if (forward.lengthSquared() < 1.0e-4) {
            forward = new Vector(0, 0, 1);
        }
        this.anchor = eye.clone().add(forward.normalize().multiply(settings.panelDistance));
    }

    public WheelInstance wheel() {
        return wheel;
    }

    public void open() {
        display = anchor.getWorld().spawn(anchor, TextDisplay.class, td -> {
            td.setPersistent(false);
            td.setBillboard(Display.Billboard.CENTER);
            td.setLineWidth(Integer.MAX_VALUE / 2);
            td.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            td.setShadowed(false);
            td.setSeeThrough(false);
            td.setBrightness(new Display.Brightness(15, 15));
            td.setViewRange(1.0f);
        });
        // Canlı önizleme ayrı bir display'dir: sadece ödüller/renkler değişince yeniden kodlanır.
        float scale = (float) (settings.panelPixelBlocks / settings.textPixelBlocks);
        previewDisplay = anchor.getWorld().spawn(anchor, TextDisplay.class, td -> {
            td.setPersistent(false);
            td.setBillboard(Display.Billboard.CENTER);
            td.setLineWidth(Integer.MAX_VALUE / 2);
            td.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            td.setShadowed(false);
            td.setSeeThrough(false);
            td.setBrightness(new Display.Brightness(15, 15));
            td.setViewRange(1.0f);
            td.text(PixelText.pad(panel.previewComponent(), settings.padLines(EditorPanel.PREVIEW_IMG_SIZE)));
            td.setTransformation(new Transformation(new Vector3f(), new Quaternionf(),
                    new Vector3f(scale), new Quaternionf()));
        });
        redraw();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 2L, 2L);
        player.sendMessage(MM.deserialize("<gold>Editör açık.</gold> <gray>Panele bak, imleci fareyle gezdir, "
                + "sol tıkla bas. Tekerlek = kaydırıcı, F = kapat."));
    }

    private void tick() {
        if (display == null || !display.isValid() || !player.isOnline()
                || !player.getWorld().equals(anchor.getWorld())
                || player.getLocation().distanceSquared(anchor) > 400) {
            close();
            return;
        }
        positionPreview();
        int[] cursor = cursor();
        int x = cursor == null ? -1 : cursor[0];
        int y = cursor == null ? -1 : cursor[1];
        int index = cursor == null ? -1 : panel.hitTest(x, y);
        // İmleç her tick birkaç piksel oynadığı için sadece belirgin değişimde yeniden çiziyoruz.
        if (index != hovered || Math.abs(x - cursorX) > 1 || Math.abs(y - cursorY) > 1) {
            hovered = index;
            cursorX = x;
            cursorY = y;
            redraw();
        }
    }

    /** Bakış ışınının panel düzlemiyle kesiştiği noktanın panel içindeki piksel koordinatı. */
    private int[] cursor() {
        Location eye = player.getEyeLocation();
        Vector origin = eye.toVector();
        Vector look = eye.getDirection();
        Vector toPanel = anchor.toVector().subtract(origin);
        double distance = toPanel.length();
        if (distance < 0.1) {
            return null;
        }
        Vector normal = toPanel.clone().multiply(1.0 / distance);
        double denominator = look.dot(normal);
        if (denominator <= 0.05) {
            return null;
        }
        Vector hit = origin.clone().add(look.clone().multiply(toPanel.dot(normal) / denominator));
        Vector right = look.getCrossProduct(new Vector(0, 1, 0));
        if (right.lengthSquared() < 1.0e-6) {
            return null;
        }
        right.normalize();
        Vector up = right.getCrossProduct(look).normalize();
        Vector delta = hit.subtract(anchor.toVector());
        int px = (int) Math.round(delta.dot(right) / settings.panelPixelBlocks + EditorPanel.IMG_W / 2.0);
        int py = (int) Math.round(EditorPanel.IMG_H / 2.0 - delta.dot(up) / settings.panelPixelBlocks);
        return new int[] {px, py};
    }

    /** Sol tık: imlecin üstündeki butona basar. @return panele denk geldiyse true */
    public boolean click() {
        int[] cursor = cursor();
        int index = cursor == null ? -1 : panel.hitTest(cursor[0], cursor[1]);
        EditorPanel.Button button = panel.button(index);
        if (button == null) {
            return cursor != null && cursor[0] >= 0 && cursor[0] < EditorPanel.IMG_W
                    && cursor[1] >= 0 && cursor[1] < EditorPanel.IMG_H;
        }
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.4f);
        if (index == 10) {
            panel.sliderToCursor(cursor[0]);   // kaydırıcı: tıklanan yere atlar
        } else {
            button.run();
        }
        hovered = index;
        redraw();
        return true;
    }

    private void spin(Player ignored) {
        spin();
    }

    private void spin() {
        if (!wheel.spin(player)) {
            player.sendActionBar(MM.deserialize("<red>Çark şu an dönüyor ya da ödülü yok."));
        }
    }

    private void applyChange() {
        wheels.save();
        wheels.respawn(wheel);
    }

    /**
     * Önizleme display'ini panelin sağ kutusuna konumlandırır. Panel oyuncuya döndüğü için kaydırma
     * yönü her tick oyuncunun sağ vektöründen yeniden hesaplanır (paket maliyeti ~50 byte).
     */
    private void positionPreview() {
        if (previewDisplay == null || !previewDisplay.isValid()) {
            return;
        }
        org.bukkit.util.Vector look = player.getEyeLocation().getDirection();
        org.bukkit.util.Vector right = look.getCrossProduct(new org.bukkit.util.Vector(0, 1, 0));
        if (right.lengthSquared() < 1.0e-6) {
            right = new org.bukkit.util.Vector(1, 0, 0);
        } else {
            right.normalize();
        }
        double px = settings.panelPixelBlocks;
        double dx = (EditorPanel.PREVIEW_CENTER_X - EditorPanel.IMG_W / 2.0) * px;
        double dy = (EditorPanel.PREVIEW_CENTER_Y - EditorPanel.IMG_H / 2.0) * px;
        float scale = (float) (px / settings.textPixelBlocks);
        previewDisplay.setTransformation(new Transformation(
                new Vector3f((float) (right.getX() * dx + look.getX() * 0.02),
                        (float) (-dy + right.getY() * dx + look.getY() * 0.02),
                        (float) (right.getZ() * dx + look.getZ() * 0.02)),
                new Quaternionf(), new Vector3f(scale), new Quaternionf()));
    }

    public void redraw() {
        if (display == null || !display.isValid()) {
            return;
        }
        float scale = (float) (settings.panelPixelBlocks / settings.textPixelBlocks);
        double centerY = settings.imageCenterY(EditorPanel.IMG_H, settings.panelPixelBlocks);
        display.text(PixelText.pad(panel.render(hovered, cursorX, cursorY), settings.padLines(EditorPanel.IMG_H)));
        display.setTransformation(new Transformation(
                new Vector3f(0, (float) -centerY, 0), new Quaternionf(), new Vector3f(scale), new Quaternionf()));
    }

    /** Fare tekerleği: kaydırıcı ince ayarı. Panele denk geldiyse true döner (hotbar değişmesin). */
    public boolean scroll(int direction) {
        int[] cursor = cursor();
        if (cursor == null) {
            return false;
        }
        panel.scroll(cursor[0], cursor[1], direction);
        redraw();
        return true;
    }

    public void close() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (display != null) {
            display.remove();
            display = null;
        }
        if (previewDisplay != null) {
            previewDisplay.remove();
            previewDisplay = null;
        }
    }
}
