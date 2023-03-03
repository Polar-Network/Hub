package net.polar.gui;

import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import net.polar.player.HubPlayer;

import java.util.function.BiConsumer;

public record GuiClickable(ItemStack item, BiConsumer<HubPlayer, ClickType> action) {}
