package net.phoenix.core.common.block.cinema;

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.phoenix.core.client.renderer.cinema.CinemaScreenRenderer;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public class CinemaBlocks {

    public static final BlockEntry<CinemaScreenBlock> CINEMA_SCREEN = REGISTRATE
            .block("cinema_screen", CinemaScreenBlock::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.noOcclusion().strength(2.0f))

            // Plain .simpleItem() inherits a block model that never gets generated - the block
            // itself is RenderShape.INVISIBLE with its own BlockEntityRenderer, so its blockstate
            // callback is intentionally empty. Borrow iron block's own icon for the held/inventory
            // item instead of leaving it undefined.
            .item(BlockItem::new)
            .model((ctx, prov) -> prov.generated(ctx::getEntry, new ResourceLocation("minecraft", "block/iron_block")))
            .build()

            .blockstate((ctx, prov) -> {})
            .lang("Cinema Screen")
            .register();

    public static final BlockEntityEntry<CinemaScreenBlockEntity> CINEMA_SCREEN_BE = REGISTRATE
            .blockEntity("cinema_screen", CinemaScreenBlockEntity::new)
            .validBlock(CINEMA_SCREEN)
            .renderer(() -> CinemaScreenRenderer::new)
            .register();

    // Place a whole grid of screens in one action instead of one block at a time - see
    // CinemaScreenArrayItem for the placement logic.
    public static final ItemEntry<CinemaScreenArrayItem> CINEMA_SCREEN_ARRAY_3X3 = REGISTRATE
            .item("cinema_screen_array_3x3", p -> new CinemaScreenArrayItem(3, p))
            .lang("Cinema Screen Array (3x3)")
            .properties(p -> p.stacksTo(16))
            .register();

    public static final ItemEntry<CinemaScreenArrayItem> CINEMA_SCREEN_ARRAY_9X9 = REGISTRATE
            .item("cinema_screen_array_9x9", p -> new CinemaScreenArrayItem(9, p))
            .lang("Cinema Screen Array (9x9)")
            .properties(p -> p.stacksTo(16))
            .register();

    public static void init() {}
}
