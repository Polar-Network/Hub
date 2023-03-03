package net.polar.event.listeners;

import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

public class DamageListener implements EventListener<EntityDamageEvent> {
    @Override
    public @NotNull Class<EntityDamageEvent> eventType() {
        return EntityDamageEvent.class;
    }

    @Override
    public @NotNull Result run(@NotNull EntityDamageEvent event) {
        event.setCancelled(true);
        if (event.getDamageType() == DamageType.VOID && event.getEntity() instanceof Player player) {
            player.teleport(player.getRespawnPoint());
        }

        return Result.SUCCESS;
    }
}
