package net.phoenix.core.common.block.cinder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public class CinderBlocks {

    public static final BlockEntry<CinderConstructionBlock> CINDER_CONSTRUCTION = REGISTRATE
            .block("cinder_construction", CinderConstructionBlock::new)
            .initialProperties(() -> Blocks.MAGMA_BLOCK)
            .properties(p -> p.strength(-1.0f, 3600000.0f).noLootTable())

            .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(),
                    prov.models().cubeAll(ctx.getName(), new ResourceLocation("minecraft", "block/magma"))))
            .lang("Cinder Construction Site")
            .register();

    public static final BlockEntityEntry<CinderConstructionBlockEntity> CINDER_CONSTRUCTION_BE = REGISTRATE
            .blockEntity("cinder_construction", CinderConstructionBlockEntity::new)
            .validBlock(CINDER_CONSTRUCTION)
            .renderer(() -> net.phoenix.core.client.renderer.cinder.CinderConstructionRenderer::new)
            .register();

    public static void init() {}
}
