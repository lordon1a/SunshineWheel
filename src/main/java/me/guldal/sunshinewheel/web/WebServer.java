package me.guldal.sunshinewheel.web;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import me.guldal.sunshinewheel.wheel.MotionStyle;
import me.guldal.sunshinewheel.wheel.WheelDefinition;
import me.guldal.sunshinewheel.wheel.WheelInstance;
import me.guldal.sunshinewheel.wheel.WheelManager;
import me.guldal.sunshinewheel.wheel.WinEffect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * JSON API + web editor + resource pack, all from one embedded server.
 * Binds to localhost only by default; exposed publicly via nginx HTTPS, or set
 * {@code web.bind} to {@code 0.0.0.0} for direct access (no extra setup needed).
 *
 * <p>Sign-in: {@code /wheel web} (in-game or console) creates a one-time key, the browser trades it
 * for a session at {@code POST /api/auth}. Sessions last 12 hours and are sent with the
 * {@code Authorization: Bearer ...} header. All writes run on the Bukkit main thread.
 */
public final class WebServer {

    private static final Gson GSON = new Gson();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long TOKEN_TTL_MS = 10 * 60 * 1000L;
    private static final long SESSION_TTL_MS = 12 * 60 * 60 * 1000L;
    private static final int MAX_BODY_BYTES = 2 * 1024 * 1024;

    /** Editor UI served straight from the jar (no files to host). */
    private static final Map<String, String> UI_FILES = Map.of(
            "/", "web/index.html",
            "/index.html", "web/index.html",
            "/style.css", "web/style.css",
            "/app.js", "web/app.js");
    private static final Map<String, String> UI_TYPES = Map.of(
            "/", "text/html; charset=utf-8",
            "/index.html", "text/html; charset=utf-8",
            "/style.css", "text/css; charset=utf-8",
            "/app.js", "application/javascript; charset=utf-8");

    public record Session(UUID playerId, String playerName, long expiresAt) {
    }

    private final JavaPlugin plugin;
    private final WheelManager wheels;
    private final Map<String, Session> tokens = new ConcurrentHashMap<>();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    private HttpServer server;
    private ExecutorService executor;
    private String publicUrl = "https://rehber.guldal.me/wheel/";
    private final Map<String, byte[]> uiCache = new ConcurrentHashMap<>();
    private final RateLimiter rateLimiter = new RateLimiter();
    private byte[] packBytes = new byte[0];
    private String packSha1 = "";

