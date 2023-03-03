package net.polar.event.listeners;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.polar.Hub;
import net.polar.utils.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public final class InteractionListener implements EventListener<PlayerEntityInteractEvent> {

    private Set<Player> filters = new HashSet<>();

    @Override
    public @NotNull Class<PlayerEntityInteractEvent> eventType() {
        return PlayerEntityInteractEvent.class;
    }

    @Override
    public @NotNull Result run(@NotNull PlayerEntityInteractEvent event) {
        int id = event.getTarget().getEntityId();
        if (id != Hub.getWumpusEntityId())  return Result.SUCCESS;
        if (!filters.contains(event.getPlayer())) {
            filters.add(event.getPlayer());
            Component message = ChatColor.color(Hub.getConfig().getString("discord.message"));
            event.getPlayer().sendMessage(message);
        }
        else {
            filters.remove(event.getPlayer());
        }
        return Result.SUCCESS;
    }
}
