package net.polar.event.listeners;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.player.PlayerStartFlyingEvent;
import net.polar.player.HubPlayer;
import org.jetbrains.annotations.NotNull;

public final class JumpListener implements EventListener<PlayerStartFlyingEvent> {
    @Override
    public @NotNull Class<PlayerStartFlyingEvent> eventType() {
        return PlayerStartFlyingEvent.class;
    }

    @Override
    public @NotNull Result run(@NotNull PlayerStartFlyingEvent event) {

        ((HubPlayer) event.getPlayer()).doubleJump();
        return Result.SUCCESS;
    }
}
