package net.phoenix.core.integration.chronicles;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.api.ConfluxApi;
import net.phoenixvine.chronicles.QuestAPI;
import net.phoenixvine.chronicles.common.registry.PhoenixTaskRegistry;

import org.jetbrains.annotations.Nullable;

final class ChroniclesImpl {

    private ChroniclesImpl() {}

    static void registerTrigger(String id, @Nullable String description) {
        QuestAPI.registerExternalTrigger(id, description);
    }

    static void fire(Player player, String id, @Nullable CompoundTag data) {
        QuestAPI.fireExternalEvent(player, id, data);
    }

    static void setup() {
        registerTrigger(PhoenixChroniclesTriggers.CONFLUX_DISCIPLINE_CHOSEN,
                "The player's Conflux team chose a discipline.");
        registerTrigger(PhoenixChroniclesTriggers.CONFLUX_RESEARCH, "A Conflux research node was unlocked.");
        registerTrigger(PhoenixChroniclesTriggers.CONFLUX_FLAG, "A Conflux research flag was granted.");
        registerTrigger(PhoenixChroniclesTriggers.STELLAR_GAS_CLOUD, "Their Stellar Forge gathered a gas cloud.");
        registerTrigger(PhoenixChroniclesTriggers.STELLAR_PROTOSTAR, "Their Stellar Forge made a protostar.");
        registerTrigger(PhoenixChroniclesTriggers.STELLAR_STAR, "Their Stellar Forge lit a star.");
        registerTrigger(PhoenixChroniclesTriggers.STELLAR_BLACK_HOLE, "Their Stellar Forge collapsed a star.");
        registerTrigger(PhoenixChroniclesTriggers.UNIQUE_REGISTERED, "Their unique multiblock formed.");
        registerTrigger(PhoenixChroniclesTriggers.UNIQUE_DUPLICATE, "They tried to build a second unique multiblock.");
        registerTrigger(PhoenixChroniclesTriggers.ITEM_PLACED, "They put an item or block on a block face.");

        PhoenixTaskRegistry.registerScripted("conflux_discipline")
                .label("Conflux Discipline (PhoenixCore)")
                .icon("§b☉")
                .tooltip("Completes when the player's Conflux team has chosen the discipline (blank for any).")
                .field(PhoenixTaskRegistry.FieldDef.text("discipline", "Discipline", "phoenix, sculk, void ..."))
                .dependsOnInventory(false)
                .onCompleted((task, player) -> {
                    if (!(player instanceof ServerPlayer sp)) return false;
                    String chosen = ConfluxApi.discipline(sp);
                    if (chosen == null) return false;
                    String want = task.getData().getString("discipline").trim();
                    return want.isEmpty() || want.equals(chosen);
                })
                .progressString((task, player) -> {
                    if (!(player instanceof ServerPlayer sp)) return null;
                    String chosen = ConfluxApi.discipline(sp);
                    return chosen == null ? "No discipline yet" : chosen;
                })
                .register();

        PhoenixCore.LOGGER.info("[Chronicles] PhoenixCore triggers and task types registered");
    }
}
