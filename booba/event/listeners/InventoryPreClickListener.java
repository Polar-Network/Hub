package net.polar.event.listeners;

import net.minestom.server.event.EventListener;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.inventory.PlayerInventory;
import net.polar.gui.Gui;
import org.jetbrains.annotations.NotNull;

public final class InventoryPreClickListener implements EventListener<InventoryPreClickEvent> {
    @Override
    public @NotNull Class<InventoryPreClickEvent> eventType() {
        return InventoryPreClickEvent.class;
    }

    @Override
    public @NotNull Result run(@NotNull InventoryPreClickEvent event) {
        if (event.getInventory() == null) {
            event.setCancelled(true);
            return Result.SUCCESS;
        }
        if (event.getInventory() instanceof Gui gui) {
            gui.handleClick(event);
        }
        return Result.SUCCESS;
    }
}
