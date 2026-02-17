package net.phoenix.core.common.block;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.block.ActiveBlock;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.api.block.IFissionFuelRodType;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@ParametersAreNonnullByDefault
public class FissionFuelRodBlock extends ActiveBlock {

    /**
     * Needed for tinting (BlockColor/ItemColor) and general introspection.
     */
    private final IFissionFuelRodType fuelRodType;

    public FissionFuelRodBlock(Properties props, IFissionFuelRodType type) {
        super(props);
        this.fuelRodType = type;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level,
                                List<Component> tooltip, TooltipFlag flag) {
        if (!GTUtil.isShiftDown()) {
            tooltip.add(Component.literal("Hold §fShift§7 for details")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        Component fuelName = getRegistryDisplayName(fuelRodType.getFuelKey());

        tooltip.add(Component.translatable("phoenix.fission.fuel_required", fuelName)
                .withStyle(ChatFormatting.AQUA));

        Component outputName = getRegistryDisplayName(fuelRodType.getOutputKey());

        tooltip.add(Component.translatable("phoenix.fission.depleted_fuel", outputName)
                .withStyle(ChatFormatting.DARK_GREEN));

        tooltip.add(Component.translatable("phoenix.fission.heat_production",
                Component.literal(String.valueOf(fuelRodType.getBaseHeatProduction()))
                        .withStyle(ChatFormatting.RED))
                .append(Component.literal(" HU/t").withStyle(ChatFormatting.GRAY)));

        double seconds = fuelRodType.getDurationTicks() / 20.0;
        tooltip.add(Component.translatable("phoenix.fission.fuel_cycle",
                Component.literal(String.valueOf(fuelRodType.getAmountPerCycle()))
                        .withStyle(ChatFormatting.WHITE),
                Component.literal(String.format("%.2f", seconds))
                        .withStyle(ChatFormatting.GOLD))
                .withStyle(ChatFormatting.GRAY));

        // NEW: neutron bias (affects blanket output distribution)
        int bias = 0;
        try {
            bias = fuelRodType.getNeutronBias();
        } catch (Throwable ignored) {}
        tooltip.add(Component.translatable("phoenix.fission.neutron_bias",
                Component.literal((bias >= 0 ? "+" : "") + bias + "%")
                        .withStyle(bias >= 0 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.BLUE))
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.translatable("gtceu.tooltip.tier",
                Component.literal(GTValues.VNF[fuelRodType.getTier()])
                        .withStyle(ChatFormatting.DARK_PURPLE)));
    }

    /**
     * PURE registry lookup:
     * - if key is an item id, show item name
     * - else if key is a fluid id, show fluid name
     * - else show raw key
     */
    public static Component getRegistryDisplayName(@NotNull String key) {
        ResourceLocation rl = ResourceLocation.tryParse(key);
        if (rl == null) return Component.literal(key).withStyle(ChatFormatting.YELLOW);

        Item item = ForgeRegistries.ITEMS.getValue(rl);
        if (item != null && item != Items.AIR) {
            return item.getName(new ItemStack(item));
        }

        Fluid fluid = ForgeRegistries.FLUIDS.getValue(rl);
        if (fluid != null && fluid != Fluids.EMPTY) {
            return Component.translatable(fluid.getFluidType().getDescriptionId());
        }

        return Component.literal(key).withStyle(ChatFormatting.YELLOW);
    }

    public enum FissionFuelRodTypes implements StringRepresentable, IFissionFuelRodType {

        // Example: uranium fuel rod has neutral bias
        URANIUM("uranium_fuel_rod",
                500, 1,
                1200, 1,
                "gtceu:uranium_nugget",
                "gtceu:plutonium_nugget",
                0xFF7DE7FF,
                4);

        @Getter
        @NotNull
        private final String name;
        @Getter
        private final int baseHeatProduction;
        @Getter
        private final int tier;
        @Getter
        private final int durationTicks;
        @Getter
        private final int amountPerCycle;
        @Getter
        @NotNull
        private final String fuelKey;
        @Getter
        @NotNull
        private final ResourceLocation texture;
        @Getter
        @NotNull
        private final String outputKey;

        /** NEW: shifts blanket breeding distribution toward higher instability outputs */
        @Getter
        private final int neutronBias;

        /** Case-by-case ARGB tint. Packdevs choose this. */
        @Getter
        private final int tintColor;

        FissionFuelRodTypes(String name, int heat, int tier, int duration, int amount,
                            String fuelKey, String outputKey, int tintColor,
                            int neutronBias) {
            this.name = name;
            this.baseHeatProduction = heat;
            this.tier = tier;
            this.durationTicks = duration;
            this.amountPerCycle = amount;
            this.fuelKey = fuelKey;
            this.texture = PhoenixCore.id("block/fission/fuel_rod/" + name);
            this.tintColor = tintColor;
            this.outputKey = outputKey;
            this.neutronBias = neutronBias;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }

        @Override
        public int getTintColor() {
            return tintColor;
        }
    }
}
