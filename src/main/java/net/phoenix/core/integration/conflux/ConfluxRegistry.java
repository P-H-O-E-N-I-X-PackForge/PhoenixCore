package net.phoenix.core.integration.conflux;


import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.conflux.pipe.ConfluxDataCapability;
import net.phoenix.core.integration.conflux.pipe.ConfluxMultiHandlerCapability;
import net.phoenix.core.integration.conflux.pipe.ConfluxPipeBlock;
import net.phoenix.core.integration.conflux.pipe.ConfluxPipeBlockEntity;
import net.phoenix.core.integration.conflux.pipe.ConfluxPipeType;
import net.phoenix.core.integration.conflux.terminal.ConfluxTerminalRegistry;

import java.util.EnumMap;
import java.util.Map;

public final class ConfluxRegistry {

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK,
            PhoenixCore.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM,
            PhoenixCore.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BES = DeferredRegister
            .create(BuiltInRegistries.BLOCK_ENTITY_TYPE, PhoenixCore.MOD_ID);

    public static final Map<ConfluxDataType, DeferredHolder<ConfluxPipeBlock, ConfluxPipeBlock>> PIPES = new EnumMap<>(
            ConfluxDataType.class);
    public static final Map<ConfluxDataType, DeferredHolder<BlockEntityType<?>, BlockEntityType<ConfluxPipeBlockEntity>>> PIPE_BES = new EnumMap<>(
            ConfluxDataType.class);

    static {
        for (ConfluxDataType dt : ConfluxDataType.values()) {
            String id = dt.id() + "_data_pipe";
            ConfluxPipeType pipeType = ConfluxPipeType.values()[dt.ordinal()];

            @SuppressWarnings("unchecked")
            DeferredHolder<BlockEntityType<?>, BlockEntityType<ConfluxPipeBlockEntity>>[] beHolder = new RegistryObject[1];

            DeferredHolder<ConfluxPipeBlock, ConfluxPipeBlock> block = BLOCKS.register(id,
                    () -> new ConfluxPipeBlock(pipeProps(dt), pipeType, () -> beHolder[0].get()));

            DeferredHolder<BlockEntityType<?>, BlockEntityType<ConfluxPipeBlockEntity>> be = BES.register(id,
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new ConfluxPipeBlockEntity(beHolder[0].get(), pos, state),
                            block.get()).build(null));

            beHolder[0] = be;

            PIPES.put(dt, block);
            PIPE_BES.put(dt, be);

            ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
        }
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BES.register(bus);
        ConfluxTerminalRegistry.register(bus);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        ConfluxDataCapability.register(event);
        ConfluxMultiHandlerCapability.register(event);
    }

    private static BlockBehaviour.Properties pipeProps(ConfluxDataType type) {
        return BlockBehaviour.Properties.of()
                .mapColor(typeColor(type))
                .strength(1.5f, 6f)
                .sound(SoundType.METAL)
                .noOcclusion()
                .dynamicShape();
    }

    private static MapColor typeColor(ConfluxDataType type) {
        return switch (type) {
            case MATERIAL -> MapColor.GOLD;
            case BIOLOGICAL -> MapColor.GRASS;
            case ENERGETIC -> MapColor.DIAMOND;
            case COMPUTATIONAL -> MapColor.COLOR_PURPLE;
            case ARCANE -> MapColor.COLOR_MAGENTA;
        };
    }

    private ConfluxRegistry() {}
}
