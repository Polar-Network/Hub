package net.polar.utils.schematic;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBTWriter;
import org.jglrxavpok.hephaistos.nbt.mutable.MutableNBTCompound;

import java.io.IOException;
import java.nio.file.Path;

public class SchematicWriter {

    public static void write(@NotNull Schematic schematic, @NotNull Path schemPath) throws IOException {
        MutableNBTCompound schematicNBT = new MutableNBTCompound();
        Point size = schematic.size();
        schematicNBT.setShort("Width", (short) size.x());
        schematicNBT.setShort("Height", (short) size.y());
        schematicNBT.setShort("Length", (short) size.z());

        Point offset = schematic.offset();
        MutableNBTCompound schematicMetadata = new MutableNBTCompound();
        schematicMetadata.setInt("WEOffsetX", offset.blockX());
        schematicMetadata.setInt("WEOffsetY", offset.blockY());
        schematicMetadata.setInt("WEOffsetZ", offset.blockZ());

        schematicNBT.set("Metadata", schematicMetadata.toCompound());

        schematicNBT.setByteArray("BlockData", schematic.blocks());
        Block[] blocks = schematic.palette();

        schematicNBT.setInt("PaletteMax", blocks.length);

        MutableNBTCompound palette = new MutableNBTCompound();
        for (int i = 0; i < blocks.length; i++) {
            palette.setInt(blocks[i].name(), i);
        }
        schematicNBT.set("Palette", palette.toCompound());

        try (NBTWriter writer = new NBTWriter(schemPath)) {
            writer.writeRaw(schematicNBT.toCompound());
        }
    }
}