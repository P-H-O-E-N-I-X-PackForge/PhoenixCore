package net.phoenix.core.integration.continuum.common;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.phoenix.core.integration.continuum.ContinuumRegistry;
import net.phoenix.core.integration.continuum.data.ContinuumData;
import net.phoenix.core.integration.continuum.data.DiscoveryStage;
import net.phoenix.core.integration.continuum.item.ContinuumRocketItem;
import net.phoenix.core.utils.TeamUtils;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * {@code /continuumadmin}: operator tools for testing Continuum while the real progression (research, crafting,
 * the GT launch pad) does not exist yet. Everything acts on the sender's own team.
 */
final class ContinuumAdminCommands {

    private ContinuumAdminCommands() {}

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("continuumadmin")
                .requires(source -> source.hasPermission(2))
                .then(literal("rocket")
                        .then(literal("give").executes(ctx -> giveRocket(ctx.getSource())))
                        .then(literal("repair").executes(ctx -> setWear(ctx.getSource(), 0.0f)))
                        .then(literal("wear")
                                .then(argument("value", FloatArgumentType.floatArg(0.0f, 1.0f))
                                        .executes(ctx -> setWear(ctx.getSource(),
                                                FloatArgumentType.getFloat(ctx, "value")))))
                        .then(literal("upgrade")
                                .then(argument("upgrade", StringArgumentType.word())
                                        .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider
                                                .suggest(List.of(RocketStats.HULL_INTERLOCK, RocketStats.OVERDRIVE,
                                                        RocketStats.REINFORCED_HULL), builder))
                                        .then(argument("level", IntegerArgumentType.integer(0, 10))
                                                .executes(ctx -> upgrade(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "upgrade"),
                                                        IntegerArgumentType.getInteger(ctx, "level")))))))
                .then(literal("discover")
                        .then(argument("target", ResourceLocationArgument.id())
                                .suggests((ctx, builder) -> {
                                    ContinuumData.systems().forEach(s -> builder.suggest(s.id().toString()));
                                    ContinuumData.allBodies().forEach(b -> builder.suggest(b.id().toString()));
                                    return builder.buildFuture();
                                })
                                .then(argument("stage", StringArgumentType.word())
                                        .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider
                                                .suggest(List.of("unknown", "detected", "surveyed"), builder))
                                        .executes(ctx -> discover(ctx.getSource(),
                                                ResourceLocationArgument.getId(ctx, "target"),
                                                StringArgumentType.getString(ctx, "stage"))))))
                .then(literal("outpost")
                        .then(literal("grant")
                                .then(argument("body", ResourceLocationArgument.id())
                                        .suggests((ctx, builder) -> {
                                            ContinuumData.allBodies().forEach(b -> builder.suggest(b.id().toString()));
                                            return builder.buildFuture();
                                        })
                                        .then(argument("probes", IntegerArgumentType.integer(1, 64))
                                                .executes(ctx -> grantOutpost(ctx.getSource(),
                                                        ResourceLocationArgument.getId(ctx, "body"),
                                                        IntegerArgumentType.getInteger(ctx, "probes"))))))
                        .then(literal("fill").executes(ctx -> fillOutposts(ctx.getSource())))
                        .then(literal("damage").executes(ctx -> damageOutposts(ctx.getSource(), true)))
                        .then(literal("repair").executes(ctx -> damageOutposts(ctx.getSource(), false)))
                        .then(literal("clear").executes(ctx -> clearOutposts(ctx.getSource()))))
                .then(literal("reset").executes(ctx -> reset(ctx.getSource())))
                .then(literal("missions")
                        .then(literal("finish").executes(ctx -> finishMissions(ctx.getSource())))
                        .then(literal("clear").executes(ctx -> clearMissions(ctx.getSource())))));
    }

    private static UUID team(CommandSourceStack source) throws CommandSyntaxException {
        return TeamUtils.getTeamIdOrPlayerFallback(source.getPlayerOrException().getUUID());
    }

    private static int giveRocket(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(ContinuumRegistry.ROCKET.get()));
        source.sendSuccess(() -> Component.literal("Gave a Continuum Rocket."), false);
        return 1;
    }

    private static ItemStack heldRocket(CommandSourceStack source) throws CommandSyntaxException {
        ItemStack stack = source.getPlayerOrException().getItemInHand(InteractionHand.MAIN_HAND);
        if (!(stack.getItem() instanceof ContinuumRocketItem)) {
            source.sendFailure(Component.literal("Hold a Continuum Rocket in your main hand."));
            return ItemStack.EMPTY;
        }
        return stack;
    }

    private static int setWear(CommandSourceStack source, float wear) throws CommandSyntaxException {
        ItemStack rocket = heldRocket(source);
        if (rocket.isEmpty()) return 0;
        RocketStats.setWear(rocket, wear);
        source.sendSuccess(() -> Component.literal("Rocket wear set to " + Math.round(wear * 100) + "%."), false);
        return 1;
    }

    private static int upgrade(CommandSourceStack source, String upgrade, int level) throws CommandSyntaxException {
        ItemStack rocket = heldRocket(source);
        if (rocket.isEmpty()) return 0;

        String id = upgrade.toLowerCase(Locale.ROOT);
        if (!RocketStats.isKnownUpgrade(id)) {
            source.sendFailure(Component.literal("Unknown upgrade. Use hull_interlock, overdrive or reinforced_hull."));
            return 0;
        }
        RocketStats.setLevel(rocket, id, level);
        source.sendSuccess(() -> Component.literal("Set " + id + " to level " + level + "."), false);
        return 1;
    }

    private static int discover(CommandSourceStack source, ResourceLocation target, String stageName)
                                                                                                      throws CommandSyntaxException {
        if (ContinuumData.body(target) == null && ContinuumData.system(target) == null) {
            source.sendFailure(Component.literal("No such system or body: " + target));
            return 0;
        }
        DiscoveryStage stage = DiscoveryStage.parse(stageName, null);
        if (stage == null) {
            source.sendFailure(Component.literal("Stage must be unknown, detected or surveyed."));
            return 0;
        }

        UUID team = team(source);
        ContinuumTeamData.get(source.getServer()).force(team, target, stage);
        ContinuumServerEvents.sendStateToTeam(source.getServer(), team);
        source.sendSuccess(() -> Component.literal(target + " is now " + stage.name().toLowerCase(Locale.ROOT) + "."),
                false);
        return 1;
    }

    private static int grantOutpost(CommandSourceStack source, ResourceLocation body, int probes)
                                                                                                  throws CommandSyntaxException {
        if (ContinuumData.body(body) == null) {
            source.sendFailure(Component.literal("No such body: " + body));
            return 0;
        }
        UUID team = team(source);
        ContinuumTeamData.get(source.getServer()).deployProbes(team, body, probes, System.currentTimeMillis());
        ContinuumServerEvents.sendStateToTeam(source.getServer(), team);
        source.sendSuccess(() -> Component.literal("Added " + probes + " probe(s) to the outpost on " + body + "."),
                false);
        return 1;
    }

    /** Test helper: bank a full stockpile at every outpost so a haul can be flown straight away. */
    private static int fillOutposts(CommandSourceStack source) throws CommandSyntaxException {
        UUID team = team(source);
        ContinuumTeamData data = ContinuumTeamData.get(source.getServer());
        long cycle = RocketStats.outpostCycleMillis();
        int max = RocketStats.outpostMaxReady();
        long longAgo = System.currentTimeMillis() - cycle * max;
        int count = 0;
        for (Outpost outpost : data.outposts(team).values()) {
            outpost.claim(longAgo);
            outpost.settle(System.currentTimeMillis(), cycle, max, Outpost.FREE);
            count++;
        }
        data.setDirty();
        ContinuumServerEvents.sendStateToTeam(source.getServer(), team);
        int filled = count;
        source.sendSuccess(() -> Component.literal("Filled " + filled + " outpost(s)."), false);
        return count;
    }

    /** Test helper: break (or fix) every outpost of the team. */
    private static int damageOutposts(CommandSourceStack source, boolean damage) throws CommandSyntaxException {
        UUID team = team(source);
        ContinuumTeamData data = ContinuumTeamData.get(source.getServer());
        int count = 0;
        for (Outpost outpost : data.outposts(team).values()) {
            if (damage) outpost.damage();
            else outpost.repair(System.currentTimeMillis());
            count++;
        }
        data.setDirty();
        ContinuumServerEvents.sendStateToTeam(source.getServer(), team);
        int changed = count;
        source.sendSuccess(() -> Component.literal((damage ? "Damaged " : "Repaired ") + changed + " outpost(s)."), false);
        return count;
    }

    private static int clearOutposts(CommandSourceStack source) throws CommandSyntaxException {
        UUID team = team(source);
        ContinuumTeamData.get(source.getServer()).clearOutposts(team);
        ContinuumServerEvents.sendStateToTeam(source.getServer(), team);
        source.sendSuccess(() -> Component.literal("All outposts removed."), false);
        return 1;
    }

    private static int reset(CommandSourceStack source) throws CommandSyntaxException {
        UUID team = team(source);
        ContinuumTeamData.get(source.getServer()).resetDiscovery(team);
        ContinuumServerEvents.sendStateToTeam(source.getServer(), team);
        source.sendSuccess(() -> Component.literal("Discovery reset to the starting state."), false);
        return 1;
    }

    private static int finishMissions(CommandSourceStack source) throws CommandSyntaxException {
        UUID team = team(source);
        ContinuumTeamData data = ContinuumTeamData.get(source.getServer());
        int count = 0;
        for (Mission mission : List.copyOf(data.missions(team))) {
            if (mission.state == Mission.State.ACTIVE) {
                // pretend it was launched long enough ago to be due
                mission.startMillis = 0L;
                mission.durationMillis = 1L;
                count++;
            }
        }
        ContinuumMissions.resolveDue(source.getServer());
        int finished = count;
        source.sendSuccess(() -> Component.literal("Landed " + finished + " mission(s)."), false);
        return count;
    }

    private static int clearMissions(CommandSourceStack source) throws CommandSyntaxException {
        UUID team = team(source);
        ContinuumTeamData data = ContinuumTeamData.get(source.getServer());
        int count = data.missions(team).size();
        for (Mission mission : List.copyOf(data.missions(team))) data.removeMission(team, mission.id);
        ContinuumServerEvents.sendStateToTeam(source.getServer(), team);
        source.sendSuccess(() -> Component.literal("Removed " + count + " mission(s) (rockets are gone).")
                .withStyle(ChatFormatting.YELLOW), false);
        return count;
    }
}
