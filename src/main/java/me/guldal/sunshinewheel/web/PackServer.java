package me.guldal.sunshinewheel.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Draws the wheel at runtime, so buyers need one tiny resource pack (the 1x1
 * pixel font). This class builds that pack on startup and serves it over HTTP,
 * so no pack merging is required: point {@code server.properties} at the URL.
 *
 * <p>Buyers who merge packs by hand can download the same file and copy
 * {@code assets/sunshinewheel} into their own pack instead.
 */
public final class PackServer {

    /** Baseline format for 1.21–1.21.1; newer clients show a warning but still apply it. */
    private static final int PACK_FORMAT = 34;
    private static final int ROWS = 256;
    private static final int SPACE_BITS = 11;

    private final JavaPlugin plugin;

    private HttpServer server;
    private ExecutorService executor;
    private File packFile;
    private String sha1 = "";

    public PackServer(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("pack.enabled", true)) {
            plugin.getLogger().info("Pack server disabled (pack.enabled: false).");
            return;
        }
        try {
            plugin.getDataFolder().mkdirs();
            packFile = new File(plugin.getDataFolder(), "pack.zip");
            buildPack(packFile);
            sha1 = sha1(packFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not build resource pack", e);
            return;
        }
        String bind = plugin.getConfig().getString("pack.bind", "0.0.0.0");
        int port = plugin.getConfig().getInt("pack.port", 8765);
        try {
            server = HttpServer.create(new InetSocketAddress(bind, port), 0);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Pack server could not bind " + bind + ":" + port, e);
            return;
        }
        server.createContext("/pack.zip", this::serve);
        executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "SunshineWheel-Pack");
            thread.setDaemon(true);
            return thread;
        });
        server.setExecutor(executor);
        server.start();

        String url = plugin.getConfig().getString("pack.public-url", "");
        if (url.isBlank()) {
            String host = bind.equals("0.0.0.0") ? "<server-ip>" : bind;
            url = "http://" + host + ":" + port + "/pack.zip";
        }
        plugin.getLogger().info("Resource pack ready: " + url);
        plugin.getLogger().info("server.properties: resource-pack=" + url);
        plugin.getLogger().info("server.properties: resource-pack-sha1=" + sha1);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    public boolean isRunning() {
        return server != null;
    }

    public String sha1() {
        return sha1;
    }

    private void serve(HttpExchange exchange) throws IOException {
        try {
            if (!exchange.getRequestMethod().equals("GET")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            byte[] data = Files.readAllBytes(packFile.toPath());
            exchange.getResponseHeaders().set("Content-Type", "application/zip");
            exchange.getResponseHeaders().set("Cache-Control", "public, max-age=3600");
            exchange.sendResponseHeaders(200, data.length);
            try (var out = exchange.getResponseBody()) {
                out.write(data);
            }
        } finally {
            exchange.close();
        }
    }

    private static void buildPack(File out) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(out.toPath()))) {
            zip.setLevel(9);
            writeEntry(zip, "pack.mcmeta",
                    ("{\"pack\":{\"description\":\"SunshineWheel wheel font\",\"pack_format\":" + PACK_FORMAT + "}}")
                            .getBytes(StandardCharsets.UTF_8));
            writeEntry(zip, "assets/sunshinewheel/font/px.json", pxFontJson().getBytes(StandardCharsets.UTF_8));
            writeEntry(zip, "assets/sunshinewheel/textures/font/px.png", whitePixelPng());
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

    private static String sha1(File file) throws IOException {
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
}
