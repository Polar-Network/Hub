package net.polar.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ChatColor {

    private ChatColor() {}
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    public static @NotNull Component color(@NotNull String message) {
        return MINI_MESSAGE.deserialize(message).decoration(TextDecoration.ITALIC, false);
    }

    public static @NotNull List<Component> color(@NotNull List<String> messages) {
        return messages.stream().map(i -> MINI_MESSAGE.deserialize(i).decoration(TextDecoration.ITALIC, false)).toList();
    }

}
