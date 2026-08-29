package net.phoenix.core.common.data.materials;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlag;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.BlastProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialStack;

import net.phoenix.core.PhoenixCore;

@SuppressWarnings("All")
public class PhoenixMaterialHelpers {

    public record ItemPipeOpts(int priority, int throughput) {}

    public record FluidPipeOpts(int maxTemp,
                                int throughput,
                                boolean gasProof,
                                boolean acidProof,
                                boolean cryoProof,
                                boolean plasmaProof) {}

    public record BlastTempOpts(int temp,
                                BlastProperty.GasTier blastProperty,
                                int EUt,
                                int duration) {}

    public static Material makeIngotWithFluidMaterialFinal(
                                                           String id,
                                                           int color,
                                                           int secondaryColor,
                                                           MaterialIconSet iconSet,
                                                           String langValue,
                                                           MaterialStack[] materialStacks,
                                                           MaterialFlag... flags) {
        return makeIngotWithFluidMaterial(
                id,
                color,
                secondaryColor,
                iconSet,
                langValue,
                null,
                materialStacks,
                null,
                null,
                null,
                flags);
    }

    public static Material makeIngotWithFluidMaterial(
                                                      String id,
                                                      int color,
                                                      int secondaryColor,
                                                      MaterialIconSet iconSet,
                                                      String langValue,
                                                      String formula,
                                                      MaterialStack[] components,
                                                      FluidPipeOpts fluidPipeOpts,
                                                      ItemPipeOpts itemPipeOpts,
                                                      BlastTempOpts blastTempOpts,
                                                      MaterialFlag... flags) {
        Material.Builder b = new Material.Builder(PhoenixCore.id(id))
                .ingot()
                .fluid()
                .color(color)
                .iconSet(iconSet);

        if (flags != null && flags.length > 0) {
            b.flags(flags);
        }
        if (secondaryColor != -1) {
            b.secondaryColor(secondaryColor);
        }
        if (langValue != null) {
            b.langValue(langValue);
        }
        if (formula != null && !formula.isBlank()) {
            b.formula(formula);
        }
        if (itemPipeOpts != null) {
            b.itemPipeProperties(itemPipeOpts.priority(), itemPipeOpts.throughput());
        }
        if (fluidPipeOpts != null) {
            b.fluidPipeProperties(
                    fluidPipeOpts.maxTemp(),
                    fluidPipeOpts.throughput(),
                    fluidPipeOpts.gasProof(),
                    fluidPipeOpts.acidProof(),
                    fluidPipeOpts.cryoProof(),
                    fluidPipeOpts.plasmaProof());
        }
        if (blastTempOpts != null) {
            b.blastTemp(
                    blastTempOpts.temp(),
                    blastTempOpts.blastProperty,
                    blastTempOpts.EUt(),
                    blastTempOpts.duration());
        }
        if (components != null && components.length > 0) {
            b.componentStacks(components);
        }

        return b.buildAndRegister();
    }

    public static Material makePolymerMaterialFinal(
                                                    String id,
                                                    int color,
                                                    int secondaryColor,
                                                    MaterialIconSet iconSet,
                                                    String langValue,
                                                    MaterialStack[] components,
                                                    MaterialFlag... flags) {
        return makePolymerMaterial(
                id,
                color,
                -1,
                iconSet,
                langValue,
                null,
                null,
                null,
                null,
                null);
    }

    public static Material makePolymerMaterial(
                                               String id,
                                               int color,
                                               int secondaryColor,
                                               MaterialIconSet iconSet,
                                               String langValue,
                                               MaterialStack[] components,
                                               FluidPipeOpts fluidPipeOpts,
                                               ItemPipeOpts itemPipeOpts,
                                               BlastTempOpts blastTempOpts,
                                               MaterialFlag... flags) {
        Material.Builder b = new Material.Builder(PhoenixCore.id(id))
                .ingot()
                .fluid()
                .polymer()
                .color(color)
                .iconSet(iconSet);

        if (flags != null && flags.length > 0) {
            b.flags(flags);
        }
        if (secondaryColor != -1) {
            b.secondaryColor(secondaryColor);
        }
        if (langValue != null) {
            b.langValue(langValue);
        }
        if (itemPipeOpts != null) {
            b.itemPipeProperties(itemPipeOpts.priority(), itemPipeOpts.throughput());
        }
        if (fluidPipeOpts != null) {
            b.fluidPipeProperties(
                    fluidPipeOpts.maxTemp(),
                    fluidPipeOpts.throughput(),
                    fluidPipeOpts.gasProof(),
                    fluidPipeOpts.acidProof(),
                    fluidPipeOpts.cryoProof(),
                    fluidPipeOpts.plasmaProof());
        }
        if (blastTempOpts != null) {
            b.blastTemp(
                    blastTempOpts.temp(),
                    blastTempOpts.blastProperty,
                    blastTempOpts.EUt(),
                    blastTempOpts.duration());
        }
        if (components != null && components.length > 0) {
            b.componentStacks(components);
        }

        return b.buildAndRegister();
    }

