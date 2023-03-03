package net.polar.utils;

import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.other.ArmorStandMeta;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class MultiLineHologram {

    private final List<Component> lines;

    private final List<Entity> entities;

    public MultiLineHologram(@NotNull List<String> strings) {
        this.lines = strings.stream().map(ChatColor::color).toList();
        this.entities = new ArrayList<>(lines.size());
    }

    public MultiLineHologram(@NotNull String... lines) {
        this.lines = Stream.of(lines).map(ChatColor::color).toList();
        this.entities = new ArrayList<>(lines.length);
    }

    public void setLine(int index, @NotNull String line) {
        this.lines.set(index, ChatColor.color(line));
        this.entities.get(index).setCustomName(ChatColor.color(line));
    }

    public void remove() {
        this.entities.forEach(Entity::remove);
        entities.clear();
    }

    public void create(@NotNull Pos position, @NotNull Instance instance) {
        for (int i = 0; i < lines.size(); i++) {
            Entity entity = new Entity(EntityType.ARMOR_STAND);
            ArmorStandMeta meta = (ArmorStandMeta) entity.getEntityMeta();

            meta.setNotifyAboutChanges(false);

            meta.setSmall(true);
            meta.setHasNoBasePlate(true);
            meta.setMarker(true);
            meta.setInvisible(true);
            meta.setCustomNameVisible(true);
            meta.setCustomName(lines.get(i));
            meta.setHasNoGravity(true);

            meta.setNotifyAboutChanges(true);

            double yLevel = 0.5 + (0.3 * (lines.size() - i));
            entity.setInstance(instance, position.add(0, yLevel, 0));
            entities.add(entity);
        }
    }

}

