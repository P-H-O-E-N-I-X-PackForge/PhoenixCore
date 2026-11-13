package net.phoenix.core.common.data.recipe.pft.discipline;

import java.util.List;

public enum Discipline {

    PHOENIX("phoenix", "Phoenix",
            ore("iron", 3), ore("tin", 3), ore("copper", 2), ore("zinc", 2), ore("lead", 3), ore("bauxite", 2),
            ore("sulfur", 3), ore("gold", 3), ore("redstone", 2), ore("pyrite", 2), ore("cinnabar", 2),
            ore("dormant_ember", 3)),

    SCULK("sculk", "Sculk",
            ore("iron", 3), ore("tin", 2), ore("zinc", 2), ore("nickel", 2), ore("bornite", 2), ore("lead", 2),
            ore("apatite", 2), ore("barite", 2), ore("stibnite", 2), ore("bauxite", 2), ore("redstone", 2),
            ore("pyrite", 2), ore("cinnabar", 2), ore("lapis", 2), ore("amethyst", 3)),

    VOID("void", "Void",
            ore("chalcopyrite", 3), ore("tin", 3), ore("zinc", 2), ore("lead", 2), ore("nickel", 2), ore("gold", 3),
            ore("nether_quartz", 2), ore("redstone", 2), ore("pyrite", 2), ore("sulfur", 2), ore("silver", 3),
            ore("apatite", 2), ore("lapis", 2), ore("amethyst", 3)),

    SEALED_A("sealed_a", "Sealed (iron)",
            ore("iron", 3), ore("copper", 3), ore("tin", 2), ore("zinc", 2), ore("coal", 3), ore("lead", 2),
            ore("nickel", 2), ore("silver", 2), ore("bauxite", 3), ore("barite", 2), ore("stibnite", 2),
            ore("pyrite", 3), ore("lapis", 3), ore("redstone", 2), ore("sulfur", 3), ore("gold", 2),
            ore("cinnabar", 2)),

    SEALED_B("sealed_b", "Sealed (mirror)",
            ore("gold", 3), ore("nether_quartz", 3), ore("sulfur", 2), ore("redstone", 2), ore("copper", 2),
            ore("tin", 2), ore("iron", 3), ore("zinc", 2), ore("lapis", 2), ore("cinnabar", 2), ore("pyrite", 2),
            ore("coal", 2), ore("nickel", 3), ore("lead", 2), ore("silver", 2), ore("bauxite", 3));

    public record Ore(String name, int weight) {}

    private static Ore ore(String name, int weight) {
        return new Ore(name, weight);
    }

    private final String id;
    private final String title;
    private final List<Ore> commonOres;

    Discipline(String id, String title, Ore... commonOres) {
        this.id = id;
        this.title = title;
        this.commonOres = List.of(commonOres);
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public List<Ore> commonOres() {
        return commonOres;
    }
}
