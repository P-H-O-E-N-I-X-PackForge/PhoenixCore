package net.phoenix.core.integration.conflux.research;

import net.minecraft.world.entity.player.Player;
import net.phoenix.core.PhoenixAttachments;

public final class PlayerResearchCapability {

    private PlayerResearchCapability() {}

    public static PlayerResearchData get(Player player) {
        return player.getData(PhoenixAttachments.RESEARCH);
    }
}
