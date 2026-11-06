package net.phoenix.core.integration.emi;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.data.recipe.CustomTags;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.conflux.ConfluxDataType;
import net.phoenix.core.integration.conflux.ConfluxRegistry;
import net.phoenix.core.integration.conflux.client.ConfluxTheme;
import net.phoenix.core.integration.conflux.producer.ConfluxProducerMachine;
import net.phoenix.core.integration.conflux.producer.ConfluxProducerMachines;
import net.phoenix.core.integration.conflux.terminal.ConfluxTerminalRegistry;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Conflux data producers: one page per type with the machines of every tier, what the type eats and how much, and
 * how to carry what it makes. Each type eats its own thing, so the page is where that is said.
 */
public final class ProducerEmi {

    private ProducerEmi() {}

    private static final int W = 220;
    private static final int[] TIERS = { GTValues.LV, GTValues.MV, GTValues.HV, GTValues.EV, GTValues.IV, GTValues.LuV,
            GTValues.ZPM, GTValues.UV };

    private static EmiStack icon() {
        for (ConfluxDataType type : ConfluxDataType.values()) {
            var entry = ConfluxProducerMachines.PRODUCERS.get(type);
            if (entry == null) continue;
            for (var machine : entry) if (machine != null) return EmiStack.of(machine.asStack());
        }
        return EmiStack.EMPTY;
    }

    public static final EmiRecipeCategory PRODUCERS = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "conflux_producers"), icon()) {

        @Override
        public Component getName() {
            return Component.literal("Conflux Data Producers");
        }
    };

    public static void register(EmiRegistry registry) {
        registry.addCategory(PRODUCERS);
        for (ConfluxDataType type : ConfluxDataType.values()) {
            var machines = ConfluxProducerMachines.PRODUCERS.get(type);
            if (machines == null) continue;
            for (var machine : machines) {
                if (machine != null) registry.addWorkstation(PRODUCERS, EmiStack.of(machine.asStack()));
            }
            registry.addRecipe(new TypePage(type));
        }
    }

    /** Things that show what the type eats: a few real items, or a tag that cycles through its members. */
    private static List<EmiIngredient> examples(ConfluxDataType type) {
        List<EmiIngredient> out = new ArrayList<>();
        switch (type) {
            case MATERIAL -> {
                for (var m : List.of(GTMaterials.Iron.get(), GTMaterials.Copper.get(), GTMaterials.Diamond.get())) {
                    for (TagPrefix prefix : List.of(TagPrefix.ingot.get(), TagPrefix.gem.get(),
                            TagPrefix.dust.get())) {
                        ItemStack s = ChemicalHelper.get(prefix, m, 1);
                        if (!s.isEmpty()) {
                            out.add(EmiStack.of(s));
                            break;
                        }
                    }
                }
            }
            case BIOLOGICAL -> out.add(EmiIngredient.of(ConfluxProducerMachine.BIOMATERIAL));
            case COMPUTATIONAL -> out.add(EmiIngredient.of(CustomTags.CIRCUITS));
            default -> {}
        }
        return out;
    }

    private static final class TypePage extends BasicEmiRecipe {

        private final ConfluxDataType type;
        private final int accent;
        private final List<EmiIngredient> examples;

        TypePage(ConfluxDataType type) {
            super(PRODUCERS, ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID,
                    "/conflux_producers/" + type.id()), W, 132);
            this.type = type;
            this.accent = ConfluxTheme.typeColor(type);
            this.examples = examples(type);
            List<EmiIngredient> in = new ArrayList<>(examples);
            this.inputs = in;
            List<EmiStack> cat = new ArrayList<>();
            for (var m : ConfluxProducerMachines.PRODUCERS.get(type)) if (m != null) cat.add(EmiStack.of(m.asStack()));
            this.catalysts = new ArrayList<>(cat);
            this.outputs = List.of(EmiStack.of(ConfluxRegistry.PIPES.get(type).get()));
            this.height = 82 + needHeight() + 3 + 12 + 20;
        }

        /** What the type eats is told in words that wrap, beside the example items if there are any. */
        private int textWidth() {
            return examples.isEmpty() ? W : W - examples.size() * 18 - 4;
        }

        private String needDetail() {
            return switch (type) {
                case ARCANE -> "Source: 2 per tick for each tier. Feed it with a Source pipe or a wireless relay.";
                case ENERGETIC -> "Electricity: 8 EU/t at LV, doubling each tier. The only producer that uses EU.";
                default -> ConfluxProducerMachine.needDetail(type, 1);
            };
        }

        private int needHeight() {
            return Math.max(examples.isEmpty() ? 0 : 18, ROW + EmiPage.wrapHeight(needDetail(), textWidth()));
        }

        private static final int ROW = 11;

        @Override
        public void addWidgets(WidgetHolder widgets) {
            EmiPage.frame(widgets, W, height, accent);
            EmiPage.title(widgets, type.displayName + " Data Producer", 0, 1, W, 0xFFFFFFFF);
            EmiPage.row(widgets, "Makes " + type.displayName.toLowerCase(Locale.ROOT) + " data from " +
                    ConfluxProducerMachine.needTitle(type).toLowerCase(Locale.ROOT), 0, 12, W, EmiPage.DIM, null);

            // the machines, every tier in a row
            int y = EmiPage.section(widgets, "Machines", 0, 24, W, accent);
            int x = 0;
            for (int tier : TIERS) {
                var machine = ConfluxProducerMachines.PRODUCERS.get(type)[tier];
                if (machine == null) continue;
                widgets.addSlot(EmiStack.of(machine.asStack()), x, y).drawBack(false);
                x += 18;
            }
            EmiPage.row(widgets, "Makes 4 u/t for each tier: LV 4, up to UV 32", 0, y + 20, W, EmiPage.TEXT,
                    List.of(Component.literal(
                            "Output is 4 x the tier, every tick it is running. The buffer and push rate grow with the tier too.")));

            // what it eats
            y = EmiPage.section(widgets, "Needs", 0, y + 34, W, accent);
            int row = y;
            int textX = 0;
            for (EmiIngredient e : examples) {
                widgets.addSlot(e, textX, row).drawBack(false);
                textX += 18;
            }
            if (!examples.isEmpty()) textX += 4;
            EmiPage.row(widgets, ConfluxProducerMachine.needTitle(type), textX, row + 1, W - textX, EmiPage.TEXT,
                    null);
            EmiPage.wrap(widgets, needDetail(), textX, row + 1 + ROW, textWidth(), EmiPage.DIM);
            row += needHeight();

            // where it goes
            row = EmiPage.section(widgets, "Carry it", 0, row + 3, W, accent);
            widgets.addSlot(EmiStack.of(ConfluxRegistry.PIPES.get(type).get()), 0, row).drawBack(false);
            widgets.addSlot(EmiStack.of(ConfluxTerminalRegistry.TERMINAL_ITEM.get()), 20, row).drawBack(false);
            EmiPage.row(widgets, "The " + type.displayName.toLowerCase(Locale.ROOT) + " pipe, into a Research Terminal",
                    42, row + 5, W - 42, EmiPage.TEXT, null);
        }
    }
}
