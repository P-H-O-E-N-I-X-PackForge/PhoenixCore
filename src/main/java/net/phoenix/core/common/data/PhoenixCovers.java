package net.phoenix.core.common.data;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import net.minecraft.core.Holder;
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

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public class PhoenixCovers {

    public static final int START_TIER = GTValues.UHV;
    public static final int END_TIER = GTValues.MAX;

    public static final int[] PHOENIX_SOLAR_PANEL_TIERS = GTValues.tiersBetween(START_TIER, END_TIER);

    public final static Holder<CoverDefinition>[] SOLAR_PANEL_TIERED;

    public static final int ULV_ITEMS_PER_SECOND = CoverRates.ULV_ITEMS_PER_SECOND;
    public static final int ULV_FLUID_PER_TICK = CoverRates.ULV_FLUID_PER_TICK;

    public final static Holder<CoverDefinition> ULV_CONVEYOR;
    public final static Holder<CoverDefinition> ULV_ROBOT_ARM;
    public final static Holder<CoverDefinition> ULV_PUMP;
    public final static Holder<CoverDefinition> ULV_FLUID_REGULATOR;

    static {
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
    }

    public static void init() {}

    private static Holder<CoverDefinition> register(String id, CoverDefinition.CoverBehaviourProvider behavior,
                                                    Supplier<Supplier<ICoverRenderer>> renderer) {
        return REGISTRATE.cover(id, behavior, renderer);
    }

    @SuppressWarnings("unchecked")
    private static Holder<CoverDefinition>[] registerPhoenixTiered(String id,
                                                                  CoverDefinition.TieredCoverBehaviourProvider behaviorCreator,
                                                                  int... tiers) {
        return Arrays.stream(tiers).mapToObj(tier -> {
            var name = id + "." + GTValues.VN[tier].toLowerCase(Locale.ROOT);
            return (Holder<CoverDefinition>) REGISTRATE.cover(name,
                    (def, coverable, side) -> behaviorCreator.create(def, coverable, side, tier),
                    () -> () -> new SimpleCoverRenderer(GTCEu.id("block/cover/" + id)));
        }).toArray(Holder[]::new);
    }
}
