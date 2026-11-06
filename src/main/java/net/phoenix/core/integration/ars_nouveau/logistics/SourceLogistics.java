package net.phoenix.core.integration.ars_nouveau.logistics;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.GTCapability;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.item.PipeBlockItem;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.registry.registrate.entry.MachineEntry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.phoenix.core.PhoenixCore;

import java.util.EnumMap;
import java.util.Map;

import static net.phoenix.core.common.machine.PhoenixMachines.registerTieredMachines;
import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

/**
 * Source logistics beyond jars: tiered pipes that carry Source between jars, hatches and relays, and wireless relays
 * that
 * move it between channels.
 */
public final class SourceLogistics {

    private SourceLogistics() {}

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK,
            PhoenixCore.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM,
            PhoenixCore.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BES = DeferredRegister
            .create(BuiltInRegistries.BLOCK_ENTITY_TYPE, PhoenixCore.MOD_ID);

    public static final Map<SourcePipeTier, DeferredHolder<Block, SourcePipeBlock>> PIPES = new EnumMap<>(
            SourcePipeTier.class);

    static {
        for (SourcePipeTier tier : SourcePipeTier.values()) {
            @SuppressWarnings("unchecked")
            DeferredHolder<BlockEntityType<?>, BlockEntityType<SourcePipeBlockEntity>>[] be = new DeferredHolder[1];

            DeferredHolder<Block, SourcePipeBlock> block = BLOCKS.register(tier.blockId(),
                    () -> new SourcePipeBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .strength(1.5f, 6f)
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .dynamicShape(), tier, () -> be[0].get()));

            be[0] = BES.register(tier.blockId(), () -> BlockEntityType.Builder.of(
                    (pos, state) -> new SourcePipeBlockEntity(be[0].get(), pos, state), block.get()).build(null));

            PIPES.put(tier, block);
            ITEMS.register(tier.blockId(), () -> new PipeBlockItem(block.get(), new Item.Properties()));
        }
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BES.register(bus);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (SourcePipeTier tier : SourcePipeTier.values()) {
            Block block = PIPES.get(tier).get();
            event.registerBlock(GTCapability.CAPABILITY_COVERABLE,
                    (level, pos, state, be, side) -> be instanceof SourcePipeBlockEntity pipe ?
                            pipe.getCoverContainer() : null,
                    block);
        }
    }

    // ------------------------------------------------------------------------------------------ the relays

    public static MachineEntry<MachineDefinition>[] RELAY;

    /** Registers the relays; called with the other machines. */
    public static void init() {
        REGISTRATE.creativeModeTab(() -> PhoenixCore.PHOENIX_CREATIVE_TAB);
        RELAY = registerTieredMachines("source_relay",
                (holder, tier) -> new SourceRelayMachine(holder, tier),
                (tier, builder) -> builder
                        .langValue(GTValues.VNF[tier] + " Source Relay")
                        .rotationState(RotationState.ALL)
                        .tooltips(
                                Component.literal("§dWireless Source.§r"),
                                Component.literal("Set a channel: senders give their Source to receivers on it."),
                                Component.literal("§7Moves " + SourceRelayMachine.rate(tier) +
                                        " Source/t. Costs more with distance.§r"))
                        .overlayTieredHullModel(PhoenixCore.id("block/machine/part/source_relay"))
                        .register(),
                new int[] { GTValues.HV, GTValues.EV, GTValues.IV });
    }
}
