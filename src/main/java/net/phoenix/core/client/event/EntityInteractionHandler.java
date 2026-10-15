package net.phoenix.core.client.event;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.phoenix.core.common.item.ChameleonSprayCanBehaviour;
import net.phoenix.core.common.item.ChameleonSprayCanItem;

@EventBusSubscriber(modid = "phoenixcore", value = Dist.CLIENT)
public class EntityInteractionHandler {

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();

        if (stack.getItem() instanceof ChameleonSprayCanItem) {

            if (event.getTarget() instanceof Wolf wolf) {

                if (wolf.isTame()) {
                    DyeColor currentColor = ChameleonSprayCanBehaviour.getColor(stack);
                    if (currentColor != null) {
                        if (!event.getLevel().isClientSide()) {
                            wolf.setCollarColor(currentColor);
                        }

                        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
                        event.setCanceled(true);
                    }
                }
            } else if (event.getTarget() instanceof Cat cat) {

                if (cat.isTame()) {
                    DyeColor currentColor = ChameleonSprayCanBehaviour.getColor(stack);
                    if (currentColor != null) {
                        if (!event.getLevel().isClientSide()) {
                            cat.setCollarColor(currentColor);
                        }

                        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
                        event.setCanceled(true);
                    }
                }
            }
        }
    }
}
