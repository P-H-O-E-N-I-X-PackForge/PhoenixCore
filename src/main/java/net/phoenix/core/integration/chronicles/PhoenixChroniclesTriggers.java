package net.phoenix.core.integration.chronicles;

public final class PhoenixChroniclesTriggers {

    private PhoenixChroniclesTriggers() {}

    public static final String CONFLUX_DISCIPLINE_CHOSEN = net.phoenix.conflux.api.ConfluxHooks.CONFLUX_DISCIPLINE_CHOSEN;
    public static final String CONFLUX_RESEARCH = net.phoenix.conflux.api.ConfluxHooks.CONFLUX_RESEARCH;
    public static final String CONFLUX_FLAG = net.phoenix.conflux.api.ConfluxHooks.CONFLUX_FLAG;

    public static final String STELLAR_GAS_CLOUD = "phoenixcore:stellar/gas_cloud";
    public static final String STELLAR_PROTOSTAR = "phoenixcore:stellar/protostar";
    public static final String STELLAR_STAR = "phoenixcore:stellar/star";
    public static final String STELLAR_BLACK_HOLE = "phoenixcore:stellar/black_hole";

    public static final String UNIQUE_REGISTERED = "phoenixcore:unique/registered";
    public static final String UNIQUE_DUPLICATE = "phoenixcore:unique/duplicate";

    public static final String ITEM_PLACED = "phoenixcore:placed_item/placed";

    public static String stellar(String stage) {
        return switch (stage) {
            case "GAS_CLOUD" -> STELLAR_GAS_CLOUD;
            case "PROTOSTAR" -> STELLAR_PROTOSTAR;
            case "STAR" -> STELLAR_STAR;
            case "BLACK_HOLE" -> STELLAR_BLACK_HOLE;
            default -> null;
        };
    }
}
