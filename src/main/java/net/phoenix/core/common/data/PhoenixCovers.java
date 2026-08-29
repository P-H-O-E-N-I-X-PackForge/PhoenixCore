package net.phoenix.core.common.data;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.client.renderer.cover.SimpleCoverRenderer;

import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.cover.PhoenixCoverSolarPanel;

import java.util.Arrays;
import java.util.Locale;

public class PhoenixCovers {

    public static final int START_TIER = GTValues.UHV;
    public static final int END_TIER = GTValues.MAX;

    public static final int[] PHOENIX_SOLAR_PANEL_TIERS = GTValues.tiersBetween(START_TIER, END_TIER);

    public final static CoverDefinition[] SOLAR_PANEL_TIERED;

    static {
        GTRegistries.COVERS.unfreeze();
        SOLAR_PANEL_TIERED = registerPhoenixTiered(
                "solar_panel",
                PhoenixCoverSolarPanel::new,
                PHOENIX_SOLAR_PANEL_TIERS);
        GTRegistries.COVERS.freeze();
    }

    public static void init() {}

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
