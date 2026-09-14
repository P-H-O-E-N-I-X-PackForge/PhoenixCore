package net.phoenix.core.api.pattern;

import com.gregtechceu.gtceu.api.multiblock.MultiPredicate;
import com.gregtechceu.gtceu.api.multiblock.Predicates;
import com.gregtechceu.gtceu.api.multiblock.error.PlaceholderError;
import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo;
import com.gregtechceu.gtceu.common.block.LampBlock;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenix.core.PhoenixAPI;
import net.phoenix.core.common.machine.PhoenixMachines;
import net.phoenix.core.integration.phoenix_tesla_network.api.machine.trait.ITeslaBattery;
import net.phoenix.core.integration.phoenix_tesla_network.common.block.TeslaBatteryBlock;
import net.phoenix.core.integration.vocal_resonance.SoundHatchPartMachine;
import net.phoenix.core.integration.vocal_resonance.SpeakerBlock;

import com.tterrag.registrate.util.entry.BlockEntry;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.gregtechceu.gtceu.common.data.GTBlocks.BORDERLESS_LAMPS;
import static com.gregtechceu.gtceu.common.data.GTBlocks.LAMPS;

public class PhoenixPredicates {

    public static MultiPredicate teslaBatteries() {
        List<BlockInfo> candidates = PhoenixAPI.TESLA_BATTERIES.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> e.getKey().getTier()))
                .map(e -> new BlockInfo(e.getValue().get().defaultBlockState()))
                .collect(Collectors.toList());

        return Predicates.builder("tesla_battery")
                .candidates(candidates)
                .predicate(ctx -> {
                    BlockState state = ctx.state();
                    for (Map.Entry<ITeslaBattery, Supplier<TeslaBatteryBlock>> entry : PhoenixAPI.TESLA_BATTERIES
                            .entrySet()) {
                        if (state.is(entry.getValue().get())) {
                            return true;
                        }
                    }
                    return false;
                })
                .errorFunction(ctx -> PlaceholderError.instance())
                .toMultiPredicate()
                .addTooltips(Component.translatable("gtceu.multiblock.pattern.error.batteries"));
    }

    @SafeVarargs
    public static MultiPredicate lamps(BlockEntry<LampBlock>... lampEntries) {
        List<BlockInfo> candidates = Arrays.stream(lampEntries)
                .map(e -> new BlockInfo(e.get().defaultBlockState()))
                .collect(Collectors.toList());

        return Predicates.builder("lamp")
                .candidates(candidates)
                .predicate(ctx -> {
                    BlockState state = ctx.state();
                    for (BlockEntry<LampBlock> entry : lampEntries) {
                        if (state.is(entry.get())) return true;
                    }
                    return false;
                })
                .errorFunction(ctx -> PlaceholderError.instance())
                .toMultiPredicate();
    }

    public static MultiPredicate anyLamp() {
        List<BlockEntry<LampBlock>> all = new ArrayList<>();
        all.addAll(LAMPS.values());
        all.addAll(BORDERLESS_LAMPS.values());
        return lamps(all.toArray(BlockEntry[]::new));
    }

    private static final Map<DyeColor, MultiPredicate> LAMPS_BY_COLOR = new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor color : DyeColor.values()) {
            LAMPS_BY_COLOR.put(color, lamps(LAMPS.get(color), BORDERLESS_LAMPS.get(color)));
        }
    }

    public static MultiPredicate lampsByColor(DyeColor color) {
        return LAMPS_BY_COLOR.getOrDefault(color, anyLamp());
    }

    public static MultiPredicate soundHatches() {
        List<BlockInfo> candidates = new ArrayList<>();
        for (var machine : PhoenixMachines.DISC_HATCH) {
            if (machine != null) candidates.add(BlockInfo.fromBlockState(machine.getBlock().defaultBlockState()));
        }
        for (var machine : PhoenixMachines.LIBRARY_HATCH) {
            if (machine != null) candidates.add(BlockInfo.fromBlockState(machine.getBlock().defaultBlockState()));
        }
        for (var machine : PhoenixMachines.STREAM_HATCH) {
            if (machine != null) candidates.add(BlockInfo.fromBlockState(machine.getBlock().defaultBlockState()));
        }

        return Predicates.builder("sound_hatch")
                .candidates(candidates)
                .predicate(ctx -> ctx.blockEntity() instanceof SoundHatchPartMachine)
                .errorFunction(ctx -> PlaceholderError.instance())
                .toMultiPredicate();
    }

    public static MultiPredicate tieredSpeakers() {
        var lvBlock = net.phoenix.core.common.block.PhoenixBlocks.SPEAKER_LV.get();
        List<BlockInfo> candidates = lvBlock != null ? List.of(
                BlockInfo.fromBlockState(lvBlock.defaultBlockState()),
                BlockInfo.fromBlockState(
                        net.phoenix.core.common.block.PhoenixBlocks.SPEAKER_MV.get().defaultBlockState()),
                BlockInfo.fromBlockState(
                        net.phoenix.core.common.block.PhoenixBlocks.SPEAKER_HV.get().defaultBlockState())) :
                List.of();

        return Predicates.builder("speaker")
                .candidates(candidates)
                .predicate(ctx -> ctx.state().getBlock() instanceof SpeakerBlock)
                .errorFunction(ctx -> PlaceholderError.instance())
                .toMultiPredicate();
    }
}
