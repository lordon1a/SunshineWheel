package me.guldal.sunshinewheel.api;

import me.guldal.sunshinewheel.wheel.WheelInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Çevirme başlamadan önce ateşlenir; iptal edilirse maliyet alınmaz ve çark dönmez. */
public final class WheelSpinStartEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final WheelInstance wheel;
    private boolean cancelled;

    public WheelSpinStartEvent(Player player, WheelInstance wheel) {
        this.player = player;
        this.wheel = wheel;
    }

    public Player getPlayer() {
        return player;
    }

    public WheelInstance getWheel() {
        return wheel;
    }

    public String getWheelId() {
        return wheel.def.id;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
