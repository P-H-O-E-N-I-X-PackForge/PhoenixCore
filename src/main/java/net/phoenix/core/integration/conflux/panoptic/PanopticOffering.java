package net.phoenix.core.integration.conflux.panoptic;

import net.phoenix.core.integration.conflux.ConfluxDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record PanopticOffering(long[] data, List<FluidCost> fluids) {

    public record FluidCost(String fluidId, int millibuckets) {}

    public static final PanopticOffering NONE = new PanopticOffering(new long[ConfluxDataType.values().length],
            List.of());

    public static PanopticOffering data(long[] perType) {
        return new PanopticOffering(perType, List.of());
    }

    public static PanopticOffering fluid(String fluidId, int millibuckets) {
        return new PanopticOffering(new long[ConfluxDataType.values().length],
                millibuckets > 0 ? List.of(new FluidCost(fluidId, millibuckets)) : List.of());
    }

    public String describe() {
        List<String> parts = new ArrayList<>();
        for (ConfluxDataType t : ConfluxDataType.values()) {
            if (data[t.ordinal()] > 0)
                parts.add(String.format(Locale.ROOT, "%,d %s", data[t.ordinal()], t.displayName));
        }
        for (FluidCost f : fluids) {
            String name = f.fluidId().contains(":") ? f.fluidId().substring(f.fluidId().indexOf(':') + 1) :
                    f.fluidId();
            parts.add(String.format(Locale.ROOT, "%,d mB %s", f.millibuckets(), name.replace('_', ' ')));
        }
        return parts.isEmpty() ? "nothing" : String.join(", ", parts);
    }

    public boolean isFree() {
        if (!fluids.isEmpty()) return false;
        for (long d : data) if (d > 0) return false;
        return true;
    }
}
