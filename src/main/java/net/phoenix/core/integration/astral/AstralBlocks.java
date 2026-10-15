package net.phoenix.core.integration.astral;


import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.astral.ritual.AstralRitualPedestalBlock;
import net.phoenix.core.integration.astral.ritual.AstralRitualPedestalBlockEntity;

import com.tterrag.registrate.util.entry.BlockEntry;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public class AstralBlocks {

    public static final BlockEntry<Block> ASTRAL_RUNE_BLOCK = REGISTRATE
            .block("astral_rune_block", Block::new)
            .initialProperties(() -> Blocks.AMETHYST_BLOCK)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .properties(p -> p.strength(3.0f, 6.0f).sound(SoundType.AMETHYST))
            .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(),
                    prov.models().cubeAll(ctx.getName(),
                            PhoenixCore.id("block/casings/multiblock/machine_casing_invariant_naquadah_alloy"))))
            .lang("Astral Rune Block")
            .item(BlockItem::new)
            .build()
            .register();

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK,
            PhoenixCore.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM,
            PhoenixCore.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BES = DeferredRegister
            .create(BuiltInRegistries.BLOCK_ENTITY_TYPE, PhoenixCore.MOD_ID);

    public static final DeferredHolder<AstralRitualPedestalBlock, AstralRitualPedestalBlock> ASTRAL_RITUAL_PEDESTAL;
    public static final DeferredHolder<BlockEntityType<AstralRitualPedestalBlockEntity>, BlockEntityType<AstralRitualPedestalBlockEntity>> ASTRAL_RITUAL_PEDESTAL_BE;

    static {
        @SuppressWarnings("unchecked")
        DeferredHolder<BlockEntityType<AstralRitualPedestalBlockEntity>, BlockEntityType<AstralRitualPedestalBlockEntity>>[] beHolder = new RegistryObject[1];

        ASTRAL_RITUAL_PEDESTAL = BLOCKS.register("astral_ritual_pedestal",
                () -> new AstralRitualPedestalBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_PURPLE)
                        .strength(3f, 12f)
                        .sound(SoundType.AMETHYST)
                        .noOcclusion()));

        ASTRAL_RITUAL_PEDESTAL_BE = BES.register("astral_ritual_pedestal",
                () -> BlockEntityType.Builder.of(
                        (pos, state) -> new AstralRitualPedestalBlockEntity(beHolder[0].get(), pos, state),
                        ASTRAL_RITUAL_PEDESTAL.get()).build(null));

        beHolder[0] = ASTRAL_RITUAL_PEDESTAL_BE;

        ITEMS.register("astral_ritual_pedestal",
                () -> new BlockItem(ASTRAL_RITUAL_PEDESTAL.get(), new Item.Properties()));
    }

    public static void registerDeferred(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BES.register(bus);
    }

    public static void init() {}
}