    public static Material makeIngotMaterialFinal(
                                                  String id,
                                                  int color,
                                                  int secondaryColor,
                                                  MaterialIconSet iconSet,
                                                  String langValue,
                                                  MaterialStack[] components,
                                                  MaterialFlag... flags) {
        return makeIngotMaterial(
                id,
                color,
                -1,
                iconSet,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    public static Material makeIngotMaterial(String id, int color,
                                             int secondaryColor,
                                             MaterialIconSet iconSet,
                                             String langValue,
                                             MaterialStack[] components,
                                             FluidPipeOpts fluidPipeOpts,
                                             ItemPipeOpts itemPipeOpts,
                                             BlastTempOpts blastTempOpts,
                                             MaterialFlag... flags) {
        Material.Builder b = new Material.Builder(PhoenixCore.id(id))
                .ingot()
                .fluid()
                .color(color)
                .iconSet(iconSet);

        if (flags != null && flags.length > 0) {
            b.flags(flags);
        }
        if (secondaryColor != -1) {
            b.secondaryColor(secondaryColor);
        }
        if (langValue != null) {
            b.langValue(langValue);
        }
        if (itemPipeOpts != null) {
            b.itemPipeProperties(itemPipeOpts.priority(), itemPipeOpts.throughput());
        }
        if (fluidPipeOpts != null) {
            b.fluidPipeProperties(fluidPipeOpts.maxTemp(), fluidPipeOpts.throughput(), fluidPipeOpts.gasProof(),
                    fluidPipeOpts.acidProof(), fluidPipeOpts.cryoProof(), fluidPipeOpts.plasmaProof());
        }
        if (blastTempOpts != null) {
            b.blastTemp(blastTempOpts.temp(), blastTempOpts.blastProperty, blastTempOpts.EUt(),
                    blastTempOpts.duration());
        }

        if (components != null && components.length > 0) {
            b.componentStacks(components);
        }

        return b.buildAndRegister();
    }

    public static Material makeDustMaterialFinal(String id,
                                                 int color,
                                                 int secondaryColor,
                                                 MaterialIconSet iconSet,
                                                 MaterialStack[] components,
                                                 MaterialFlag... flags) {
        return makeDustMaterial(
                id,
                color,
                -1,
                iconSet,
                null,
                null);
    }

    public static Material makeDustMaterial(String id,
                                            int color,
                                            int secondaryColor,
                                            MaterialIconSet iconSet,
                                            MaterialStack[] componenets,
                                            MaterialFlag... flags) {
        Material.Builder b = new Material.Builder(PhoenixCore.id(id))
                .dust()
                .color(color)
                .iconSet(iconSet);
        if (flags != null && flags.length > 0) {
            b.flags(flags);
        }
        if (secondaryColor != -1) {
            b.secondaryColor(secondaryColor);
        }
        return b.buildAndRegister();
    }

    public static Material makeDustWithFluidMaterialFinal(String id,
                                                          int color,
                                                          int secondaryColor,
                                                          MaterialIconSet iconSet,
                                                          MaterialStack[] componets,
                                                          MaterialFlag... flags) {
        return makeDustWithFluidMaterial(
                id,
                color,
                -1,
                iconSet,
                null,
                null);
    }

    public static Material makeDustWithFluidMaterial(String id,
                                                     int color,
                                                     int secondaryColor,
                                                     MaterialIconSet iconSet,
                                                     MaterialStack[] components,
                                                     MaterialFlag... flags) {
        Material.Builder b = new Material.Builder(PhoenixCore.id(id))
                .dust()
                .fluid()
                .color(color)
                .iconSet(iconSet);
        if (flags != null && flags.length > 0) {
            b.flags(flags);
        }
        if (secondaryColor != -1) {
            b.secondaryColor(secondaryColor);
        }
        if (components != null && components.length > 0) {
            b.componentStacks(components);
        }
        return b.buildAndRegister();
    }

    public static Material makeFluidMaterialFinal(String id,
                                                  int color,
                                                  int secondaryColor,
                                                  MaterialIconSet iconSet,
                                                  MaterialStack[] components,
                                                  MaterialFlag... flags) {
        return makeFluidMaterial(
                id,
                color,
                -1,
                iconSet,
                null,
                null);
    }

    public static Material makeFluidMaterial(String id,
                                             int color,
                                             int secondaryColor,
                                             MaterialIconSet iconSet,
                                             MaterialStack[] components,
                                             MaterialFlag... flags) {
        Material.Builder b = new Material.Builder(PhoenixCore.id(id))
                .fluid()
                .color(color)
                .iconSet(iconSet);

        if (flags != null && flags.length > 0) {
            b.flags(flags);
        }
        if (secondaryColor != -1) {
            b.secondaryColor(secondaryColor);
        }
        if (components != null && components.length > 0) {
            b.componentStacks(components);
        }
        return b.buildAndRegister();
    }

    public static Material makeGasMaterialFinal(
                                                String id,
                                                int color,
                                                int secondaryColor,
                                                MaterialIconSet iconSet,
                                                MaterialStack[] components,
                                                MaterialFlag... flags) {
        return makeGasMaterial(
                id,
                color,
                secondaryColor,
                iconSet,
                components,
                flags);
    }

    public static Material makeGasMaterial(String id,
                                           int color,
                                           int secondaryColor,
                                           MaterialIconSet iconSet,
                                           MaterialStack[] components,
                                           MaterialFlag... flags) {
        Material.Builder b = new Material.Builder(PhoenixCore.id(id))
                .gas()
                .color(color)
                .iconSet(iconSet);

        if (flags != null && flags.length > 0) {
            b.flags(flags);
        }
        if (secondaryColor != -1) {
            b.secondaryColor(secondaryColor);
        }
        if (components != null && components.length > 0) {
            b.componentStacks(components);
        }

        return b.buildAndRegister();
    }
}
