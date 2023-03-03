package net.polar.event.listeners;

import net.minestom.server.event.EventListener;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import org.jetbrains.annotations.NotNull;

public class BlockBreakListener implements EventListener<PlayerBlockBreakEvent> {
    @Override
    public @NotNull Class<PlayerBlockBreakEvent> eventType() {
        return PlayerBlockBreakEvent.class;
    }

    @Override
    public @NotNull Result run(@NotNull PlayerBlockBreakEvent event) {
        event.setCancelled(true);
        return Result.SUCCESS;
    }
}
