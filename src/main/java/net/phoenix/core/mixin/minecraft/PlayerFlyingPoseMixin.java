package net.phoenix.core.mixin.minecraft;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.common.data.item.PhoenixItems;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public class PlayerFlyingPoseMixin {

    @Inject(method = "setupRotations*", at = @At("TAIL"))
    private void phoenix$applyFlyingRotation(AbstractClientPlayer player, PoseStack poseStack,
                                             float ageInTicks, float rotationYaw, float partialTicks,
                                             CallbackInfo ci) {
        // Only apply if wearing the Phoenix chestplate
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.is(PhoenixItems.PHOENIX_CHESTPLATE.get())) return;

        if (player.getAbilities().flying || player.isFallFlying()) {
            CompoundTag nbt = chest.getTag();
            boolean sonic = nbt != null && nbt.getBoolean("IsSonicFlight");

            // Target pitch: -70° when sonic (nearly horizontal), -40° for normal flight
            float targetPitch = sonic ? -70.0f : -40.0f;

            // Lerp toward target based on current look angle
            float currentPitch = player.getXRot();
            float blend = Math.min(Math.abs(currentPitch) / 90.0f, 1.0f);
            float appliedPitch = targetPitch * blend;

            poseStack.mulPose(Axis.XP.rotationDegrees(appliedPitch));
        }
    }
}
