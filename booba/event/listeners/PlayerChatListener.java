package net.polar.event.listeners;

import net.minestom.server.event.EventListener;
import net.minestom.server.event.player.PlayerChatEvent;
import net.polar.Hub;
import net.polar.utils.ChatColor;
import org.jetbrains.annotations.NotNull;

public class PlayerChatListener implements EventListener<PlayerChatEvent> {
    @Override
    public @NotNull Class<PlayerChatEvent> eventType() {
        return PlayerChatEvent.class;
    }

    @Override
    public @NotNull Result run(@NotNull PlayerChatEvent event) {
        event.setCancelled(true);
        event.getPlayer().sendMessage(ChatColor.color(Hub.getConfig().getString("server.chat-message")));
        return Result.SUCCESS;
    }
}
