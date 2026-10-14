package net.phoenix.core.common.data;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.client.renderer.cover.ICoverRenderer;
import com.gregtechceu.gtceu.client.renderer.cover.IOCoverRenderer;
import com.gregtechceu.gtceu.client.renderer.cover.SimpleCoverRenderer;
import com.gregtechceu.gtceu.common.cover.ConveyorCover;
import com.gregtechceu.gtceu.common.cover.FluidRegulatorCover;
import com.gregtechceu.gtceu.common.cover.PumpCover;
import com.gregtechceu.gtceu.common.cover.RobotArmCover;

import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.cover.PhoenixCoverSolarPanel;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.Supplier;

public class PhoenixCovers {

    public static final int START_TIER = GTValues.UHV;
    public static final int END_TIER = GTValues.MAX;

    public static final int[] PHOENIX_SOLAR_PANEL_TIERS = GTValues.tiersBetween(START_TIER, END_TIER);

    public final static CoverDefinition[] SOLAR_PANEL_TIERED;

    public static final int ULV_ITEMS_PER_SECOND = CoverRates.ULV_ITEMS_PER_SECOND;
    public static final int ULV_FLUID_PER_TICK = CoverRates.ULV_FLUID_PER_TICK;

    public final static CoverDefinition ULV_CONVEYOR;
    public final static CoverDefinition ULV_ROBOT_ARM;
    public final static CoverDefinition ULV_PUMP;
    public final static CoverDefinition ULV_FLUID_REGULATOR;

    static {
        GTRegistries.COVERS.unfreeze();
        ULV_CONVEYOR = register("ulv_conveyor",
                (def, cov, side) -> new ConveyorCover(def, cov, side, GTValues.ULV, ULV_ITEMS_PER_SECOND),
                () -> () -> new IOCoverRenderer(GTCEu.id("block/cover/conveyor"), null,
                        GTCEu.id("block/cover/conveyor_emissive"),
                        GTCEu.id("block/cover/conveyor_inverted_emissive")));
        ULV_ROBOT_ARM = register("ulv_robot_arm",
                (def, cov, side) -> new RobotArmCover(def, cov, side, GTValues.ULV, ULV_ITEMS_PER_SECOND),
                () -> () -> new IOCoverRenderer(GTCEu.id("block/cover/arm"), null,
                        GTCEu.id("block/cover/arm_emissive"), GTCEu.id("block/cover/arm_inverted_emissive")));
        ULV_PUMP = register("ulv_pump",
                (def, cov, side) -> new PumpCover(def, cov, side, GTValues.ULV, ULV_FLUID_PER_TICK),
                () -> () -> IOCoverRenderer.PUMP_LIKE_COVER_RENDERER);
        ULV_FLUID_REGULATOR = register("ulv_fluid_regulator",
                (def, cov, side) -> new FluidRegulatorCover(def, cov, side, GTValues.ULV, ULV_FLUID_PER_TICK),
                () -> () -> IOCoverRenderer.PUMP_LIKE_COVER_RENDERER);
        SOLAR_PANEL_TIERED = registerPhoenixTiered(
                "solar_panel",
                PhoenixCoverSolarPanel::new,
                PHOENIX_SOLAR_PANEL_TIERS);
        GTRegistries.COVERS.freeze();
    }

    public static void init() {}

    private static CoverDefinition register(String id, CoverDefinition.CoverBehaviourProvider behavior,
                                            Supplier<Supplier<ICoverRenderer>> renderer) {
        var definition = new CoverDefinition(PhoenixCore.id(id), behavior, renderer);
        GTRegistries.COVERS.register(definition.getId(), definition);
        return definition;
    }

    private static CoverDefinition[] registerPhoenixTiered(
                                                           String id,
                                                           CoverDefinition.TieredCoverBehaviourProvider behaviorCreator,
                                                           int... tiers) {
        return Arrays.stream(tiers).mapToObj(tier -> {
            var name = id + "." + GTValues.VN[tier].toLowerCase(Locale.ROOT);
            var resourceLocation = PhoenixCore.id(name);

            var definition = new CoverDefinition(
                    resourceLocation,
                    (def, coverable, side) -> behaviorCreator.create(def, coverable, side, tier),
                    () -> () -> new SimpleCoverRenderer(GTCEu.id("block/cover/" + id)));

            GTRegistries.COVERS.register(definition.getId(), definition);
            return definition;
        }).toArray(CoverDefinition[]::new);
    }
}
