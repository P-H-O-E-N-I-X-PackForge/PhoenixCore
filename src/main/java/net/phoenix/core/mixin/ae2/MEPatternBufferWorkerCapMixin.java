package net.phoenix.core.mixin.ae2;

import com.gregtechceu.gtceu.integration.ae2.machine.MEPatternBufferPartMachine;

import net.phoenix.core.integration.ae2.ExpandedPatternBufferPartMachine;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = MEPatternBufferPartMachine.class, remap = false)
public abstract class MEPatternBufferWorkerCapMixin {

    @ModifyConstant(method = "syncWorkerCount", constant = @Constant(intValue = 27))
    private int phoenix$syncCap(int original) {
        return phoenix$cap(original);
    }

    @ModifyConstant(method = "addWorker", constant = @Constant(intValue = 27))
    private int phoenix$addCap(int original) {
        return phoenix$cap(original);
    }

    private int phoenix$cap(int original) {
        return (Object) this instanceof ExpandedPatternBufferPartMachine ? ExpandedPatternBufferPartMachine.SLOTS :
                original;
    }
}
