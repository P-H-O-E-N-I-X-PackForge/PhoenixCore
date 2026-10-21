package net.phoenix.core.integration.solaris;

import net.phoenix.core.network.packet.S2CLandformZonesPacket;

import java.util.List;

/** Client copy of the landform zones the server last sent. Replaced wholesale on every update. */
public final class ClientLandformZones {

    private static volatile List<S2CLandformZonesPacket.ZoneDef> zones = List.of();

    private ClientLandformZones() {}

    public static void set(List<S2CLandformZonesPacket.ZoneDef> newZones) {
        zones = List.copyOf(newZones);
    }

    public static List<S2CLandformZonesPacket.ZoneDef> all() {
        return zones;
    }

    public static void clear() {
        zones = List.of();
    }
}
