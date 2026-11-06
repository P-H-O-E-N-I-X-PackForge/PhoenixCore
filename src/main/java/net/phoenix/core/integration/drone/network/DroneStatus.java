package net.phoenix.core.integration.drone.network;

public final class DroneStatus {

    private DroneStatus() {}

    public static int colour(DroneTargetView v) {
        if (!v.controllable()) return 0xFF8A8A8A;
        if (!v.workingEnabled()) return 0xFFE04848;
        return switch (v.status()) {
            case "WORKING" -> 0xFF55E070;
            case "WAITING" -> 0xFFFFD040;
            case "SUSPEND" -> 0xFFFF8040;
            default -> 0xFF7FB4FF;
        };
    }
}
