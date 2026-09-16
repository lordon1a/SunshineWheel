package me.guldal.sunshinewheel.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds the tiny resource pack (the 1x1 pixel font the wheel is drawn with).
 * Served by {@link WebServer} at {@code /pack.zip}; buyers who merge packs by hand
 * take {@code assets/sunshinewheel} from the same file.
 */
public final class PackServer {

    private static final int ROWS = 256;
    private static final int SPACE_BITS = 11;

    private PackServer() {
    }

    /** Pack format for the running server (1.21 → 1.21.9 table, 26.x → 88); unknown falls back to 34. */
    public static int packFormat(org.bukkit.plugin.java.JavaPlugin plugin) {
        int override = plugin.getConfig().getInt("pack.format", 0);
        if (override > 0) {
            return override;
        }
        String version = org.bukkit.Bukkit.getBukkitVersion().split("-")[0];
        return switch (version) {
            case "1.21", "1.21.1" -> 34;
            case "1.21.2", "1.21.3" -> 42;
            case "1.21.4" -> 46;
            case "1.21.5" -> 55;
            case "1.21.6" -> 63;
            case "1.21.7", "1.21.8", "1.21.9" -> 64;
            default -> version.startsWith("26.") ? 88 : 34;
        };
    }

    /** Builds the standalone resource pack (also used at runtime on startup). */
    public static void buildPack(File out, int packFormat) throws IOException {
        if (out.getParentFile() != null) {
            out.getParentFile().mkdirs();
        }
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(out.toPath()))) {
            zip.setLevel(9);
            writeEntry(zip, "pack.mcmeta",
                    ("{\"pack\":{\"description\":\"SunshineWheel wheel font\",\"pack_format\":" + packFormat + "}}")
                            .getBytes(StandardCharsets.UTF_8));
            writeEntry(zip, "assets/sunshinewheel/font/px.json", pxFontJson().getBytes(StandardCharsets.UTF_8));
            writeEntry(zip, "assets/sunshinewheel/textures/font/px.png", whitePixelPng());
        }
    }

    static String sha1(File file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(Files.readAllBytes(file.toPath()));
            StringBuilder out = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                out.append(String.format("%02x", b));
            }
            return out.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    private static void writeEntry(ZipOutputStream zip, String name, byte[] data) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(data);
        zip.closeEntry();
    }

    private static String pxFontJson() {
        JsonObject root = new JsonObject();
        JsonArray providers = new JsonArray();
        JsonObject spaces = new JsonObject();
        spaces.addProperty("type", "space");
        JsonObject advances = new JsonObject();
        for (int i = 0; i < SPACE_BITS; i++) {
            advances.addProperty(String.valueOf((char) (0xE000 + i)), -(1 << i));
            advances.addProperty(String.valueOf((char) (0xE020 + i)), 1 << i);
        }
        spaces.add("advances", advances);
        providers.add(spaces);
        for (int r = 0; r < ROWS; r++) {
            JsonObject row = new JsonObject();
            row.addProperty("type", "bitmap");
            row.addProperty("file", "sunshinewheel:font/px.png");
            row.addProperty("height", 1);
            row.addProperty("ascent", 1 - r);
            JsonArray chars = new JsonArray();
            chars.add(String.valueOf((char) (0xE100 + r)));
            row.add("chars", chars);
            providers.add(row);
        }
        root.add("providers", providers);
        return root.toString();
    }

    /** 1x1 white RGBA PNG, built by hand so java.desktop is not required. */
    private static byte[] whitePixelPng() throws IOException {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        png.write(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
        try (DataOutputStream out = new DataOutputStream(png)) {
            ByteArrayOutputStream ihdr = new ByteArrayOutputStream();
            try (DataOutputStream h = new DataOutputStream(ihdr)) {
                h.writeInt(1);
                h.writeInt(1);
                h.writeByte(8);
                h.writeByte(6);
                h.writeByte(0);
                h.writeByte(0);
                h.writeByte(0);
            }
            chunk(out, "IHDR", ihdr.toByteArray());
            Deflater deflater = new Deflater(9);
            deflater.setInput(new byte[] {0, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
            deflater.finish();
            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            byte[] buffer = new byte[64];
            while (!deflater.finished()) {
                compressed.write(buffer, 0, deflater.deflate(buffer));
            }
            deflater.end();
            chunk(out, "IDAT", compressed.toByteArray());
            chunk(out, "IEND", new byte[0]);
        }
        return png.toByteArray();
    }

    private static void chunk(DataOutputStream out, String kind, byte[] data) throws IOException {
        byte[] kindBytes = kind.getBytes(StandardCharsets.US_ASCII);
        out.writeInt(data.length);
        out.write(kindBytes);
        out.write(data);
        CRC32 crc = new CRC32();
        crc.update(kindBytes);
        crc.update(data);
        out.writeInt((int) crc.getValue());
    }
}
