package net.polar.event.listeners;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.polar.Hub;
import net.polar.utils.ChatColor;
import org.jetbrains.annotations.NotNull;

public final class AttackListener implements EventListener<EntityAttackEvent> {
    @Override
    public @NotNull Class<EntityAttackEvent> eventType() {
        return EntityAttackEvent.class;
    }

    @Override
    public @NotNull Result run(@NotNull EntityAttackEvent event) {

        if (!(event.getEntity() instanceof Player player)) return Result.SUCCESS;

        int id = event.getTarget().getEntityId();
        if (id != Hub.getWumpusEntityId()) return Result.SUCCESS;

        Component message = ChatColor.color(Hub.getConfig().getString("discord.message"));
        player.sendMessage(message);

        return Result.SUCCESS;
    }
}
