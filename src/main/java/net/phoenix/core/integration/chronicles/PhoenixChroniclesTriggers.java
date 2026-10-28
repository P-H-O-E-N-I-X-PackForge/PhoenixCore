package net.phoenix.core.integration.chronicles;

/** The trigger ids PhoenixCore fires for Phoenix Chronicles (see {@link ChroniclesLink}). */
public final class PhoenixChroniclesTriggers {

    private PhoenixChroniclesTriggers() {}

    // Conflux (data: "discipline", "node" or "flag")
    public static final String CONFLUX_DISCIPLINE_CHOSEN = "phoenixcore:conflux/discipline_chosen";
    public static final String CONFLUX_RESEARCH = "phoenixcore:conflux/research";
    public static final String CONFLUX_FLAG = "phoenixcore:conflux/flag";

    // Stellar Forge: its owner, when the body reaches that stage (data: "stage")
    public static final String STELLAR_GAS_CLOUD = "phoenixcore:stellar/gas_cloud";
    public static final String STELLAR_PROTOSTAR = "phoenixcore:stellar/protostar";
    public static final String STELLAR_STAR = "phoenixcore:stellar/star";
    public static final String STELLAR_BLACK_HOLE = "phoenixcore:stellar/black_hole";

    // Unique multiblocks (data: "machine"): the owner's one of its kind formed, or a second was tried
    public static final String UNIQUE_REGISTERED = "phoenixcore:unique/registered";
    public static final String UNIQUE_DUPLICATE = "phoenixcore:unique/duplicate";

    // Placed items (data: "item")
    public static final String ITEM_PLACED = "phoenixcore:placed_item/placed";

    /** The trigger for a Stellar Forge stage name, or null for none (idle). */
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
