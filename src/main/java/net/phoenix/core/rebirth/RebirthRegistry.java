package net.phoenix.core.rebirth;

import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import com.tterrag.registrate.util.entry.ItemEntry;

import static net.phoenix.core.PhoenixCore.PHOENIX_CREATIVE_TAB;
import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public final class RebirthRegistry {

    private RebirthRegistry() {}

    static {
        REGISTRATE.creativeModeTab(() -> PHOENIX_CREATIVE_TAB);
    }

    public static final ItemEntry<RebirthBinderItem> REBIRTH_BINDER = REGISTRATE
            .item("rebirth_binder", RebirthBinderItem::new)
            .lang("Rebirth Binder")
            .register();

    public static final ItemEntry<AshenReelItem> ASHEN_REEL = REGISTRATE
            .item("ashen_reel", AshenReelItem::new)
            .lang("Ashen Reel")
            .register();

    public static void init() {
        if (ModList.get().isLoaded("phoenix_tesla_network")) {

            try {
                TeslaHook.install();
            } catch (LinkageError e) {
                net.phoenix.core.PhoenixCore.LOGGER
                        .info("The installed Tesla mod has no CapstoneGate; skipping the hook.");
            }
        }
    }

    public static void registerPackets(PayloadRegistrar registrar) {
        registrar.playToClient(S2CEndingPacket.TYPE, S2CEndingPacket.STREAM_CODEC, S2CEndingPacket::handle);
        registrar.playToClient(S2CRebirthStatePacket.TYPE, S2CRebirthStatePacket.STREAM_CODEC,
                S2CRebirthStatePacket::handle);
        registrar.playToServer(C2SRebirthActionPacket.TYPE, C2SRebirthActionPacket.STREAM_CODEC,
                C2SRebirthActionPacket::handle);
        registrar.playToClient(net.phoenix.core.rebirth.rite.S2CRitePacket.TYPE,
                net.phoenix.core.rebirth.rite.S2CRitePacket.STREAM_CODEC,
                net.phoenix.core.rebirth.rite.S2CRitePacket::handle);
        registrar.playToServer(net.phoenix.core.rebirth.rite.C2SRitePacket.TYPE,
                net.phoenix.core.rebirth.rite.C2SRitePacket.STREAM_CODEC,
                net.phoenix.core.rebirth.rite.C2SRitePacket::handle);
    }

    private static final class TeslaHook {

        static void install() {
            net.phoenix_tesla_network.tesla.api.CapstoneGate.check = RebirthNetwork::allows;
        }
    }
}
