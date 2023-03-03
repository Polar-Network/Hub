package net.polar.gui;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemHideFlag;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.polar.player.HubPlayer;
import net.polar.utils.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class Gui extends Inventory {

    private static final ItemStack FILLER = ItemStack.builder(Material.GRAY_STAINED_GLASS_PANE)
            .displayName(Component.empty())
            .meta((meta) -> {
                meta.hideFlag(ItemHideFlag.HIDE_ATTRIBUTES);
            })
            .build();

    private final Map<Integer, GuiClickable> clickables = new HashMap<>();

    public Gui(@NotNull InventoryType inventoryType, @NotNull String title) {
        super(inventoryType, ChatColor.color(title));
    }

    public void open(Player player) {
        player.closeInventory();
        buildInventory();
        player.openInventory(this);
    }

    protected void buildInventory() {
        for (int i = 0; i < getSize(); i++) {
            setItemStack(i, FILLER);
        }
        clickables.forEach((slot, clickable) -> {
            setItemStack(slot, clickable.item());
        });
    }

    public void handleClick(InventoryPreClickEvent event) {
        event.setCancelled(true);
        GuiClickable clickable = this.clickables.get(event.getSlot());
        if (clickable != null) {
            clickable.action().accept((HubPlayer) event.getPlayer(), event.getClickType());
        }
    }

    public void addClickable(int slot, GuiClickable clickable) {
        this.clickables.put(slot, clickable);
    }

    public void removeClickable(int slot) {
        this.clickables.remove(slot);
    }

    public void clearClickables() {
        this.clickables.clear();
    }
}
