package net.phoenix.core.mixin.gtceu;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;

import net.phoenix.core.api.IMaterialBuilderExtension;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = Material.Builder.class, remap = false)
public abstract class MixinMaterialBuilder implements IMaterialBuilderExtension {

    @Unique
    private String phoenix$tempNameColor;

    @Override
    public Material.Builder phoenixCore$nameColor(String hex) {
        this.phoenix$tempNameColor = hex;
        return (Material.Builder) (Object) this;
    }

    // This is the "secret sauce" - we need a way for the Material to get this value
    // You can use a static Map to bridge the builder to the Material during registration
    @Unique
    public String phoenix$getCapturedColor() {
        return phoenix$tempNameColor;
    }
}
