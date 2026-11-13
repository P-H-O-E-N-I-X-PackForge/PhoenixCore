package net.phoenix.core.integration.recipe_helper;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.phoenix.conflux.research.ConfluxResearchCondition;
import net.phoenix.conflux.research.GatedRecipeRegistry;
import net.phoenix.conflux.research.GatedRecipeRule;
import net.phoenix.conflux.research.ResearchTreeRegistry;
import net.phoenix.core.PhoenixCore;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public final class RecipeDump {

    private RecipeDump() {}

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final int INF = 99;

    private static final int MAX_OPTIONS = 40;

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("phoenixrecipes").requires(s -> s.hasPermission(2))
                .then(Commands.literal("dump").executes(c -> dump(c.getSource()))));
    }

    static final class Node {

        final String id;
        final String type;
        final String kind;
        final long eut;
        final int duration;
        final String inputs;
        final String outputs;
        final String gate;
        final String gateSource;
        final String questSource;
        final String machineGate;

        final List<List<Integer>> ins = new ArrayList<>();
        final List<Integer> outs = new ArrayList<>();
        final boolean machine;

        int tier;
        int need = INF;
        String needBy = "";

        static Node of(String id, boolean machine, int tier, List<List<Integer>> ins, List<Integer> outs) {
            Node n = new Node(id, "test", machine ? "machine" : "crafting", machine, tier, 0, 0, "", "", "", "", "",
                    "");
            n.ins.addAll(ins);
            n.outs.addAll(outs);
            return n;
        }

        Node(String id, String type, String kind, boolean machine, int tier, long eut, int duration, String inputs,
             String outputs, String gate, String gateSource, String questSource, String machineGate) {
            this.id = id;
            this.type = type;
            this.kind = kind;
            this.machine = machine;
            this.tier = tier;
            this.eut = eut;
            this.duration = duration;
            this.inputs = inputs;
            this.outputs = outputs;
            this.gate = gate;
            this.gateSource = gateSource;
            this.questSource = questSource;
            this.machineGate = machineGate;
        }
    }

    static final class Items {

        final Map<String, Integer> index = new HashMap<>();

        int of(String id) {
            return index.computeIfAbsent(id, k -> index.size());
        }
    }

    private static int dump(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        List<Node> nodes = new ArrayList<>();
        Items items = new Items();
        Map<String, String> flagSources = flagSources();
        Map<String, String> questSources = questSources();
        Map<ResourceLocation, List<String>> machineGates = machineGates();
        var registry = server.registryAccess();

        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            try {
                Recipe<?> recipe = holder.value();
                nodes.add(recipe instanceof GTRecipe gt ?
                        machineNode(holder.id(), gt, items, flagSources, questSources, machineGates) :
                        otherNode(holder, registry, items, flagSources, questSources));
            } catch (Throwable t) {
                PhoenixCore.LOGGER.warn("[recipe dump] skipped {}: {}", holder.id(), t.toString());
            }
        }
        analyse(nodes, items.index.size());

        nodes.sort(Comparator.<Node>comparingInt(n -> n.tier < 0 || n.tier >= INF ? 99 : n.tier)
                .thenComparing(n -> n.type).thenComparing(n -> n.id));

        Path dir = FMLPaths.GAMEDIR.get().resolve("phoenix_dumps");
        Path file = dir.resolve("recipes-" + LocalDateTime.now().format(STAMP) + ".csv");
        try {
            Files.createDirectories(dir);
            try (Writer out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                out.write("id,type,kind,possible_tier,needed_tier,slack_tiers,needed_by,eut,duration_ticks,inputs," +
                        "outputs,gate_flag,gate_unlocked_by,gate_quest,machine_gated_by\n");
                for (Node n : nodes) {
                    boolean known = n.tier >= 0 && n.tier < INF;
                    boolean needed = n.need >= 0 && n.need < INF;
                    out.write(
                            String.join(",", csv(n.id), csv(n.type), csv(n.kind), csv(known ? GTValues.VN[n.tier] : ""),
                                    csv(needed ? GTValues.VN[n.need] : ""),
                                    known && needed ? Integer.toString(n.need - n.tier) : "", csv(n.needBy),
                                    Long.toString(n.eut), Integer.toString(n.duration), csv(n.inputs), csv(n.outputs),
                                    csv(n.gate), csv(n.gateSource), csv(n.questSource), csv(n.machineGate)));
                    out.write('\n');
                }
            }
        } catch (IOException e) {
            source.sendFailure(Component.literal("Could not write the dump: " + e.getMessage()));
            return 0;
        }

        Map<String, Integer> perTier = new TreeMap<>();
        int gated = 0;
        int machine = 0;
        int slack = 0;
        for (Node n : nodes) {
            if (n.machine) machine++;
            boolean known = n.tier >= 0 && n.tier < INF;
            perTier.merge(known ? String.format("%02d %s", n.tier, GTValues.VN[n.tier]) : "no tier", 1, Integer::sum);
            if (!n.gate.isEmpty()) gated++;
            if (known && n.need < INF && n.need > n.tier) slack++;
        }
        final int fMachine = machine;
        final int fGated = gated;
        final int fSlack = slack;
        source.sendSuccess(() -> Component.literal("Wrote " + nodes.size() + " recipes (" + fMachine + " machine) to " +
                file + ". " + fGated + " are research-gated, " + fSlack +
                " are possible earlier than they are needed."),
                false);
        PhoenixCore.LOGGER.info("[recipe dump] {} recipes, per tier: {}", nodes.size(), perTier);
        perTier.forEach((tier, n) -> source.sendSuccess(() -> Component.literal("  " + tier + ": " + n), false));
        return nodes.size();
    }

    static void analyse(List<Node> nodes, int itemCount) {
        int[] possible = new int[itemCount];
        Arrays.fill(possible, INF);
        boolean[] produced = new boolean[itemCount];
        for (Node n : nodes) for (int o : n.outs) produced[o] = true;
        for (int i = 0; i < itemCount; i++) if (!produced[i]) possible[i] = 0;
        for (Node n : nodes) {
            if (!n.machine) n.tier = INF;
        }

        for (int round = 0; round < 24; round++) {
            boolean changed = false;
            for (Node n : nodes) {
                if (!n.machine) {
                    int tier = 0;
                    for (List<Integer> options : n.ins) {
                        int best = INF;
                        for (int opt : options) best = Math.min(best, possible[opt]);
                        tier = Math.max(tier, options.isEmpty() ? 0 : best);
                    }
                    if (tier != n.tier) {
                        n.tier = tier;
                        changed = true;
                    }
                }
                if (n.tier >= INF) continue;
                for (int o : n.outs) {
                    if (n.tier < possible[o]) {
                        possible[o] = n.tier;
                        changed = true;
                    }
                }
            }
            if (!changed) break;
        }

        boolean[] consumed = new boolean[itemCount];
        for (Node n : nodes) for (List<Integer> options : n.ins) for (int opt : options) consumed[opt] = true;
        int[] itemNeed = new int[itemCount];
        int[] itemBy = new int[itemCount];
        for (Node n : nodes) n.need = INF;

        for (int round = 0; round < 40; round++) {
            Arrays.fill(itemNeed, INF);
            Arrays.fill(itemBy, -1);
            for (int c = 0; c < nodes.size(); c++) {
                Node consumer = nodes.get(c);
                if (consumer.tier >= INF) continue;

                int value = Math.max(consumer.tier, Math.min(consumer.need, INF - 1));
                if (consumer.need >= INF) continue;
                for (List<Integer> options : consumer.ins) {
                    for (int opt : options) {
                        if (value < itemNeed[opt] || (value == itemNeed[opt] && itemBy[opt] < 0)) {
                            itemNeed[opt] = value;
                            itemBy[opt] = c;
                        }
                    }
                }
            }
            boolean changed = false;
            for (Node n : nodes) {
                if (n.tier >= INF) continue;
                int best = INF;
                String by = "";
                for (int o : n.outs) {
                    int v;
                    String who = "";
                    if (!consumed[o]) {
                        v = n.tier;
                    } else {
                        v = itemNeed[o];
                        if (itemBy[o] >= 0) who = nodes.get(itemBy[o]).id;
                    }
                    if (v < best) {
                        best = v;
                        by = who;
                    }
                }
                if (best < n.need) {
                    n.need = best;
                    n.needBy = by;
                    changed = true;
                }
            }
            if (!changed) break;
        }
    }

    private static Node machineNode(ResourceLocation id, GTRecipe recipe, Items items, Map<String, String> flagSources,
                                    Map<String, String> questSources,
                                    Map<ResourceLocation, List<String>> machineGates) {
        long voltage = Math.max(recipe.getInputEUt().voltage(), recipe.getOutputEUt().voltage());
        long eut = Math.max(recipe.getInputEUt().getTotalEU(), recipe.getOutputEUt().getTotalEU());
        int tier = voltage <= 0 ? 0 : GTUtil.getTierByVoltage(voltage);
        ResourceLocation type = recipe.recipeType.id;

        List<String> flags = new ArrayList<>();
        for (var condition : recipe.conditions) {
            if (condition instanceof ConfluxResearchCondition c && !c.getFlag().isEmpty()) {
                String scope = !c.getOnly().isEmpty() ? " (only " + c.getOnly() + ")" :
                        !c.getExcept().isEmpty() ? " (not " + c.getExcept() + ")" : "";
                flags.add(c.getFlag() + scope);
            }
        }
        Node node = new Node(id.toString(), type.toString(), "machine", true, tier, eut, recipe.duration,
                contents(recipe, true, null, items), contents(recipe, false, null, items), String.join("; ", flags),
                sources(flags, flagSources), sources(flags, questSources),
                String.join("; ", machineGates.getOrDefault(type, List.of())));
        contents(recipe, true, node.ins, items);
        List<List<Integer>> outs = new ArrayList<>();
        contents(recipe, false, outs, items);
        for (List<Integer> o : outs) node.outs.addAll(o);
        return node;
    }

    private static Node otherNode(RecipeHolder<?> holder, net.minecraft.core.HolderLookup.Provider registry,
                                  Items items, Map<String, String> flagSources, Map<String, String> questSources) {
        Recipe<?> recipe = holder.value();
        ItemStack result = recipe.getResultItem(registry);
        String type = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType()).toString();
        StringBuilder in = new StringBuilder();
        List<List<Integer>> ins = new ArrayList<>();
        for (var ingredient : recipe.getIngredients()) {
            ItemStack[] options = ingredient.getItems();
            if (options.length == 0) continue;
            if (in.length() > 0) in.append(" | ");
            in.append(name(options[0]));
            if (options.length > 1) in.append(" (+").append(options.length - 1).append(" more)");
            List<Integer> ids = new ArrayList<>();
            for (int i = 0; i < Math.min(options.length, MAX_OPTIONS); i++) {
                ids.add(items.of(BuiltInRegistries.ITEM.getKey(options[i].getItem()).toString()));
            }
            ins.add(ids);
        }

        List<String> flags = new ArrayList<>();
        String node = ResearchTreeRegistry.INSTANCE.getGatedRecipes().get(holder.id());
        if (node != null) flags.add(node);
        for (GatedRecipeRule rule : GatedRecipeRegistry.INSTANCE.getRules()) {
            if (rule.crafting() && rule.namesCraftingRecipe(holder.id(), result) && !rule.flag().isEmpty()) {
                String scope = !rule.onlyDisciplines().isEmpty() ?
                        " (only " + String.join(",", rule.onlyDisciplines()) +
                                ")" :
                        !rule.exceptDisciplines().isEmpty() ? " (not " + String.join(",", rule.exceptDisciplines()) +
                                ")" : "";
                flags.add(rule.flag() + scope);
            }
        }
        String kind = type.endsWith("crafting_shaped") || type.endsWith("crafting_shapeless") ? "crafting" : "other";
        Node out = new Node(holder.id().toString(), type, kind, false, INF, 0, 0, in.toString(), name(result),
                String.join("; ", flags), sources(flags, flagSources), sources(flags, questSources), "");
        out.ins.addAll(ins);
        if (!result.isEmpty()) out.outs.add(items.of(BuiltInRegistries.ITEM.getKey(result.getItem()).toString()));
        return out;
    }

    private static String contents(GTRecipe recipe, boolean input, List<List<Integer>> ids, Items items) {
        List<String> parts = new ArrayList<>();
        try {
            var itemContents = input ? recipe.getInputContents(ItemRecipeCapability.CAP) :
                    recipe.getOutputContents(ItemRecipeCapability.CAP);
            for (var content : itemContents) {
                ItemStack[] stacks = ItemRecipeCapability.CAP.of(content.content()).getItems();
                if (stacks.length == 0) continue;
                parts.add(name(stacks[0]) + (stacks.length > 1 ? " (+" + (stacks.length - 1) + " more)" : "") +
                        (content.chance() < content.maxChance() ?
                                " @" + content.chance() * 100 / content.maxChance() + "%" :
                                ""));
                if (ids != null) {
                    List<Integer> options = new ArrayList<>();
                    for (int i = 0; i < Math.min(stacks.length, MAX_OPTIONS); i++) {
                        options.add(items.of(BuiltInRegistries.ITEM.getKey(stacks[i].getItem()).toString()));
                    }
                    ids.add(options);
                }
            }
            var fluidContents = input ? recipe.getInputContents(FluidRecipeCapability.CAP) :
                    recipe.getOutputContents(FluidRecipeCapability.CAP);
            for (var content : fluidContents) {
                var stacks = FluidRecipeCapability.CAP.of(content.content()).getFluids();
                if (stacks.length == 0) continue;
                String fluid = BuiltInRegistries.FLUID.getKey(stacks[0].getFluid()).toString();
                parts.add(stacks[0].getAmount() + "mB " + fluid);
                if (ids != null) ids.add(new ArrayList<>(List.of(items.of("fluid:" + fluid))));
            }
        } catch (Throwable t) {
            parts.add("?");
        }
        return String.join(" | ", parts);
    }

    private static String name(ItemStack stack) {
        if (stack.isEmpty()) return "";
        return stack.getCount() + "x " + BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    private static Map<String, String> flagSources() {
        Map<String, String> out = new LinkedHashMap<>();
        for (var tree : ResearchTreeRegistry.INSTANCE.getAllTrees()) {
            for (var node : tree.getNodes()) {
                for (var unlock : node.unlocks) {
                    String flag = switch (unlock.type()) {
                        case "flag" -> unlock.value();
                        case "recipe" -> ResearchTreeRegistry.recipeFlag(unlock.value());
                        default -> null;
                    };
                    if (flag != null) out.merge(flag, tree.title + " > " + node.title, (a, b) -> a + "; " + b);
                }
            }
        }
        return out;
    }

    private static Map<String, String> questSources() {
        Map<String, String> out = new LinkedHashMap<>();
        Path root = FMLPaths.CONFIGDIR.get().resolve("phoenix_chronicles").resolve("quests");
        if (!Files.isDirectory(root)) return out;
        Pattern flag = Pattern.compile("\"flag\"\\s*:\\s*\"([^\"]+)\"");
        try (var walk = Files.walk(root)) {
            for (Path p : (Iterable<Path>) walk.filter(f -> f.toString().endsWith(".snbt"))::iterator) {
                String text = Files.readString(p, StandardCharsets.UTF_8);
                int at = 0;
                while ((at = text.indexOf("conflux_unlock", at)) >= 0) {
                    Matcher m = flag.matcher(text.substring(at, Math.min(text.length(), at + 400)));
                    if (m.find()) {
                        String quest = p.getFileName().toString().replace(".snbt", "");
                        out.merge(m.group(1), quest, (a, b) -> a.contains(b) ? a : a + "; " + b);
                    }
                    at += 14;
                }
            }
        } catch (IOException e) {
            PhoenixCore.LOGGER.warn("[recipe dump] could not read the quest files: {}", e.toString());
        }
        return out;
    }

    private static Map<ResourceLocation, List<String>> machineGates() {
        Map<ResourceLocation, List<String>> out = new LinkedHashMap<>();
        for (ResourceLocation id : ResearchTreeRegistry.INSTANCE.getGatedMultiblocks()) {
            var definition = GTRegistries.MACHINES.get(id);
            if (definition == null || definition.getRecipeTypes() == null) continue;
            for (var type : definition.getRecipeTypes()) {
                if (type != null) out.computeIfAbsent(type.id, k -> new ArrayList<>()).add(id.toString());
            }
        }
        return out;
    }

    private static String sources(List<String> flags, Map<String, String> map) {
        List<String> found = new ArrayList<>();
        for (String f : flags) {
            String bare = f.contains(" (") ? f.substring(0, f.indexOf(" (")) : f;
            String s = map.get(bare);
            if (s != null) found.add(bare + ": " + s);
        }
        return String.join("; ", found);
    }

    private static String csv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
