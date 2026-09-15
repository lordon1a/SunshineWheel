package me.guldal.sunshinewheel.editor;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.body.PlainMessageDialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import me.guldal.sunshinewheel.render.Glyphs;
import me.guldal.sunshinewheel.render.PixelImage;
import me.guldal.sunshinewheel.render.PixelText;
import me.guldal.sunshinewheel.render.UiCanvas;
import me.guldal.sunshinewheel.render.WheelArt;
import me.guldal.sunshinewheel.wheel.RenderSettings;
import me.guldal.sunshinewheel.wheel.Effects;
import me.guldal.sunshinewheel.wheel.MotionStyle;
import me.guldal.sunshinewheel.wheel.Reward;
import me.guldal.sunshinewheel.wheel.WheelDefinition;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Editör ekranları. Sam's Wheel'deki sol sekme sütununun karşılığı, Dialog API'de ana menüdeki
 * buton ızgarasıdır; her sekme kendi ekranında gerçek yazı kutuları ve kaydırıcılarla açılır.
 *
 * <p>Butonlara verilen anahtarlar {@code sunshinewheel:...} biçimindedir ve
 * {@link DialogEditor} tarafından yakalanır. Bir butona basıldığında o ekrandaki tüm giriş
 * değerleri sunucuya gelir, yani her tıklama aynı zamanda kaydeder.
 */
public final class WheelDialogs {

    public static final String NAMESPACE = "sunshinewheel";
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final TextColor GOLD = TextColor.color(0xF2B544);
    private static final int W = 320;

    /** Ana menüdeki sekmeler: anahtar ve başlık. */
    public static final List<String[]> TABS = List.of(
            new String[] {"general", "Genel"},
            new String[] {"location", "Konum"},
            new String[] {"rewards", "Ödüller"},
            new String[] {"animation", "Animasyon"},
            new String[] {"cost", "Maliyet & Bilet"},
            new String[] {"cooldown", "Bekleme Süresi"},
            new String[] {"appearance", "Görünüm"},
            new String[] {"items", "Ödül Eşyaları"},
            new String[] {"holograms", "Hologramlar"},
            new String[] {"sounds", "Sesler"},
            new String[] {"particles", "Parçacıklar"});

    /** Ekran içindeki canlı önizleme panelinin ölçüleri ve yazı satır yüksekliği (GUI pikseli). */
    private static final int PREVIEW_SIZE = 80;
    private static final int PREVIEW_PANEL_W = 148;
    private static final int GUI_LINE_HEIGHT = 9;
    private static final int PANEL_BG = 0x16112A;
    private static final int PANEL_DIM = 0x9C93B8;

    private WheelDialogs() {
    }

    /**
     * Ekranın arkasına çizilen panel. İlk gövde elemanı olarak konur; font glifleri satırın altına
     * taştığı için panel, altındaki gerçek kutuların ve butonların arkasına geçer.
     */
    private static DialogBody background(RenderSettings settings) {
        Component art = new UiCanvas(settings.uiOffsetX, settings.uiOffsetY).drawPanel(0).build();
        return DialogBody.plainMessage(art, W);
    }

