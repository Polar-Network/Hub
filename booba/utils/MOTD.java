package net.polar.utils;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.kyori.adventure.text.Component;
import net.minestom.server.ping.ResponseData;
import net.polar.Hub;
import net.polar.utils.ChatColor;


import java.util.List;

public class MOTD extends ResponseData {

    public MOTD() {
        YamlDocument config = Hub.getConfig();
        Component randomMotd = ChatColor.color(
                config.getStringList("server.motd", List.of()).get((int) (Math.random() * config.getStringList("server.motd", List.of()).size())));
        setDescription(randomMotd);
        if (config.getInt("server.max-players", 0) != 0) {
            setMaxPlayer(config.getInt("server.max-players", 0));
        }
    }

}
