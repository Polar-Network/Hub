package net.polar.event.listeners;

import net.minestom.server.event.EventListener;
import net.minestom.server.event.item.ItemDropEvent;
import org.jetbrains.annotations.NotNull;

public final class PlayerDropItemListener implements EventListener<ItemDropEvent> {
    @Override
    public @NotNull Class<ItemDropEvent> eventType() {
        return ItemDropEvent.class;
    }

    @Override
    public @NotNull Result run(@NotNull ItemDropEvent event) {
        event.setCancelled(true);
        return Result.SUCCESS;
    }
}
