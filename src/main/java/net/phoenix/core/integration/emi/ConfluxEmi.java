package net.phoenix.core.integration.emi;

import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.conflux.ConfluxDataType;
import net.phoenix.core.integration.conflux.client.ClientResearchCache;
import net.phoenix.core.integration.conflux.client.ConfluxIcons;
import net.phoenix.core.integration.conflux.client.ConfluxTheme;
import net.phoenix.core.integration.conflux.research.ResearchNode;
import net.phoenix.core.integration.conflux.research.ResearchTree;
import net.phoenix.core.integration.conflux.research.ResearchTreeRegistry;
import net.phoenix.core.integration.conflux.research.ResearchUnlock;
import net.phoenix.core.integration.conflux.terminal.ConfluxTerminalRegistry;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ConfluxEmi {

    private ConfluxEmi() {}

    private static EmiStack terminal() {
        return EmiStack.of(new ItemStack(ConfluxTerminalRegistry.TERMINAL_ITEM.get()));
    }

    public static final EmiRecipeCategory RESEARCH = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "conflux_research"), terminal()) {

        @Override
        public Component getName() {
            return Component.literal("Conflux Research");
        }
    };

    private static final int W = 200;
    private static final int LABEL_W = 62;
    private static final int BAR_X = 66;
    private static final int BAR_W = 70;
    private static final int AMOUNT_X = 140;

    public static void register(EmiRegistry registry) {
        registry.addCategory(RESEARCH);
        registry.addWorkstation(RESEARCH, terminal());
        String mine = ClientResearchCache.getDisciplineInfo().disciplineId();
        for (ResearchTree tree : ResearchTreeRegistry.INSTANCE.getAllTrees()) {
            if (!tree.isVisibleTo(mine) || !tree.isOpenTo(ClientResearchCache::hasMilestone)) continue;
            for (ResearchNode node : tree.getNodes()) {
                if (node.hidden && !ClientResearchCache.isUnlocked(node.id)) continue;
                registry.addRecipe(new NodeRecipe(tree, node));
            }
        }
    }

    private static ItemStack resultOfRecipe(ResourceLocation id) {
        var level = Minecraft.getInstance().level;
        if (level == null) return ItemStack.EMPTY;
        var holder = level.getRecipeManager().byKey(id).orElse(null);
        if (holder == null) return ItemStack.EMPTY;
        if (holder.value() instanceof GTRecipe gt) {
            for (var content : gt.getOutputContents(ItemRecipeCapability.CAP)) {
                for (ItemStack stack : ItemRecipeCapability.CAP.of(content.content()).getItems()) {
                    if (!stack.isEmpty()) return stack;
                }
            }
            return ItemStack.EMPTY;
        }
        return holder.value().getResultItem(level.registryAccess());
    }

    private record UnlockRow(String kind, String label, int color, EmiStack stack, String detail) {}

    private static final class NodeRecipe extends BasicEmiRecipe {

        private final ResearchTree tree;
        private final ResearchNode node;
        private final List<EmiStack> unlockedStacks = new ArrayList<>();
        private final List<UnlockRow> unlockRows = new ArrayList<>();

        NodeRecipe(ResearchTree tree, ResearchNode node) {
            super(RESEARCH, ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID,
                    "/conflux_research/" + node.id.getNamespace() + "/" + node.id.getPath()), W, 40);
            this.tree = tree;
            this.node = node;
            Set<ItemStack> seen = new LinkedHashSet<>();
            for (ResearchUnlock unlock : node.unlocks) {
                ItemStack stack = ItemStack.EMPTY;
                String value = unlock.value();
                ResourceLocation id = ResourceLocation.tryParse(value);
                String kind;
                int color;
                switch (unlock.type()) {
                    case "multiblock" -> {
                        MachineDefinition def = id == null ? null : GTRegistries.MACHINES.get(id);
                        if (def != null) stack = new ItemStack(def.getBlock().asItem());
                        kind = "Multiblock";
                        color = 0xFFB0D0FF;
                    }
                    case "recipe" -> {
                        if (id != null) stack = resultOfRecipe(id);
                        kind = "Recipe";
                        color = 0xFFB0FFC8;
                    }
                    case "item" -> {
                        if (id != null) stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
                        kind = "Item";
                        color = 0xFFFFE0A0;
                    }
                    case "recipe_tag" -> {
                        kind = "Recipes";
                        color = 0xFFB0FFC8;
                    }
                    case "world_stage" -> {
                        kind = "World stage";
                        color = 0xFFFFB0B0;
                    }
                    case "archive" -> {
                        kind = "Archive entry";
                        color = 0xFFE0C8FF;
                    }
                    default -> {
                        continue;
                    }
                }
                final ItemStack found = stack;
                EmiStack emi = null;
                if (!found.isEmpty()) {
                    emi = EmiStack.of(found);
                    if (seen.stream().noneMatch(s -> ItemStack.isSameItemSameComponents(s, found))) {
                        seen.add(found);
                        unlockedStacks.add(emi);
                    }
                }
                String label = found.isEmpty() ? value : found.getHoverName().getString();
                unlockRows.add(new UnlockRow(kind, label, color, emi, value));
            }
            this.outputs = List.copyOf(unlockedStacks);
            this.catalysts = List.of(terminal());
            this.height = heightOf();
        }

        private int heightOf() {
            int h = 24 + 15;
            if (!node.cost.isEmpty()) h += EmiPage.ROW_H + 1 + node.cost.size() * EmiPage.ROW_H + 3;
            int needs = node.prerequisites.size() + node.requiresFlags.size() +
                    (node.prerequisitesAny.isEmpty() ? 0 : 1 + node.prerequisitesAny.size());
            if (needs > 0) h += EmiPage.ROW_H + 1 + needs * EmiPage.ROW_H + 3;
            if (!unlockRows.isEmpty()) {
                h += EmiPage.ROW_H + 1;
                for (UnlockRow row : unlockRows) h += row.stack() != null ? 19 : EmiPage.ROW_H;
            }
            return h + 2;
        }

        private static String nameOf(ResourceLocation id) {
            return ResearchTreeRegistry.INSTANCE.getNode(id).map(n -> n.title).orElse(id.getPath());
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            int accent = 0xFF000000 | (ConfluxTheme.disciplineAccent(tree.discipline) & 0xFFFFFF);
            EmiPage.frame(widgets, W, height, accent);

            ItemStack icon = ConfluxIcons.asItem(node.icon);
            widgets.addSlot(EmiStack.of(icon.isEmpty() ? new ItemStack(Items.BOOK) : icon), 0, 0).drawBack(false);
            EmiPage.title(widgets, node.title, 22, 2, W - 22, 0xFFFFFFFF);
            EmiPage.row(widgets, tree.title + (tree.discipline != null ? "  ·  " + tree.discipline : ""), 22, 13,
                    W - 22, EmiPage.DIM, null);

            boolean done = ClientResearchCache.isUnlocked(node.id);
            boolean lockedOut = ClientResearchCache.isLockedOut(node.id);
            boolean ready = !done && !lockedOut && node.canUnlock(ClientResearchCache.unlocked,
                    ClientResearchCache.lockedOut, ClientResearchCache::hasFlag);
            EmiPage.pill(widgets, done ? "Researched" : lockedOut ? "Locked out" : ready ? "Ready to research" :
                    "Not yet available", 0, 24, done ? EmiPage.GOOD : lockedOut ? EmiPage.BAD :
                            ready ? EmiPage.WARM :
                                    EmiPage.DIM);

            int y = 24 + 15;
            if (!node.cost.isEmpty()) {
                y = EmiPage.section(widgets, "Costs", 0, y, W, accent);
                for (Map.Entry<ConfluxDataType, Long> e : node.cost.entrySet()) {
                    long need = e.getValue();
                    long have = ClientResearchCache.dataStored(e.getKey());
                    boolean enough = have >= need;
                    EmiPage.row(widgets, e.getKey().displayComponent().getString(), 0, y, LABEL_W,
                            ConfluxTheme.typeColor(e.getKey()), null);
                    EmiPage.bar(widgets, BAR_X, y + 3, BAR_W, 4, need == 0 ? 1.0 : have / (double) need,
                            enough ? EmiPage.GOOD : EmiPage.BAD);
                    EmiPage.row(widgets, String.format("%,d", need), AMOUNT_X, y, W - AMOUNT_X, EmiPage.TEXT, null);
                    widgets.addTooltipText(List.of(e.getKey().displayComponent(),
                            Component.literal(String.format("You have %,d of %,d", have, need))), 0, y - 1, W,
                            EmiPage.ROW_H);
                    y += EmiPage.ROW_H;
                }
                y += 3;
            }

            int needs = node.prerequisites.size() + node.requiresFlags.size() +
                    (node.prerequisitesAny.isEmpty() ? 0 : 1 + node.prerequisitesAny.size());
            if (needs > 0) {
                y = EmiPage.section(widgets, "Needs first", 0, y, W, accent);
                for (ResourceLocation id : node.prerequisites) y = prereq(widgets, id, 0, y);
                if (!node.prerequisitesAny.isEmpty()) {
                    EmiPage.row(widgets, "any one of:", 0, y, W, EmiPage.DIM, null);
                    y += EmiPage.ROW_H;
                    for (ResourceLocation id : node.prerequisitesAny) y = prereq(widgets, id, 8, y);
                }
                for (String flag : node.requiresFlags) {
                    boolean met = ClientResearchCache.hasFlag(flag);
                    EmiPage.row(widgets, (met ? "✔ " : "◦ ") + ClientResearchCache.flagLabel(flag), 0, y, W,
                            met ? EmiPage.GOOD : EmiPage.TEXT, List.of(Component.literal(flag)));
                    y += EmiPage.ROW_H;
                }
                y += 3;
            }

            if (!unlockRows.isEmpty()) {
                y = EmiPage.section(widgets, "Unlocks", 0, y, W, accent);
                for (UnlockRow row : unlockRows) {
                    if (row.stack() != null) {
                        widgets.addSlot(row.stack(), 0, y).recipeContext(this);
                        EmiPage.row(widgets, row.kind() + ": " + row.label(), 22, y + 5, W - 22, row.color(),
                                List.of(Component.literal(row.kind()), Component.literal(row.detail())));
                        y += 19;
                    } else {
                        EmiPage.row(widgets, row.kind() + ": " + row.label(), 0, y, W, row.color(),
                                List.of(Component.literal(row.kind()), Component.literal(row.detail())));
                        y += EmiPage.ROW_H;
                    }
                }
            }
        }

        private int prereq(WidgetHolder widgets, ResourceLocation id, int x, int y) {
            boolean met = ClientResearchCache.isUnlocked(id);
            EmiPage.row(widgets, (met ? "✔ " : "◦ ") + nameOf(id), x, y, W - x,
                    met ? EmiPage.GOOD : EmiPage.TEXT,
                    List.of(Component.literal(nameOf(id)),
                            Component.literal(met ? "Researched" : "Not researched yet")));
            return y + EmiPage.ROW_H;
        }
    }
}
