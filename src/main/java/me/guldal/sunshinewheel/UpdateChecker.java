package me.guldal.sunshinewheel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Async update check against a small JSON file
 * ({@code {"version":"1.1.0","url":"https://..."}}). Never blocks the server;
 * offline/missing file is silently ignored. Admins are told in-game once per session.
 */
public final class UpdateChecker implements Listener {

    private final JavaPlugin plugin;
    private final Set<UUID> notified = ConcurrentHashMap.newKeySet();

    private volatile boolean available;
    private volatile String latest = "";
    private volatile String page = "";
    private BukkitTask task;

    public UpdateChecker(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("update-checker.enabled", true)) {
            return;
        }
        String url = plugin.getConfig().getString("update-checker.url",
                "https://rehber.sunshine-mc.net/wheel-version.json");
        if (url == null || url.isBlank()) {
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> check(url));
        task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin,
                () -> check(url), 20L * 60 * 30, 20L * 60 * 60 * 6);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void check(String url) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "SunshineWheel-UpdateChecker")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return;
            }
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            String remote = json.has("version") ? json.get("version").getAsString() : "";
            if (remote.isBlank()) {
                return;
            }
            boolean newer = compare(remote, plugin.getPluginMeta().getVersion()) > 0;
            String link = json.has("url") ? json.get("url").getAsString() : "";
            if (newer && !available) {
                plugin.getLogger().info("Update available: " + remote
                        + " (running " + plugin.getPluginMeta().getVersion() + ")"
                        + (link.isBlank() ? "" : " — " + link));
            }
            latest = remote;
            page = link;
            available = newer;
        } catch (Exception ignored) {
            // Offline, timeout, bad JSON: keep the last known state.
        }
    }

    /** Numeric dotted compare; missing parts are 0. */
    static int compare(String a, String b) {
        String[] left = a.split("\\.");
        String[] right = b.split("\\.");
        for (int i = 0; i < Math.max(left.length, right.length); i++) {
            int x = part(left, i);
            int y = part(right, i);
            if (x != y) {
                return Integer.compare(x, y);
            }
        }
        return 0;
    }

    private static int part(String[] parts, int index) {
        if (index >= parts.length) {
            return 0;
        }
        try {
            return Integer.parseInt(parts[index].replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!available) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("sunshinewheel.admin") || !notified.add(player.getUniqueId())) {
            return;
        }
        player.sendMessage(Messages.mm("update.available",
                "latest", latest,
                "current", plugin.getPluginMeta().getVersion(),
                "url", page.isBlank() ? "https://rehber.sunshine-mc.net/wheel-setup.html" : page));
    }
}
