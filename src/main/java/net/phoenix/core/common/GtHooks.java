package net.phoenix.core.common;

import com.gregtechceu.gtceu.api.events.MachineRecipeFinishedEvent;
import com.gregtechceu.gtceu.api.events.MultiblockFormEvent;
import com.gregtechceu.gtceu.api.recipe.modifier.GlobalRecipeModifiers;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.phoenix.conflux.common.research.ConfluxRecipeEffects;
import net.phoenix.conflux.common.research.ResearchTeamHelper;
import net.phoenix.conflux.common.research.ResearchTreeRegistry;
import net.phoenix.conflux.common.research.WorldResearchData;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.ars_nouveau.api.recipe.SoulGrowthHook;

import java.util.UUID;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public final class GtHooks {

    private GtHooks() {}

    public static void registerModifiers() {
        GlobalRecipeModifiers.register(ConfluxRecipeEffects.MODIFIER);

        com.gregtechceu.gtceu.api.cover.CoverTransferRates.register(
                (kind, tier, rate) -> tier < 1 ? rate : rate * net.phoenix.core.common.data.CoverRates.GT_MULTIPLIER);
    }

    @SubscribeEvent
    public static void gateMultiblockFormation(MultiblockFormEvent event) {
        var machine = event.getController();
        if (!(machine.getLevel() instanceof ServerLevel level)) return;
        var machineId = machine.getDefinition().getId();
        if (!ResearchTreeRegistry.INSTANCE.getGatedMultiblocks().contains(machineId)) return;
        ServerPlayer nearest = (ServerPlayer) level.getNearestPlayer(
                machine.getBlockPos().getX() + 0.5,
                machine.getBlockPos().getY() + 0.5,
                machine.getBlockPos().getZ() + 0.5,
                64, p -> !p.isSpectator() && p instanceof ServerPlayer);
        UUID teamId = nearest != null ? ResearchTeamHelper.getTeamId(nearest) : null;
        if (teamId == null || !WorldResearchData.get(level).canFormMultiblock(teamId, machineId)) {
            event.deny(Component.translatable("phoenixcore.research.multiblock_locked", machineId.getPath()));

            var state = machine.getPatternState(event.getStructureName());
            if (state != null) {
                state.setErrors(java.util.List.of(new net.phoenix.core.api.pattern.PhoenixPatternError(
                        Component.literal("This multiblock is locked behind research"),
                        Component.literal("Unlock it in the research tree to let it form"))));
            }
        }
    }

    @SubscribeEvent
    public static void soulGrowth(MachineRecipeFinishedEvent event) {
        var recipe = event.getRecipe();
        if (recipe == null ||
                (!recipe.data.contains("soul_growth_perm") && !recipe.data.contains("soul_growth_temp"))) {
            return;
        }
        if (event
                .getMachine() instanceof com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine machine &&
                machine.getLevel() instanceof ServerLevel serverLevel) {
            SoulGrowthHook.handle(recipe, serverLevel, machine.getBlockPos());
        }
    }
}
