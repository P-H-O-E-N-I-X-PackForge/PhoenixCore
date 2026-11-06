package net.phoenix.core.integration.conflux.terminal;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.phoenix.core.PhoenixCore;

public final class ConfluxTerminalRegistry {

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK,
            PhoenixCore.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM,
            PhoenixCore.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BES = DeferredRegister
            .create(BuiltInRegistries.BLOCK_ENTITY_TYPE, PhoenixCore.MOD_ID);

    public static final DeferredHolder<Block, ResearchTerminalBlock> TERMINAL;
    public static final DeferredHolder<Item, Item> TERMINAL_ITEM;
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResearchTerminalBlockEntity>> TERMINAL_BE;

    static {
        @SuppressWarnings("unchecked")
        DeferredHolder<BlockEntityType<?>, BlockEntityType<ResearchTerminalBlockEntity>>[] beHolder = new DeferredHolder[1];

        TERMINAL = BLOCKS.register("research_terminal",
                () -> new ResearchTerminalBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(3f, 12f)
                        .sound(SoundType.METAL)
                        .noOcclusion()));

        TERMINAL_BE = BES.register("research_terminal",
                () -> BlockEntityType.Builder.of(
                        (pos, state) -> new ResearchTerminalBlockEntity(beHolder[0].get(), pos, state),
                        TERMINAL.get()).build(null));

        beHolder[0] = TERMINAL_BE;

        TERMINAL_ITEM = ITEMS.register("research_terminal", () -> new BlockItem(TERMINAL.get(), new Item.Properties()) {

            @Override
            public net.minecraft.network.chat.Component getName(net.minecraft.world.item.ItemStack stack) {
                return net.minecraft.network.chat.Component.translatableWithFallback(
                        "block.phoenixcore.research_terminal", "Conflux Research Terminal");
            }
        });
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BES.register(bus);
    }

    private ConfluxTerminalRegistry() {}
}
