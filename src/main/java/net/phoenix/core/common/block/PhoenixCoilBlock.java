package net.phoenix.core.common.block;

import com.gregtechceu.gtceu.api.block.ActiveBlock;
import com.gregtechceu.gtceu.api.block.ICoilType;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.phoenix.core.PhoenixCore;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import static net.phoenix.core.common.data.materials.PhoenixProgressionMaterials.PHOENIX_ENRICHED_TRITANIUM;

@ParametersAreNonnullByDefault
public class PhoenixCoilBlock extends ActiveBlock {

    public ICoilType coilType;

    public PhoenixCoilBlock(Properties properties, ICoilType coilType) {
        super(properties);
        this.coilType = coilType;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (GTUtil.isShiftDown()) {
            int coilTier = coilType.getTier();
            tooltip.add(Component.translatable("block.gtceu.wire_coil.tooltip_heat", coilType.getCoilTemperature()));
            tooltip.add(Component.translatable("block.gtceu.wire_coil.tooltip_smelter"));
            tooltip.add(
                    Component.translatable("block.gtceu.wire_coil.tooltip_parallel_smelter", coilType.getLevel() * 32));
            tooltip.add(Component.translatable("block.gtceu.wire_coil.tooltip_energy_smelter",
                    Math.max(1, (4 * coilType.getLevel() * 32 / (8 * coilType.getEnergyDiscount())))));
            tooltip.add(Component.translatable("block.gtceu.wire_coil.tooltip_pyro"));
            tooltip.add(Component.translatable("block.gtceu.wire_coil.tooltip_speed_pyro",
                    coilTier == 0 ? 75 : 50 * (coilTier + 1)));
            tooltip.add(Component.translatable("block.gtceu.wire_coil.tooltip_cracking"));
            tooltip.add(Component.translatable("block.gtceu.wire_coil.tooltip_energy_cracking", 100 - 10 * coilTier));
        } else {
            tooltip.add(Component.translatable("block.gtceu.wire_coil.tooltip_extended_info"));
        }
    }

    public enum CoilType implements StringRepresentable, ICoilType {

        COIL_TRUE_HEAT_STABLE("true_heat_stable", 1, 1, 1, 1, PHOENIX_ENRICHED_TRITANIUM,
                PhoenixCore.id("block/true_heat_stable_coil_block"));

        @NotNull
        @Getter
        private final String name;
        @Getter
        private final int coilTemperature;

        @Getter
        private final int level;
        @Getter
        private final int energyDiscount;
        @Getter
        private final int tier;
        @NotNull
        @Getter
        private final Material material;
        @NotNull
        @Getter
        private final ResourceLocation texture;

        CoilType(String name, int coilTemperature, int level, int energyDiscount, int tier, Material material,
                 ResourceLocation texture) {
            this.name = name;
            this.coilTemperature = coilTemperature;
            this.level = level;
            this.energyDiscount = energyDiscount;
            this.tier = tier;
            this.material = material;
            this.texture = texture;
        }

        @NotNull
        @Override
        public int getTier() {
            return this.tier;
        }

        @NotNull
        @Override
        public String toString() {
            return getName();
        }

        @Override
        @NotNull
        public String getSerializedName() {
            return name;
        }
    }
}
