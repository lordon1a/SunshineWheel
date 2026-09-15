package me.guldal.sunshinewheel.api;

import me.guldal.sunshinewheel.wheel.Reward;
import me.guldal.sunshinewheel.wheel.WheelInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Ödül verilmeden hemen önce ateşlenir; iptal edilirse ödül (eşya, komut, efekt) verilmez. */
public final class WheelSpinWinEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final WheelInstance wheel;
    private final Reward reward;
    private boolean cancelled;

    public WheelSpinWinEvent(Player player, WheelInstance wheel, Reward reward) {
        this.player = player;
        this.wheel = wheel;
        this.reward = reward;
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

    public Reward getReward() {
        return reward;
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
