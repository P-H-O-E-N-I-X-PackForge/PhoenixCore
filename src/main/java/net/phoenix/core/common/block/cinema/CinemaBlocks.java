package net.phoenix.core.common.block.cinema;

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.world.level.block.Blocks;
import net.phoenix.core.client.renderer.cinema.CinemaScreenRenderer;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public class CinemaBlocks {

    public static final BlockEntry<CinemaScreenBlock> CINEMA_SCREEN = REGISTRATE
            .block("cinema_screen", CinemaScreenBlock::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.noOcclusion().strength(2.0f))

            .simpleItem()

            .blockstate((ctx, prov) -> {})
            .lang("Cinema Screen")
            .register();

    public static final BlockEntityEntry<CinemaScreenBlockEntity> CINEMA_SCREEN_BE = REGISTRATE
            .blockEntity("cinema_screen", CinemaScreenBlockEntity::new)
            .validBlock(CINEMA_SCREEN)
            .renderer(() -> CinemaScreenRenderer::new)
            .register();

    public static void init() {}
}
