package net.phoenix.core.common.block;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootTable;

import com.tterrag.registrate.util.entry.BlockEntry;

import java.util.List;

import static net.phoenix.core.PhoenixCore.PHOENIX_CREATIVE_TAB;
import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public final class PrismariteBlocks {

    private PrismariteBlocks() {}

    static {
        REGISTRATE.creativeModeTab(() -> PHOENIX_CREATIVE_TAB);
    }

    public static final List<String> COLORS = List.of("red", "orange", "yellow", "green", "cyan", "blue", "purple",
            "pink");

    private static String title(String color) {
        return Character.toUpperCase(color.charAt(0)) + color.substring(1);
    }

    private static BlockEntry<PrismariteBlock> cluster(String id, String name, boolean item) {
        var builder = REGISTRATE.block(id, PrismariteBlock::new)
                .initialProperties(() -> Blocks.AMETHYST_CLUSTER)
                .properties(p -> p.noCollission().noOcclusion().instabreak().sound(SoundType.AMETHYST_CLUSTER)
                        .pushReaction(PushReaction.DESTROY).offsetType(BlockBehaviour.OffsetType.XZ)
                        .lightLevel(s -> 10))
                .blockstate((ctx, prov) -> prov.simpleBlock(ctx.get(), prov.models()
                        .cross(ctx.getName(), prov.modLoc("block/" + ctx.getName())).renderType("minecraft:cutout")))
                .lang(name);
        if (!item) {
            return builder.loot((loot, block) -> loot.add(block, LootTable.lootTable())).register();
        }
        return builder.item().model((ctx, prov) -> prov.generated(ctx, prov.modLoc("block/" + ctx.getName()))).build()
                .register();
    }

    static {
        for (String color : COLORS) {
            cluster("prismarite_cluster_" + color, title(color) + " Prismarite Cluster", true);
            cluster("large_prismarite_cluster_" + color, "Large " + title(color) + " Prismarite Cluster", true);
            cluster("large_prismarite_cluster_" + color + "_top", "Large " + title(color) + " Prismarite Cluster Top",
                    false);
        }
    }

    public static void init() {}
}
