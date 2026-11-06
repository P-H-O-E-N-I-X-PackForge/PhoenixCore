package net.phoenix.core.integration.conflux.research;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.phoenix.core.utils.TeamUtils;

import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Research that makes machines run faster or slower, or draw more or less power. A node gives it as an unlock of type
 * {@code recipe_modifier}, and its value reads
 *
 * <pre>
 *     &lt;stat&gt; &lt;amount&gt; [&lt;selector&gt;]
 * </pre>
 *
 * where the stat is {@code duration} (how long a recipe takes), {@code speed} (the same, the other way round: speed 2
 * halves the time) or {@code eut} (the power it draws); the amount is a multiplier ({@code 0.8}, {@code x0.8}) or a
 * change in percent ({@code -20%}); and the selector is a machine id, a recipe type id, a namespace followed by
 * {@code :*}, or {@code *} for everything (the default). So {@code "duration 0.8 gtceu:macerator"} makes macerators
 * 20% quicker and {@code "eut 0.9 *"} takes a tenth off every machine's power draw. Effects from every node a team has
 * researched stack by multiplying, and apply to the machines the team's members placed.
 */
public final class ConfluxRecipeEffects {

    private ConfluxRecipeEffects() {}

    public enum Stat {
        DURATION,
        EUT
    }

    public record Effect(Stat stat, double multiplier, String selector) {

        boolean matches(String machineId, String recipeTypeId) {
            if (selector.equals("*")) return true;
            if (selector.endsWith(":*")) {
                String ns = selector.substring(0, selector.length() - 1);
                return machineId.startsWith(ns) || recipeTypeId.startsWith(ns);
            }
            return selector.equals(machineId) || selector.equals(recipeTypeId);
        }
    }

    /** Reads an unlock's value; null when it does not make sense. */
    @Nullable
    public static Effect parse(String value) {
        if (value == null) return null;
        String[] parts = value.trim().split("\\s+");
        if (parts.length < 2) return null;
        String stat = parts[0].toLowerCase(Locale.ROOT);
        String amountText = parts[1].toLowerCase(Locale.ROOT);
        String selector = parts.length > 2 ? parts[2] : "*";

        double amount;
        try {
            if (amountText.endsWith("%")) {
                amount = 1.0 + Double.parseDouble(amountText.substring(0, amountText.length() - 1)) / 100.0;
            } else {
                amount = Double.parseDouble(amountText.startsWith("x") ? amountText.substring(1) : amountText);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        if (!(amount > 0.0) || Double.isInfinite(amount)) return null;

        return switch (stat) {
            case "duration", "time" -> new Effect(Stat.DURATION, amount, selector);
            case "speed" -> new Effect(Stat.DURATION, 1.0 / amount, selector);
            case "eut", "power", "energy" -> new Effect(Stat.EUT, amount, selector);
            default -> null;
        };
    }

    /** A sentence for the research screen: "Macerator recipes 20% faster". */
    public static String describe(String value) {
        Effect e = parse(value);
        if (e == null) return value;
        String what = e.selector.equals("*") ? "All machines" : pretty(e.selector) + " machines";
        double change = Math.abs(1.0 - e.multiplier) * 100.0;
        String amount = String.format(Locale.ROOT, Math.abs(change - Math.rint(change)) < 1e-6 ? "%.0f%%" : "%.1f%%",
                change);
        if (e.stat == Stat.DURATION) {
            return what + " " + (e.multiplier < 1.0 ? "run " + amount + " faster" : "run " + amount + " slower");
        }
        return what + " use " + amount + (e.multiplier < 1.0 ? " less" : " more") + " power";
    }

    private static String pretty(String selector) {
        String path = selector.contains(":") ? selector.substring(selector.indexOf(':') + 1) : selector;
        if (path.equals("*")) return selector.substring(0, selector.indexOf(':'));
        String spaced = path.replace('_', ' ');
        return spaced.isEmpty() ? selector : Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    // ------------------------------------------------------------------------------------------ for a team

    private record Cached(int signature, List<Effect> effects) {}

    private static final Map<UUID, Cached> CACHE = new ConcurrentHashMap<>();

    /** Forget what was worked out, when the research trees reload. */
    public static void invalidate() {
        CACHE.clear();
    }

    private static List<Effect> effectsOf(UUID team, ServerLevel level) {
        WorldResearchData data = WorldResearchData.get(level);
        Set<ResourceLocation> unlocked = data.getUnlocked(team);
        if (unlocked.isEmpty()) return List.of();
        // the same set of nodes gives the same effects, so only work them out again when the set changes
        int signature = unlocked.hashCode() * 31 + ResearchTreeRegistry.INSTANCE.getAllNodes().size();
        Cached cached = CACHE.get(team);
        if (cached != null && cached.signature == signature) return cached.effects;

        List<Effect> effects = new ArrayList<>();
        for (ResourceLocation nodeId : unlocked) {
            ResearchNode node = ResearchTreeRegistry.INSTANCE.getNode(nodeId).orElse(null);
            if (node == null) continue;
            for (ResearchUnlock unlock : node.unlocks) {
                if (!"recipe_modifier".equals(unlock.type())) continue;
                Effect e = parse(unlock.value());
                if (e != null) effects.add(e);
            }
        }
        List<Effect> result = List.copyOf(effects);
        CACHE.put(team, new Cached(signature, result));
        return result;
    }

    /** The combined multiplier of a stat for this team on this machine and recipe type. */
    public static double multiplier(UUID team, ServerLevel level, Stat stat, String machineId, String recipeTypeId) {
        double result = 1.0;
        for (Effect e : effectsOf(team, level)) {
            if (e.stat == stat && e.matches(machineId, recipeTypeId)) result *= e.multiplier;
        }
        return result;
    }

    // ------------------------------------------------------------------------------------------ in the machines

    /** Hooked onto every machine's own recipe modifier, so research reaches every recipe GregTech runs. */
    public static final RecipeModifier MODIFIER = (MetaMachine machine, GTRecipe recipe) -> {
        if (!(machine.getLevel() instanceof ServerLevel level)) return ModifierFunction.IDENTITY;
        UUID owner = machine.getOwnerUUID();
        if (owner == null) return ModifierFunction.IDENTITY;
        UUID team = TeamUtils.getTeamIdOrPlayerFallback(owner);
        if (team == null) return ModifierFunction.IDENTITY;

        String machineId = machine.getDefinition().getId().toString();
        String recipeType = recipe.recipeType.id.toString();
        double duration = multiplier(team, level, Stat.DURATION, machineId, recipeType);
        double eut = multiplier(team, level, Stat.EUT, machineId, recipeType);
        if (duration == 1.0 && eut == 1.0) return ModifierFunction.IDENTITY;

        var builder = ModifierFunction.builder();
        if (duration != 1.0) builder.durationMultiplier(duration);
        if (eut != 1.0) builder.eutMultiplier(eut);
        return builder.build();
    };
}
