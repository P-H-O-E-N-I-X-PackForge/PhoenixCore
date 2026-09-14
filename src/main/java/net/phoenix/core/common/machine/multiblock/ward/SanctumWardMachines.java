package net.phoenix.core.common.machine.multiblock.ward;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.multiblock.Predicates;
import com.gregtechceu.gtceu.api.multiblock.pattern.MultiblockPatternBuilder;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.client.renderer.machine.multiblock.PhoenixDynamicRenderHelpers;
import net.phoenix.core.common.data.PhoenixRecipeTypes;

import static com.gregtechceu.gtceu.common.data.GTBlocks.*;
import static com.gregtechceu.gtceu.common.data.models.GTMachineModels.createWorkableCasingMachineModel;
import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public final class SanctumWardMachines {

    public static final MultiblockMachineDefinition SANCTUM_WARD = REGISTRATE
            .multiblock("sanctum_ward", SanctumWardMachine::new)
            .langValue("Sanctum Ward")
            .recipeType(PhoenixRecipeTypes.SANCTUM_WARD_RECIPES)
            .rotationState(RotationState.NON_Y_AXIS)
            .appearanceBlock(CASING_STAINLESS_CLEAN)
            .tooltips(
                    Component.literal("§6Radiates a 50x50 ward while active,"),
                    Component.literal("§7pushing out and blocking the spawns of whichever mobs"),
                    Component.literal("§7its current upkeep item targets. No power needed."),
                    Component.literal("§7§oGlowstone Dust: hostile  §7§oWheat: passive"),
                    Component.literal("§7§oGunpowder: neutral  §7§oEnder Pearl: all"))
            .pattern(definition -> MultiblockPatternBuilder
                    .start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)

                    .slice("BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB",
                            "BBBBBCBBBBB", "BBBBBCBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB")
                    .slice("BDDDDDDDDDB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBCBBBBB",
                            "BBBBEEEBBBB", "BBBBEEEBBBB", "BBBBBCBBBBB", "BBBBBBBBBBB")
                    .slice("BDDDDDDDDDB", "BBFBBBBBFBB", "BBFBBBBBFBB", "BBFBBCBBFBB", "BBFGGGGGFBB",
                            "BBGGAAAGGBB", "BBGGAAAGGBB", "BBBGGGGGBBB", "BBBBBCBBBBB")
                    .slice("BDDDDDDDDDB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBCBCBCBBB", "BBGGGGGGGBB",
                            "BBGAAAAAGBB", "BBGAAAAAGBB", "BBGGGGGGGBB", "BBBBBCBBBBB")
                    .slice("BDDDDDDDDDB", "BBBBBCBBBBB", "BBBBBCBBBBB", "BBBBCCCBBBB", "BBGGAAAGGBB",
                            "BEAAAAAAAEB", "BEAAAAAAAEB", "BBGGGAGGGBB", "BBBBBCBBBBB")
                    .slice("BDDDDDDDDDB", "BBBBCDCBBBB", "BBBBCDCBBBB", "BBCCCCCCCBB", "BCGAACAAGCB",
                            "CEAAACAAAEC", "CEAAACAAAEC", "BCGGACAGGCB", "BBCCCCCCCBB")
                    .slice("BDDDDDDDDDB", "BBBBBCBBBBB", "BBBBBCBBBBB", "BBBBCCCBBBB", "BBGGAAAGGBB",
                            "BEAAAAAAAEB", "BEAAAAAAAEB", "BBGGGAGGGBB", "BBBBBCBBBBB")
                    .slice("BDDDDDDDDDB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBCBCBCBBB", "BBGGGGGGGBB",
                            "BBGAAAAAGBB", "BBGAAAAAGBB", "BBGGGGGGGBB", "BBBBBCBBBBB")
                    .slice("BDDDDDDDDDB", "BBFBBBBBFBB", "BBFBBBBBFBB", "BBFBBCBBFBB", "BBFGGGGGFBB",
                            "BBGGAAAGGBB", "BBGGAAAGGBB", "BBBGGGGGBBB", "BBBBBCBBBBB")
                    .slice("BDDDDDDDDDB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBCCCBBBB",
                            "BBBCEHECBBB", "BBBCEEECBBB", "BBBBCCCBBBB", "BBBBBBBBBBB")
                    .slice("BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB",
                            "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB")

                    .where('A', Predicates.air())
                    .where('B', Predicates.any())
                    .where('C',
                            Predicates.blocks(ForgeRegistries.BLOCKS
                                    .getValue(ResourceLocation.fromNamespaceAndPath("gtceu", "steel_frame"))))
                    .where('D', Predicates.blocks(CASING_BRONZE_BRICKS.get()))
                    .where('E', Predicates.blocks(CASING_LAMINATED_GLASS.get()))
                    .where('F', Predicates.blocks(CASING_STEEL_SOLID.get()))
                    .where('G', Predicates.blocks(CASING_STAINLESS_CLEAN.get())
                            .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                            .or(Predicates.autoAbilities(definition.getRecipeTypes())))
                    .where('H', Predicates.controller(definition))
                    .build())
            .model(
                    createWorkableCasingMachineModel(
                            GTCEu.id("block/casings/solid/machine_casing_clean_stainless_steel"),
                            GTCEu.id("block/multiblock/large_miner"))
                            .andThen(b -> b.addDynamicRenderer(PhoenixDynamicRenderHelpers::getSanctumWardRenderer)))
            .hasBER(true)
            .register();

    public static void init() {}

    private SanctumWardMachines() {}
}
