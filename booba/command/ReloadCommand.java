package net.polar.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;
import net.polar.Hub;

public final class ReloadCommand extends Command {

    public ReloadCommand() {
        super("reload");
        this.setCondition(((sender, commandString) -> {
            if (sender instanceof Player player) {
                return player.hasPermission("hub.reload");
            }
            return true;
        }));
        addSyntax(((sender, context) -> {
            try {
                Hub.getConfig().reload();
                sender.sendMessage(Component.text("Reloaded config!", NamedTextColor.GREEN));
            }
            catch (Exception e) {
                sender.sendMessage(Component.text("Failed to reload config!", NamedTextColor.RED));
                e.printStackTrace();
            }
        }));
    }

}
