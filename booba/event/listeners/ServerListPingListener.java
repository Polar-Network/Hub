package net.polar.event.listeners;

import net.minestom.server.event.EventListener;
import net.minestom.server.event.server.ServerListPingEvent;
import net.polar.utils.MOTD;
import org.jetbrains.annotations.NotNull;

public final class ServerListPingListener implements EventListener<ServerListPingEvent> {
    @Override
    public @NotNull Class<ServerListPingEvent> eventType() {
        return ServerListPingEvent.class;
    }

    @Override
    public @NotNull Result run(@NotNull ServerListPingEvent event) {
        event.setResponseData(new MOTD());
        return Result.SUCCESS;
    }
}
