package net.phoenix.core.integration.conflux.dimension.physics;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Applies a landform impulse to the local player, whose motion the client owns. */
@OnlyIn(Dist.CLIENT)
public final class ClientImpulses {

    private ClientImpulses() {}

    public static void apply(PhysicsImpulse impulse) {
        var player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator() || player.getAbilities().flying || player.isPassenger()) return;
        impulse.apply(player);
    }
}