    /**
     * Çarkın o anki hâlini ekranın içine resim olarak çizer.
     *
     * <p>Kaynak paketindeki px fontu sayesinde resim aslında tek satırlık bir metindir; satırlar
     * aşağı doğru taştığı için altına boş satır eklenerek yer ayrılır.
     */
    private static DialogBody preview(WheelDefinition def) {
        List<Integer> colors = def.rewards.stream().map(r -> r.color).toList();
        PixelImage wheel = WheelArt.wheel(colors, PREVIEW_SIZE);

        int height = PREVIEW_SIZE + 34;
        PixelImage panel = new PixelImage(PREVIEW_PANEL_W, height);
        panel.fillRect(0, 0, PREVIEW_PANEL_W, height, 0xFF000000 | PANEL_BG);
        panel.border(0, 0, PREVIEW_PANEL_W, height, 0xFF000000 | 0xF2B544);
        // köşeleri yuvarlak göstermek için uç pikselleri boşalt
        panel.set(0, 0, 0);
        panel.set(PREVIEW_PANEL_W - 1, 0, 0);
        panel.set(0, height - 1, 0);
        panel.set(PREVIEW_PANEL_W - 1, height - 1, 0);

        Glyphs.drawCentered(panel, PREVIEW_PANEL_W / 2, 5, "CANLI ONIZLEME", 0xF2B544, 1);
        panel.blit(wheel, (PREVIEW_PANEL_W - PREVIEW_SIZE) / 2, 16);
        Glyphs.drawCentered(panel, PREVIEW_PANEL_W / 2, height - 11, def.rewards.size() + " ODUL", PANEL_DIM, 1);

        // Genişlik diğer yazılarla aynı (W) olmalı, yoksa ekran o genişliğe göre yayılıp butonları taşırır.
        // Resim net genişliği sıfır olacak şekilde kodlanır, böylece ortada çizilir.
        int padLines = (int) Math.ceil((double) height / GUI_LINE_HEIGHT);
        return DialogBody.plainMessage(PixelText.encode(panel, padLines, true), W);
    }

    private static Key key(String path) {
        return Key.key(NAMESPACE, path);
    }

    private static ActionButton button(String label, String tooltip, String path) {
        return ActionButton.create(Component.text(label, GOLD),
                tooltip == null ? null : Component.text(tooltip),
                140, DialogAction.customClick(key(path), null));
    }

    private static ActionButton plainButton(String label, String path) {
        return ActionButton.create(Component.text(label), null, 140, DialogAction.customClick(key(path), null));
    }

