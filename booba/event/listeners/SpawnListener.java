package net.polar.event.listeners;

import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.polar.Hub;
import net.polar.player.HubPlayer;
import net.polar.utils.ChatColor;
import org.jetbrains.annotations.NotNull;

import static net.polar.player.HubPlayer.HIDE_PLAYERS_ITEM;

public final class SpawnListener implements EventListener<PlayerSpawnEvent> {

    @Override
    public @NotNull Class<PlayerSpawnEvent> eventType() {
        return PlayerSpawnEvent.class;
    }

    @Override
    public @NotNull Result run(@NotNull PlayerSpawnEvent event) {
        final HubPlayer player = (HubPlayer) event.getPlayer();
        if (player.getEntityId() == Hub.getWumpusEntityId()) return Result.SUCCESS;

        player.setTeam(Hub.getTeam());
        player.setGameMode(GameMode.valueOf(Hub.getConfig().getString("server.default-gamemode", "SURVIVAL").toUpperCase()));
        player.getInventory().setItemStack(8, HIDE_PLAYERS_ITEM);
        player.getInventory().setItemStack(0, Hub.getServerSelectorItem());

        if (Hub.getConfig().getBoolean("players-invisible-default")) {
            player.togglePlayerVisibility();
        }


        player.sendMessage(ChatColor.color(Hub.getConfig().getString("server.join-message").replace("%player%", player.getUsername())));
        return Result.SUCCESS;
    }
}