    public WebServer(JavaPlugin plugin, WheelManager wheels) {
        this.plugin = plugin;
        this.wheels = wheels;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("web.enabled", true)) {
            plugin.getLogger().info("Web API disabled (web.enabled: false).");
            return;
        }
        String bind = plugin.getConfig().getString("web.bind", "0.0.0.0");
        int port = plugin.getConfig().getInt("web.port", 8770);
        publicUrl = plugin.getConfig().getString("web.public-url", publicUrl);
        try {
            server = HttpServer.create(new InetSocketAddress(bind, port), 0);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Web API failed to bind " + bind + ":" + port, e);
            return;
        }
        server.createContext("/api/", this::route);
        server.createContext("/pack.zip", this::servePack);
        server.createContext("/", this::serveUi);
        executor = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "SunshineWheel-Web");
            thread.setDaemon(true);
            return thread;
        });
        server.setExecutor(executor);
        server.start();
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::pruneExpired, 20L * 300, 20L * 300);
        buildPack();
        String host = bind.equals("0.0.0.0") ? "<your-server-ip>" : bind;
        plugin.getLogger().info("Web editor ready: http://" + host + ":" + port + "/"
                + (publicUrl.isBlank() ? "" : " (link: " + publicUrl + ")"));
        if (bind.equals("0.0.0.0")) {
            plugin.getLogger().info("If the editor is unreachable, open the port in your firewall:");
            plugin.getLogger().info("  Linux:   ufw allow " + port + "/tcp   (or your host panel)");
            plugin.getLogger().info("  Windows: New-NetFirewallRule -DisplayName SunshineWheel -Direction Inbound -LocalPort " + port + " -Protocol TCP -Action Allow");
        }
        if (!packSha1.isEmpty()) {
            plugin.getLogger().info("Resource pack ready: http://" + host + ":" + port + "/pack.zip");
            plugin.getLogger().info("Put this in server.properties:  resource-pack=http://" + host + ":" + port + "/pack.zip");
            plugin.getLogger().info("Put this in server.properties:  resource-pack-sha1=" + packSha1);
        }
    }

    private void buildPack() {
        if (!plugin.getConfig().getBoolean("pack.enabled", true)) {
            return;
        }
        try {
            File file = new File(plugin.getDataFolder(), "pack.zip");
            PackServer.buildPack(file);
            packBytes = Files.readAllBytes(file.toPath());
            packSha1 = PackServer.sha1(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not build resource pack", e);
        }
    }

    private void servePack(HttpExchange exchange) throws IOException {
        try {
            if (!exchange.getRequestMethod().equals("GET") || packBytes.length == 0) {
                exchange.sendResponseHeaders(packBytes.length == 0 ? 404 : 405, -1);
                return;
            }
            exchange.getResponseHeaders().set("Content-Type", "application/zip");
            exchange.getResponseHeaders().set("Cache-Control", "public, max-age=3600");
            exchange.sendResponseHeaders(200, packBytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(packBytes);
            }
        } finally {
            exchange.close();
        }
    }

    private void serveUi(HttpExchange exchange) throws IOException {
        try {
            if (!exchange.getRequestMethod().equals("GET")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            String resource = UI_FILES.get(exchange.getRequestURI().getPath());
            if (resource == null) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            byte[] data = uiCache.get(resource);
            if (data == null) {
                try (var in = WebServer.class.getResourceAsStream("/" + resource)) {
                    if (in == null) {
                        exchange.sendResponseHeaders(404, -1);
                        return;
                    }
                    data = in.readAllBytes();
                }
                uiCache.put(resource, data);
            }
            exchange.getResponseHeaders().set("Content-Type", UI_TYPES.get(exchange.getRequestURI().getPath()));
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, data.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(data);
            }
        } finally {
            exchange.close();
        }
    }

    /** Drops expired keys, sessions and rate-limit windows. */
    private void pruneExpired() {
        long now = System.currentTimeMillis();
        tokens.entrySet().removeIf(entry -> entry.getValue().expiresAt() < now);
        sessions.entrySet().removeIf(entry -> entry.getValue().expiresAt() < now);
        rateLimiter.prune();
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

    /** Editor link: one-time key produced by the /wheel web command. */
    public String createToken(Player player) {
        return publicUrl + "#token=" + createRawToken(player);
    }

    /** Raw one-time key (the command builds the message around it). */
    public String createRawToken(Player player) {
        String token = random(24);
        long ttl = player == null ? 5 * 60 * 1000L : TOKEN_TTL_MS;
        tokens.put(token, new Session(player == null ? null : player.getUniqueId(),
                player == null ? "konsol" : player.getName(), System.currentTimeMillis() + ttl));
        return token;
    }

    public String publicUrl() {
        return publicUrl;
    }

    public String guideUrl() {
        return plugin.getConfig().getString("web.guide-url", "https://rehber.guldal.me/wheel-setup.html");
    }

    public int port() {
        return plugin.getConfig().getInt("web.port", 8770);
    }

    /** Client IP, proxy-aware: X-Forwarded-For is trusted only from localhost. */
    private static String clientIp(HttpExchange exchange) {
        String remote = exchange.getRemoteAddress().getAddress().getHostAddress();
        if (remote.equals("127.0.0.1") || remote.equals("0:0:0:0:0:0:0:1")) {
            String forwarded = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return remote;
    }

    private void route(HttpExchange exchange) throws IOException {
        if (!rateLimiter.allow("all:" + clientIp(exchange), 120)) {
            error(exchange, 429, "Too many requests. Slow down.");
            return;
        }
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if (path.equals("/api/auth")) {
                if (method.equals("POST")) {
                    handleAuth(exchange);
                } else {
                    error(exchange, 405, "Method not allowed.");
                }
                return;
            }
            if (path.equals("/api/logout") && method.equals("POST")) {
                handleLogout(exchange);
                return;
            }
            Session session = authenticate(exchange);
            if (session == null) {
                error(exchange, 401, "Session invalid or expired. Run /wheel web in-game for a new link.");
                return;
            }
            switch (path) {
                case "/api/meta" -> {
                    if (method.equals("GET")) {
                        handleMeta(exchange);
                    } else {
                        error(exchange, 405, "Method not allowed.");
                    }
                }
                case "/api/materials" -> {
                    if (method.equals("GET")) {
                        handleMaterials(exchange);
                    } else {
                        error(exchange, 405, "Method not allowed.");
                    }
                }
                case "/api/inventory" -> {
                    if (method.equals("GET")) {
                        handleInventory(exchange, session);
                    } else {
                        error(exchange, 405, "Method not allowed.");
                    }
                }
                case "/api/held" -> {
                    if (method.equals("GET")) {
                        handleHeld(exchange, session);
                    } else {
                        error(exchange, 405, "Method not allowed.");
                    }
                }
                case "/api/wheels" -> {
                    if (method.equals("GET")) {
                        json(exchange, 200, wheelList());
                    } else if (method.equals("POST")) {
                        handleWheelCreate(exchange, session);
                    } else {
                        error(exchange, 405, "Method not allowed.");
                    }
                }
                default -> {
                    if (path.startsWith("/api/wheels/")) {
                        String id = URLDecoder.decode(path.substring("/api/wheels/".length()), StandardCharsets.UTF_8);
                        switch (method) {
                            case "GET" -> handleWheelGet(exchange, id);
                            case "PUT" -> handleWheelPut(exchange, id);
                            case "DELETE" -> handleWheelDelete(exchange, id);
                            default -> error(exchange, 405, "Method not allowed.");
                        }
                    } else {
                        error(exchange, 404, "Not found.");
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            error(exchange, 400, e.getMessage() == null ? "Invalid request." : e.getMessage());
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Web API error: " + exchange.getRequestURI(), e);
            error(exchange, 500, "Server error. Check console.");
        } finally {
            exchange.close();
        }
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            sessions.remove(header.substring("Bearer ".length()).trim());
        }
        JsonObject out = new JsonObject();
        out.addProperty("ok", true);
        json(exchange, 200, out);
    }

    private void handleAuth(HttpExchange exchange) throws IOException {
        if (!rateLimiter.allow("auth:" + clientIp(exchange), 10)) {
            error(exchange, 429, "Too many requests. Slow down.");
            return;
        }
        JsonObject input = body(exchange);
        String token = string(input, "token", "");
        Session pending = tokens.remove(token);
        if (pending == null || pending.expiresAt() < System.currentTimeMillis()) {
            plugin.getLogger().warning("Failed web login attempt (invalid key).");
            error(exchange, 401, "Connection key invalid or already used.");
            return;
        }
        String session = random(32);
        long expires = System.currentTimeMillis() + SESSION_TTL_MS;
        sessions.put(session, new Session(pending.playerId(), pending.playerName(), expires));
        JsonObject out = new JsonObject();
        out.addProperty("session", session);
        out.addProperty("name", pending.playerName());
        out.addProperty("expires", expires);
        json(exchange, 200, out);
    }

    private void handleMeta(HttpExchange exchange) throws IOException {
        JsonObject out = new JsonObject();
        out.addProperty("version", plugin.getPluginMeta().getVersion());
        out.addProperty("wheels", wheels.ids().size());
        JsonArray worlds = new JsonArray();
        Bukkit.getWorlds().forEach(world -> worlds.add(world.getName()));
        out.add("worlds", worlds);
        JsonArray styles = new JsonArray();
        for (MotionStyle style : MotionStyle.values()) {
            JsonObject item = new JsonObject();
            item.addProperty("id", style.name());
            item.addProperty("label", style.label());
            styles.add(item);
        }
        out.add("styles", styles);
        JsonArray effects = new JsonArray();
        for (WinEffect effect : WinEffect.values()) {
            JsonObject item = new JsonObject();
            item.addProperty("id", effect.name());
            item.addProperty("label", effect.label());
            effects.add(item);
        }
        out.add("effects", effects);
        json(exchange, 200, out);
    }

    private void handleMaterials(HttpExchange exchange) throws IOException {
        JsonArray out = new JsonArray();
        Arrays.stream(Material.values())
                .filter(material -> material.isItem() && !material.isAir())
                .map(Material::name)
                .sorted()
                .forEach(out::add);
        json(exchange, 200, out);
    }

    /** Oturum sahibinin envanteri (ödül ikonu seçmek için). */
    private void handleInventory(HttpExchange exchange, Session session) throws Exception {
        JsonObject out = sync(() -> {
            Player player = session.playerId() == null ? null : Bukkit.getPlayer(session.playerId());
            if (player == null) {
                return null;
            }
            JsonArray slots = new JsonArray();
            ItemStack[] contents = player.getInventory().getContents();
            for (int i = 0; i < contents.length; i++) {
                ItemStack item = contents[i];
                if (item == null || item.getType().isAir()) {
                    continue;
                }
                JsonObject slot = new JsonObject();
                slot.addProperty("slot", i);
                slot.addProperty("icon", item.getType().name());
                slot.addProperty("amount", item.getAmount());
                slots.add(slot);
            }
            JsonObject result = new JsonObject();
            result.add("slots", slots);
            return result;
        });
        if (out == null) {
            error(exchange, 409, "You must be online in-game to pick from your inventory.");
            return;
        }
        json(exchange, 200, out);
    }

    /** Oturum sahibinin elindeki eşya (ödül ikonu olarak kullanmak için). */
    private void handleHeld(HttpExchange exchange, Session session) throws Exception {
        JsonObject out = sync(() -> {
            Player player = session.playerId() == null ? null : Bukkit.getPlayer(session.playerId());
            if (player == null) {
                return null;
            }
            ItemStack hand = player.getInventory().getItemInMainHand();
            JsonObject result = new JsonObject();
            if (hand.getType().isAir()) {
                result.addProperty("empty", true);
            } else {
                result.addProperty("icon", hand.getType().name());
                result.addProperty("amount", hand.getAmount());
            }
            return result;
        });
        if (out == null) {
            error(exchange, 409, "You must be online in-game to use your held item.");
            return;
        }
        json(exchange, 200, out);
    }

    private JsonObject wheelList() throws Exception {
        return sync(() -> {
            JsonArray array = new JsonArray();
            for (String id : wheels.ids()) {
                array.add(WheelJson.toJson(wheels.get(id).def));
            }
            JsonObject out = new JsonObject();
            out.add("wheels", array);
            return out;
        });
    }

    private void handleWheelGet(HttpExchange exchange, String id) throws Exception {
        JsonObject out = sync(() -> {
            WheelInstance instance = wheels.get(id);
            return instance == null ? null : WheelJson.toJson(instance.def);
        });
        if (out == null) {
            error(exchange, 404, "Wheel not found: " + id);
            return;
        }
        json(exchange, 200, out);
    }

    private void handleWheelPut(HttpExchange exchange, String id) throws Exception {
        JsonObject input = body(exchange);
        String result = sync(() -> {
            WheelInstance instance = wheels.get(id);
            if (instance == null) {
                return "missing";
            }
            if (instance.isSpinning()) {
                return "spinning";
            }
            WheelJson.apply(instance.def, input);
            wheels.save();
            wheels.respawn(instance);
            return "ok";
        });
        switch (result) {
            case "missing" -> error(exchange, 404, "Wheel not found: " + id);
            case "spinning" -> error(exchange, 409, "This wheel is spinning; wait for it to finish, then save again.");
            default -> {
                JsonObject out = new JsonObject();
                out.addProperty("ok", true);
                json(exchange, 200, out);
            }
        }
    }

    private void handleWheelCreate(HttpExchange exchange, Session session) throws Exception {
        JsonObject input = body(exchange);
        String id = string(input, "id", "").trim().toLowerCase(Locale.ROOT);
        if (!id.matches("[a-z0-9_-]{1,32}")) {
            error(exchange, 400, "ID must be 1-32 chars: a-z, 0-9, -, _.");
            return;
        }
        String result = sync(() -> {
            if (wheels.get(id) != null) {
                return "exists";
            }
            Player player = session.playerId() == null ? null : Bukkit.getPlayer(session.playerId());
            Location location = player != null ? player.getLocation()
                    : Bukkit.getWorlds().get(0).getSpawnLocation();
            WheelDefinition def = new WheelDefinition(id);
            def.world = location.getWorld().getName();
            def.x = location.getX();
            def.y = location.getY();
            def.z = location.getZ();
            def.yaw = Math.round((location.getYaw() + 180f) / 90f) * 90f;
            WheelJson.apply(def, input);
            return wheels.create(def) == null ? "exists" : "ok";
        });
        if ("exists".equals(result)) {
            error(exchange, 409, "This ID is already taken: " + id);
            return;
        }
        JsonObject out = new JsonObject();
        out.addProperty("ok", true);
        out.addProperty("id", id);
        json(exchange, 200, out);
    }

    private void handleWheelDelete(HttpExchange exchange, String id) throws Exception {
        String result = sync(() -> {
            WheelInstance instance = wheels.get(id);
            if (instance == null) {
                return "missing";
            }
            if (instance.isSpinning()) {
                return "spinning";
            }
            return wheels.remove(id) ? "ok" : "missing";
        });
        switch (result) {
            case "missing" -> error(exchange, 404, "Wheel not found: " + id);
            case "spinning" -> error(exchange, 409, "This wheel is spinning; wait for it to finish and try again.");
            default -> {
                JsonObject out = new JsonObject();
                out.addProperty("ok", true);
                json(exchange, 200, out);
            }
        }
    }

    private Session authenticate(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        String key = header.substring("Bearer ".length()).trim();
        Session session = sessions.get(key);
        if (session == null) {
            return null;
        }
        if (session.expiresAt() < System.currentTimeMillis()) {
            sessions.remove(key);
            return null;
        }
        return session;
    }

    private JsonObject body(HttpExchange exchange) throws IOException {
        String length = exchange.getRequestHeaders().getFirst("Content-Length");
        if (length != null) {
            try {
                if (Long.parseLong(length) > MAX_BODY_BYTES) {
                    throw new IllegalArgumentException("Request too large.");
                }
            } catch (NumberFormatException ignored) {
                // Broken header: bounded read below kicks in.
            }
        }
        byte[] data = exchange.getRequestBody().readNBytes(MAX_BODY_BYTES + 1);
        if (data.length > MAX_BODY_BYTES) {
            throw new IllegalArgumentException("Request too large.");
        }
        if (data.length == 0) {
            return new JsonObject();
        }
        try {
            return JsonParser.parseString(new String(data, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid JSON body.");
        }
    }

    private void json(HttpExchange exchange, int status, JsonElement body) throws IOException {
        byte[] data = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, data.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(data);
        }
    }

    private void error(HttpExchange exchange, int status, String message) throws IOException {
        JsonObject out = new JsonObject();
        out.addProperty("error", message);
        json(exchange, status, out);
    }

    private <T> T sync(java.util.concurrent.Callable<T> callable) throws Exception {
        try {
            return Bukkit.getScheduler().callSyncMethod(plugin, callable).get(10, TimeUnit.SECONDS);
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IllegalArgumentException illegal) {
                throw illegal;
            }
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw new RuntimeException(cause);
        }
    }

    private static String string(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("'" + key + "' must be text.");
        }
        return element.getAsString();
    }

    private static String random(int bytes) {
        byte[] buffer = new byte[bytes];
        RANDOM.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }
}
