package net.polar.event.listeners;

import net.minestom.server.event.EventListener;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.item.ItemStack;
import net.polar.Hub;
import net.polar.player.HubPlayer;
import org.jetbrains.annotations.NotNull;

import static net.polar.player.HubPlayer.HIDE_PLAYERS_ITEM;
import static net.polar.player.HubPlayer.SHOW_PLAYERS_ITEM;

public final class ItemInteractionListener implements EventListener<PlayerUseItemEvent> {
    @Override
    public @NotNull Class<PlayerUseItemEvent> eventType() {
        return PlayerUseItemEvent.class;
    }

    @Override
    public @NotNull Result run(@NotNull PlayerUseItemEvent event) {
        final HubPlayer player = (HubPlayer) event.getPlayer();
        final ItemStack heldItem = event.getItemStack();
        if (
              heldItem.isSimilar(SHOW_PLAYERS_ITEM)
              ||
              heldItem.isSimilar(HIDE_PLAYERS_ITEM)
           ) {
            player.togglePlayerVisibility();
        }
        else if (heldItem.isSimilar(Hub.getServerSelectorItem())) {
            Hub.getServerSelectorGui().open(player);
        }
        else if (heldItem.isSimilar(HubPlayer.RESET_PARKOUR_ITEM)) {
            player.resetCurrentParkour();
        }
        else if (heldItem.isSimilar(HubPlayer.EXIT_PARKOUR_ITEM)) {
            player.exitCurrentParkour();
        }
        else if (heldItem.isSimilar(HubPlayer.LAST_CHECKPOINT_ITEM)) {
            player.teleportToLastCheckpoint();
        }

        return Result.SUCCESS;
    }
}
