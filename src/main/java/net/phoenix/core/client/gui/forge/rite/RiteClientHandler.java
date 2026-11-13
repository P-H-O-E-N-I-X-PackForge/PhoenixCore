package net.phoenix.core.client.gui.forge.rite;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.rebirth.Capstone;
import net.phoenix.core.rebirth.rite.S2CRitePacket;

@OnlyIn(Dist.CLIENT)
public final class RiteClientHandler {

    private RiteClientHandler() {}

    public static void accept(S2CRitePacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof RiteScreen open && open.pos().equals(msg.pos())) {
            open.update(msg);
            return;
        }
        if (msg.status() != S2CRitePacket.OPEN) return;
        Capstone cap = Capstone.values()[Math.floorMod(msg.capstone(), Capstone.COUNT)];
        mc.setScreen(switch (cap) {
            case TESLA -> new TeslaRiteScreen(msg);
            case HONEY -> new HoneyRiteScreen(msg);
            case PLASMA -> new PlasmaRiteScreen(msg);
            case ENGINE -> new EngineRiteScreen(msg);
            case CONTINUUM -> new ContinuumRiteScreen(msg);
            case NULL_WELL -> new NullWellRiteScreen(msg);
            case TOKAMAK -> new TokamakRiteScreen(msg);
            case STELLAR_FORGE -> new StellarRiteScreen(msg);
            case AETHER_CRUCIBLE -> new CrucibleRiteScreen(msg);
            case SCULK_CORTEX -> new CortexRiteScreen(msg);
            case PHOENIX_INFUSER -> new InfuserRiteScreen(msg);
            case ANTIMATTER_COLLIDER -> new ColliderRiteScreen(msg);
            case ASCENSION_REACTOR -> new AscensionRiteScreen(msg);
        });
    }
}
