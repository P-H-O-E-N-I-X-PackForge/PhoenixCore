package net.phoenix.core.integration.emi;

import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.data.PTags;
import net.phoenix.core.common.machine.multiblock.cinder.CinderForgeMachines;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import java.util.ArrayList;
import java.util.List;

public final class CinderEmi {

    private CinderEmi() {}

    private static EmiStack forge() {
        return EmiStack.of(CinderForgeMachines.CINDER_FORGE.asStack());
    }

    public static EmiRecipeCategory TARGETS;

    public static void register(EmiRegistry registry) {
        if (CinderForgeMachines.CINDER_FORGE == null) return;
        TARGETS = new EmiRecipeCategory(ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "cinder_targets"),
                forge()) {

            @Override
            public Component getName() {
                return Component.literal("Cinder Forge: what it builds");
            }
        };
        registry.addCategory(TARGETS);
        registry.addWorkstation(TARGETS, forge());
        for (MachineDefinition candidate : GTRegistries.MACHINES) {
            if (!(candidate instanceof MultiblockMachineDefinition multiblock)) continue;
            if (multiblock == CinderForgeMachines.CINDER_FORGE.get()) continue;
            try {
                var supplier = multiblock.getStructurePatterns().get(MultiblockControllerMachine.DEFAULT_STRUCTURE);
                if (supplier == null || !(supplier.get() instanceof BlockPattern)) continue;
            } catch (Throwable t) {
                continue;
            }
            int tier = multiblock.getBlock().defaultBlockState().is(PTags.CINDER_FORGE_TIER1_ALLOWED) ? 1 :
                    multiblock.getBlock().defaultBlockState().is(PTags.CINDER_FORGE_TIER2_ALLOWED) ? 2 : 3;
            registry.addRecipe(new TargetRecipe(multiblock, tier));
        }
    }

    private static final class TargetRecipe extends BasicEmiRecipe {

        private static final int ACCENT = 0xFFFF8A3D;

        private final EmiStack machine;
        private final String name;
        private final int tier;

        private final List<EmiPage.Line> needs = new ArrayList<>();

        TargetRecipe(MultiblockMachineDefinition def, int tier) {
            super(TARGETS, ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID,
                    "/cinder_targets/" + def.getId().getNamespace() + "/" + def.getId().getPath()), 176, 70);
            this.machine = EmiStack.of(new ItemStack(def.getBlock().asItem()));
            this.name = def.getBlock().getName().getString();
            this.tier = tier;
            this.outputs = List.of(machine);
            this.catalysts = List.of(forge());
            if (tier == 1) {
                needs.add(EmiPage.Line.of("✔ No ME network: a small forge makes it by hand", EmiPage.GOOD));
            } else {
                needs.add(
                        EmiPage.Line.of("◦ A Cinder Forge ME Interface hatch, to connect the forge to your ME network",
                                EmiPage.TEXT));
                needs.add(EmiPage.Line.of("◦ A Core on the network, the item that says what to build", EmiPage.TEXT));
            }
            this.height = 36 + EmiPage.ROW_H + 1 + EmiPage.measure(needs, 176) + 4;
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            EmiPage.frame(widgets, 176, height, ACCENT);
            EmiPage.title(widgets, name, 0, 1, 176, 0xFFFFFFFF);

            widgets.addSlot(forge(), 0, 14);
            EmiPage.row(widgets, "builds", 24, 19, 26, EmiPage.DIM, null);
            widgets.addSlot(machine, 52, 14).recipeContext(this);

            int tierColor = tier == 1 ? EmiPage.GOOD : tier == 2 ? EmiPage.WARM : EmiPage.BAD;
            EmiPage.pill(widgets, tier == 1 ? "Tier 1 and up" : tier == 2 ? "Tier 2 and up" : "Tier 3 only", 80, 17,
                    tierColor);

            int y = EmiPage.section(widgets, "Needs", 0, 36, 176, ACCENT);
            EmiPage.draw(widgets, needs, 0, y, 176);
        }
    }
}
