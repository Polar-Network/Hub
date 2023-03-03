package net.polar.event.listeners;

import net.minestom.server.event.EventListener;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.polar.Hub;
import net.polar.player.HubPlayer;
import org.jetbrains.annotations.NotNull;

public class DisconnectListener implements EventListener<PlayerDisconnectEvent> {
    @Override
    public @NotNull Class<PlayerDisconnectEvent> eventType() {
        return PlayerDisconnectEvent.class;
    }

    @Override
    public @NotNull Result run(@NotNull PlayerDisconnectEvent event) {

        Hub.getDatabase().savePlayer((HubPlayer) event.getPlayer());

        return Result.SUCCESS;
    }
}
