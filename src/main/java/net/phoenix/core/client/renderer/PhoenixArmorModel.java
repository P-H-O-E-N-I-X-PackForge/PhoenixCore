package net.phoenix.core.client.renderer;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.common.data.item.PhoenixArmorItem;

import software.bernie.geckolib.model.DefaultedGeoModel;

public class PhoenixArmorModel extends DefaultedGeoModel<PhoenixArmorItem> {

    public PhoenixArmorModel(PhoenixArmorItem item) {
        super(BuiltInRegistries.ITEM.getKey(item));
    }

    @Override
    protected String subtype() {
        return "armor";
    }

    @Override
    public ResourceLocation getAnimationResource(PhoenixArmorItem animatable) {
        return new ResourceLocation("phoenixcore", "animations/phoenix_armor.animation.json");
    }
}