    /** Ana menü: tüm sekmeler + test çevirme. */
    public static Dialog menu(WheelDefinition def, RenderSettings settings) {
        List<ActionButton> buttons = new ArrayList<>();
        for (String[] tab : TABS) {
            buttons.add(button(tab[1], null, "open/" + tab[0]));
        }
        buttons.add(plainButton("▶ Test Çevir", "spin"));
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("SunshineWheel — " + def.id, GOLD))
                        // Ad ve özet önizleme kutusunda zaten görünüyor; ekranı kısa tutmak için
                        // tekrar yazılmıyor (uzun içerik kaydırma çubuğu çıkarıp paneli kaydırıyor).
                        .body(List.of(background(settings), preview(def)))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(buttons, plainButton("Kapat", "close"), 3)));
    }

    /** Bir sekmenin ekranı. */
    public static Dialog tab(WheelDefinition def, String tab, RenderSettings settings) {
        List<DialogInput> inputs = new ArrayList<>();
        List<ActionButton> buttons = new ArrayList<>();
        List<DialogBody> body = new ArrayList<>();
        String title = label(tab);
        body.add(background(settings));
        body.add(heading(tab));

        switch (tab) {
            case "general" -> {
                body.add(info("Çarkın adı ve altındaki açıklama. MiniMessage renk etiketleri kullanılabilir."));
                inputs.add(text("name", "Ad", def.name, 128));
                inputs.add(text("subtitle", "Alt yazı", def.subtitle, 128));
            }
            case "location" -> {
                body.add(info("Dünya: " + def.world + "  X: " + round(def.x) + "  Y: " + round(def.y)
                        + "  Z: " + round(def.z)));
                body.add(info("Taşımak için aşağıdaki butona bas, sonra çarkın duracağı bloğa sağ tıkla."));
                buttons.add(button("Taşı", "Ekran kapanır, bir bloğa sağ tıkla", "move"));
            }
            case "rewards" -> {
                double total = def.rewards.stream().mapToDouble(r -> Math.max(0, r.weight)).sum();
                body.add(info("Dilim renkleri ve sırası bu listeden gelir. Yüzdeler ağırlıklara göre hesaplanır."));
                for (int i = 0; i < def.rewards.size(); i++) {
                    Reward reward = def.rewards.get(i);
                    String percent = total <= 0 ? "-" : Math.round(reward.weight / total * 100) + "%";
                    body.add(rewardRow(reward, percent));
                    buttons.add(button(strip(reward.name) + "  (" + percent + ")", "Düzenle", "reward/open/" + i));
                }
                buttons.add(plainButton("+ Ödül Ekle", "reward/add"));
            }
            case "animation" -> {
                body.add(info("Dönüşün uzunluğu, kaç tur atacağı ve hız eğrisi."));
                inputs.add(number("duration", "Süre (tick)", 20, 600, def.duration, 5));
                inputs.add(number("revolutions", "Tur sayısı", 1, 30, (float) def.revolutions, 0.5f));
                inputs.add(styleInput(def.style));
            }
            case "cost" -> {
                body.add(info("Bilet eşyası envanterden alınır. Komutlar çevirmeden önce konsolda çalışır; "
                        + "%player% oyuncu adıyla değişir (ör. eco take %player% 500)."));
                inputs.add(text("ticket", "Bilet eşyası (boş = yok)", def.ticketItem == null ? "" : def.ticketItem.name(), 64));
                inputs.add(number("ticket_amount", "Bilet adedi", 1, 64, def.ticketAmount, 1));
                inputs.add(text("cost_commands", "Ücret komutları (; ile ayır)", String.join(";", def.costCommands), 512));
            }
            case "cooldown" -> {
                body.add(info("Aynı oyuncunun tekrar çevirebilmesi için geçmesi gereken süre. 0 = kapalı."));
                inputs.add(number("cooldown", "Bekleme (saniye)", 0, 3600, def.cooldownSeconds, 5));
            }
            case "appearance" -> {
                body.add(info("Piksel boyu çarkın büyüklüğünü, görüntü boyutu ise çözünürlüğünü belirler."));
                inputs.add(number("pixel", "Piksel boyu (blok)", 0.005f, 0.2f, (float) def.pixelSize, 0.005f));
                inputs.add(number("image", "Görüntü boyutu (piksel)", 32, 256, def.imageSize, 8));
            }
            case "items" -> {
                body.add(info("Dilimlerin üstündeki ödül eşyaları."));
                inputs.add(DialogInput.bool("upright", Component.text("Eşyalar dik dursun"), def.itemsUpright, "true", "false"));
                inputs.add(number("item_scale", "Eşya boyutu", 0.05f, 2f, (float) def.itemScale, 0.05f));
            }
            case "holograms" -> {
                body.add(info("Çarkın üstündeki yazı."));
                inputs.add(DialogInput.bool("show", Component.text("Hologram görünsün"), def.showHologram, "true", "false"));
                inputs.add(number("offset", "Yükseklik (blok)", -2f, 4f, (float) def.hologramOffset, 0.05f));
            }
            case "sounds" -> {
                body.add(info("Biçim: anahtar ses_düzeyi perde. Kapatmak için boş bırak ya da 'yok' yaz."));
                inputs.add(text("s_start", "Başlangıç", def.soundStart, 128));
                inputs.add(text("s_tick", "Dilim geçişi", def.soundTick, 128));
                inputs.add(text("s_land", "Durma", def.soundLanding, 128));
                inputs.add(text("s_win", "Kazanma", def.soundWin, 128));
            }
            case "particles" -> {
                body.add(info("Kazanınca çıkan parçacık. Örnek: TOTEM_OF_UNDYING, HAPPY_VILLAGER, FLAME."));
                inputs.add(text("p_win", "Parçacık", def.particleWin, 64));
                inputs.add(number("p_count", "Adet", 0, 300, def.particleCount, 10));
            }
            default -> body.add(info("Bilinmeyen sekme."));
        }

        buttons.add(button("Kaydet & uygula", null, "apply/" + tab + "/same"));
        buttons.add(plainButton("← Menü", "apply/" + tab + "/menu"));

        List<DialogInput> finalInputs = inputs;
        List<DialogBody> finalBody = body;
        List<ActionButton> finalButtons = buttons;
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(title + " — " + def.id, GOLD))
                        .body(finalBody)
                        .inputs(finalInputs)
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(finalButtons, plainButton("Kapat", "apply/" + tab + "/close"), 2)));
    }

    /** Tek bir ödülün düzenleme ekranı. */
    public static Dialog reward(WheelDefinition def, int index, RenderSettings settings) {
        Reward reward = def.rewards.get(index);
        List<DialogInput> inputs = List.of(
                text("r_name", "Ad", reward.name, 128),
                number("r_weight", "Ağırlık (şans)", 0, 100, (float) reward.weight, 1),
                text("r_color", "Dilim rengi (#RRGGBB)", String.format("#%06X", reward.color & 0xFFFFFF), 16),
                text("r_icon", "İkon eşyası", reward.icon.name(), 64),
                text("r_commands", "Komutlar (; ile ayır)", String.join(";", reward.commands), 512));

        List<ActionButton> buttons = List.of(
                button("Kaydet", null, "reward/apply/" + index + "/same"),
                plainButton("← Ödüller", "reward/apply/" + index + "/list"),
                ActionButton.create(Component.text("Sil", NamedTextColor.RED), Component.text("Bu ödülü siler"),
                        140, DialogAction.customClick(key("reward/delete/" + index), null)));

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Ödül " + (index + 1) + " — " + def.id, GOLD))
                        .body(List.of(background(settings),
                                info("Ağırlık, diğer ödüllere göre çıkma şansını belirler.")))
                        .inputs(inputs)
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(buttons, plainButton("Kapat", "close"), 2)));
    }

    // --- giriş değerlerinin uygulanması ---

    public static void applyTab(WheelDefinition def, String tab, DialogResponseView view) {
        if (view == null) {
            return;
        }
        switch (tab) {
            case "general" -> {
                def.name = textOr(view, "name", def.name);
                def.subtitle = textOr(view, "subtitle", def.subtitle);
            }
            case "animation" -> {
                def.duration = (int) floatOr(view, "duration", def.duration);
                def.revolutions = floatOr(view, "revolutions", (float) def.revolutions);
                String style = view.getText("style");
                if (style != null) {
                    try {
                        def.style = MotionStyle.valueOf(style);
                    } catch (IllegalArgumentException ignored) {
                        // seçim listesi dışında bir değer geldiyse mevcut stil kalır
                    }
                }
            }
            case "cost" -> {
                String ticket = textOr(view, "ticket", "");
                def.ticketItem = ticket.isBlank() ? null : Material.matchMaterial(ticket.trim().toUpperCase(Locale.ROOT));
                def.ticketAmount = Math.max(1, (int) floatOr(view, "ticket_amount", def.ticketAmount));
                def.costCommands.clear();
                def.costCommands.addAll(split(textOr(view, "cost_commands", "")));
            }
            case "cooldown" -> def.cooldownSeconds = Math.max(0, (int) floatOr(view, "cooldown", def.cooldownSeconds));
            case "appearance" -> {
                def.pixelSize = floatOr(view, "pixel", (float) def.pixelSize);
                def.imageSize = WheelDefinition.clamp((int) floatOr(view, "image", def.imageSize), 32, 256);
            }
            case "items" -> {
                Boolean upright = view.getBoolean("upright");
                if (upright != null) {
                    def.itemsUpright = upright;
                }
                def.itemScale = floatOr(view, "item_scale", (float) def.itemScale);
            }
            case "holograms" -> {
                Boolean show = view.getBoolean("show");
                if (show != null) {
                    def.showHologram = show;
                }
                def.hologramOffset = floatOr(view, "offset", (float) def.hologramOffset);
            }
            case "sounds" -> {
                def.soundStart = textOr(view, "s_start", def.soundStart);
                def.soundTick = textOr(view, "s_tick", def.soundTick);
                def.soundLanding = textOr(view, "s_land", def.soundLanding);
                def.soundWin = textOr(view, "s_win", def.soundWin);
            }
            case "particles" -> {
                def.particleWin = textOr(view, "p_win", def.particleWin);
                def.particleCount = Math.max(0, (int) floatOr(view, "p_count", def.particleCount));
            }
            default -> {
                // location ve rewards sekmelerinde giriş yok
            }
        }
    }

    public static void applyReward(WheelDefinition def, int index, DialogResponseView view) {
        if (view == null || index < 0 || index >= def.rewards.size()) {
            return;
        }
        Reward reward = def.rewards.get(index);
        reward.name = textOr(view, "r_name", reward.name);
        reward.weight = floatOr(view, "r_weight", (float) reward.weight);
        reward.color = Reward.parseColor(textOr(view, "r_color", "#808080"));
        Material icon = Material.matchMaterial(textOr(view, "r_icon", reward.icon.name()).trim().toUpperCase(Locale.ROOT));
        if (icon != null && icon.isItem()) {
            reward.icon = icon;
        }
        reward.commands.clear();
        reward.commands.addAll(split(textOr(view, "r_commands", "")));
    }

    public static String label(String tab) {
        for (String[] entry : TABS) {
            if (entry[0].equals(tab)) {
                return entry[1];
            }
        }
        return tab;
    }

    // --- yardımcılar ---

    private static DialogBody info(String text) {
        return DialogBody.plainMessage(Component.text(text, NamedTextColor.GRAY), W);
    }

    /** Bölüm başlığı: altın renkli, sekme adıyla. */
    private static DialogBody heading(String tab) {
        return DialogBody.plainMessage(Component.text("▸ " + label(tab).toUpperCase(Locale.ROOT), GOLD), W);
    }

    /** Ödül satırı: eşya ikonu + adı ve yüzdesi. */
    private static DialogBody rewardRow(Reward reward, String percent) {
        PlainMessageDialogBody description = DialogBody.plainMessage(
                MM.deserialize(reward.name).append(Component.text("  " + percent, NamedTextColor.GRAY)), W - 40);
        return DialogBody.item(new ItemStack(reward.icon), description, true, true, 16, 16);
    }

    private static DialogInput text(String key, String label, String initial, int maxLength) {
        return DialogInput.text(key, W, Component.text(label), true, initial == null ? "" : initial, maxLength, null);
    }

    private static DialogInput number(String key, String label, float min, float max, float initial, float step) {
        return DialogInput.numberRange(key, W, Component.text(label), "%s: %s", min, max,
                Math.max(min, Math.min(max, initial)), step);
    }

    private static DialogInput styleInput(MotionStyle current) {
        List<SingleOptionDialogInput.OptionEntry> entries = new ArrayList<>();
        Arrays.stream(MotionStyle.values()).forEach(style -> entries.add(
                SingleOptionDialogInput.OptionEntry.create(style.name(), Component.text(style.label()), style == current)));
        return DialogInput.singleOption("style", W, entries, Component.text("Hareket stili"), true);
    }

    private static String textOr(DialogResponseView view, String key, String fallback) {
        String value = view.getText(key);
        return value == null ? fallback : value;
    }

    private static float floatOr(DialogResponseView view, String key, float fallback) {
        Float value = view.getFloat(key);
        return value == null ? fallback : value;
    }

    private static List<String> split(String joined) {
        List<String> out = new ArrayList<>();
        for (String part : joined.split(";")) {
            if (!part.isBlank()) {
                out.add(part.trim());
            }
        }
        return out;
    }

    private static String strip(String miniMessage) {
        return miniMessage.replaceAll("<[^>]+>", "").trim();
    }

    private static String round(double value) {
        return String.valueOf(Math.round(value * 10) / 10.0);
    }

    /** Ses ayarının geçerli olup olmadığını kabaca kontrol eder (editörde uyarı için). */
    public static boolean soundLooksValid(String spec) {
        return Effects.disabled(spec) || spec.trim().split("\\s+")[0].contains(".");
    }
}
