package net.phoenix.core.integration.continuum.machine;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.multiblock.Predicates;
import com.gregtechceu.gtceu.api.multiblock.pattern.IBlockPattern;
import com.gregtechceu.gtceu.api.multiblock.pattern.MultiblockPatternBuilder;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.common.data.GTBlocks;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public final class ContinuumMachines {

    private ContinuumMachines() {}

    private static final String[][] SLICES = buildSlices();

    public static final MultiblockMachineDefinition LAUNCH_COMPLEX = REGISTRATE
            .multiblock("continuum_launch_complex", LaunchPadMachine::new)
            .langValue("Continuum Launch Complex")
            .rotationState(RotationState.NON_Y_AXIS)
            .appearanceBlock(GTBlocks.CASING_STEEL_SOLID)
            .tooltips(
                    Component.literal("§6Where Continuum missions are launched from."),
                    Component.literal("§7Use the controller to open the map and launch."),
                    Component.literal("§7Its tier is the best energy input hatch it has; farther bodies need a higher"),
                    Component.literal("§7tier, and every launch draws power from the hatches."),
                    Component.literal("§7Rockets, probes and repair kits are taken from its item input buses;"),
                    Component
                            .literal("§7returned rockets and hauled resources are delivered to its item output buses."),
                    Component.literal("§7Hatches and buses go anywhere on the slab's edge (not the corners)."),
                    Component.literal("§7Keep the 1x1 shaft in the middle of the gantry clear for the rocket."))
            .pattern(definition -> buildPattern(definition))
            .workableCasingModel(GTCEu.id("block/casings/solid/machine_casing_solid_steel"),
                    GTCEu.id("block/multiblock/pyrolyse_oven"))
            .register();

    private static IBlockPattern buildPattern(MultiblockMachineDefinition definition) {
        var builder = MultiblockPatternBuilder.start(RelativeDirection.FRONT, RelativeDirection.UP,
                RelativeDirection.RIGHT);
        for (String[] slice : SLICES) builder = builder.slice(slice);

        var casing = Predicates.blocks(GTBlocks.CASING_STEEL_SOLID.get());
        var frame = Predicates.blocks(ForgeRegistries.BLOCKS.getValue(
                ResourceLocation.fromNamespaceAndPath("gtceu", "steel_frame")));

        return builder
                .where('S', Predicates.controller(definition))
                .where('B', casing)
                .where('H', Predicates.blocks(GTBlocks.CASING_STEEL_SOLID.get())
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1)
                                .setMaxGlobalLimited(2))
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(4))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(4))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2))
                        .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(2)))
                .where('F', frame)
                .where('A', Predicates.air())
                .build();
    }

    private static String[][] buildSlices() {
        int depth = 7;
        int width = 7;
        int height = 6;
        String[][] slices = new String[depth][height];

        for (int s = 0; s < depth; s++) {
            for (int y = 0; y < height; y++) {
                StringBuilder row = new StringBuilder();
                for (int c = 0; c < width; c++) row.append(cell(s, c, y));
                slices[s][y] = row.toString();
            }
        }
        return slices;
    }

    private static char cell(int s, int c, int y) {
        boolean outerCorner = (s == 0 || s == 6) && (c == 0 || c == 6);
        boolean innerRing = s >= 2 && s <= 4 && c >= 2 && c <= 4 && !(s == 3 && c == 3);
        boolean innerCorner = (s == 2 || s == 4) && (c == 2 || c == 4);
        boolean shaft = s == 3 && c == 3;

        if (y == 0) {
            if (s == 6 && c == 3) return 'S';
            boolean edge = s == 0 || s == 6 || c == 0 || c == 6;
            boolean corner = (s == 0 || s == 6) && (c == 0 || c == 6);
            return edge && !corner ? 'H' : 'B';
        }
        if (shaft && y <= 5) return 'A';
        if (y >= 1 && y <= 4 && outerCorner) return 'F';
        if (y == 5 && (s == 0 || s == 6 || c == 0 || c == 6)) return 'F';
        if (innerRing && y >= 1 && y <= 3 && innerCorner) return 'F';
        if (innerRing && y == 4) return 'F';
        return ' ';
    }

    public static void init() {}
}
