package net.polar.player;

import net.minestom.server.entity.Player;
import net.minestom.server.network.PlayerProvider;
import net.minestom.server.network.player.PlayerConnection;
import net.polar.Hub;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class HubPlayerProvider implements PlayerProvider {
    @Override
    public @NotNull Player createPlayer(@NotNull UUID uuid, @NotNull String username, @NotNull PlayerConnection connection) {
        return Hub.getDatabase().loadPlayer(uuid, username, connection);
    }
}
