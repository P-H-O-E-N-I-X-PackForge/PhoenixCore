package net.phoenix.core.integration.growth;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.growth.tendril.GrowthWardBlock;

import com.tterrag.registrate.util.entry.BlockEntry;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public class GrowthBlocks {

    public static final BlockEntry<GrowthWardBlock> GROWTH_WARD = REGISTRATE
            .block("growth_ward", GrowthWardBlock::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .properties(p -> p.strength(3.0f, 6.0f).sound(SoundType.METAL)
                    .isValidSpawn((state, level, pos, ent) -> false))
            .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(),
                    prov.models().cubeAll(ctx.getName(), PhoenixCore.id("block/growth_ward"))))
            .lang("Growth Ward")
            .item(BlockItem::new)
            .build()
            .register();

    public static final BlockEntry<GrowthShellBlock> GROWTH_SHELL = REGISTRATE
            .block("growth_shell", GrowthShellBlock::new)
            .initialProperties(() -> Blocks.MOSS_BLOCK)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .properties(p -> p.strength(3.0f, 6.0f).sound(SoundType.MOSS)
                    .isValidSpawn((state, level, pos, ent) -> false))
            .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(),
                    prov.models().cubeAll(ctx.getName(), PhoenixCore.id("block/growth_shell"))))
            .lang("Living Shell")
            .item(BlockItem::new)
            .build()
            .register();

    public static final BlockEntry<GrowthTendrilBlock> GROWTH_TENDRIL = REGISTRATE
            .block("growth_tendril", GrowthTendrilBlock::new)
            .initialProperties(() -> Blocks.MOSS_BLOCK)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .properties(p -> p.strength(2.0f, 4.0f).sound(SoundType.MOSS)
                    .isValidSpawn((state, level, pos, ent) -> false))
            .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(),
                    prov.models().cubeAll(ctx.getName(), PhoenixCore.id("block/growth_tendril"))))
            .lang("Tendril")
            .item(BlockItem::new)
            .build()
            .register();

    public static void init() {}
}
