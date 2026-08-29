package net.phoenix.core.common.data.recipe.records;

import java.util.List;

public record ApisProgenitorConfig(
                                   String id,

                                   String inputMaterialId,
                                   int inputTier,

                                   String outputMaterialId,
                                   int outputTier,

                                   List<String> extraItemInputs,

                                   List<String> extraFluidInputs,

                                   int duration,
                                   int EUt) {

    public ApisProgenitorConfig(
                                String id,
                                String inputMaterialId, int inputTier,
                                String outputMaterialId, int outputTier,
                                List<String> extraItemInputs,
                                List<String> extraFluidInputs,
                                int duration, int EUt) {
        this.id = id;
        this.inputMaterialId = inputMaterialId;
        this.inputTier = inputTier;
        this.outputMaterialId = outputMaterialId;
        this.outputTier = outputTier;
        this.extraItemInputs = (extraItemInputs == null) ? List.of() : extraItemInputs;
        this.extraFluidInputs = (extraFluidInputs == null) ? List.of() : extraFluidInputs;
        this.duration = duration;
        this.EUt = EUt;
    }
}
