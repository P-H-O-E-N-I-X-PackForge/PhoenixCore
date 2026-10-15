package net.phoenix.core.integration.wingsoffire;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.configs.PhoenixConfigs;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public final class WingsOfFireCompat {

    private WingsOfFireCompat() {}

    public static final TagKey<EntityType<?>> PHOENIX = TagKey.create(Registries.ENTITY_TYPE,
            new ResourceLocation("wings_of_fire", "phoenix"));

    private static final String STAY_TAG = "pft_stay";

    private static final Map<Class<?>, EntityDataAccessor<Boolean>> RIDDEN_FLAG = new HashMap<>();

    public static boolean isPhoenix(Entity entity) {
        return entity != null && entity.getType().is(PHOENIX);
    }

    @SuppressWarnings("unchecked")
    private static EntityDataAccessor<Boolean> riddenFlag(Entity entity) {
        return RIDDEN_FLAG.computeIfAbsent(entity.getClass(), type -> {
            try {
                Field field = type.getField("DATA_HasBeenRidden");
                return (EntityDataAccessor<Boolean>) field.get(null);
            } catch (ReflectiveOperationException | ClassCastException e) {
                return null;
            }
        });
    }

    private static void keepAlive(Entity phoenix) {
        EntityDataAccessor<Boolean> flag = riddenFlag(phoenix);
        if (flag != null && Boolean.TRUE.equals(phoenix.getEntityData().get(flag))) {
            phoenix.getEntityData().set(flag, false);
        }
    }

    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide || !isPhoenix(entity)) return;
        var cfg = PhoenixConfigs.INSTANCE.wingsOfFire;

        if (!entity.isVehicle()) {
            if (cfg.persistentMounts) keepAlive(entity);

            if (cfg.stayCommand && entity.getPersistentData().getBoolean(STAY_TAG)) {
                var data = entity.getPersistentData();

                if (data.contains("pft_stay_x")) {
                    entity.setPos(data.getDouble("pft_stay_x"), data.getDouble("pft_stay_y"),
                            data.getDouble("pft_stay_z"));
                }
                entity.setDeltaMovement(0.0, 0.0, 0.0);
                entity.setNoGravity(true);
            }
        }
    }

    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        Entity mount = event.getEntityBeingMounted();
        if (mount.level().isClientSide || !isPhoenix(mount)) return;

        if (event.isMounting()) {

            clearStay(mount);
        } else if (PhoenixConfigs.INSTANCE.wingsOfFire.persistentMounts) {
            keepAlive(mount);
        }
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!PhoenixConfigs.INSTANCE.wingsOfFire.stayCommand) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        Player player = event.getEntity();
        Entity target = event.getTarget();
        if (!player.isShiftKeyDown() || !player.getMainHandItem().isEmpty()) return;
        if (!isPhoenix(target) || !(target instanceof TamableAnimal phoenix) || !phoenix.isOwnedBy(player)) return;

        if (!player.level().isClientSide) {
            boolean stay = !phoenix.getPersistentData().getBoolean(STAY_TAG);
            if (stay) {
                phoenix.getPersistentData().putBoolean(STAY_TAG, true);
                phoenix.getPersistentData().putDouble("pft_stay_x", phoenix.getX());
                phoenix.getPersistentData().putDouble("pft_stay_y", phoenix.getY());
                phoenix.getPersistentData().putDouble("pft_stay_z", phoenix.getZ());
                phoenix.setOrderedToSit(true);
                phoenix.getNavigation().stop();
            } else {
                clearStay(phoenix);
            }
            player.displayClientMessage(Component.literal(stay ? "Your phoenix will stay here." :
                    "Your phoenix will follow you again."), true);
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));
        event.setCanceled(true);
    }

    private static void clearStay(Entity phoenix) {
        if (!phoenix.getPersistentData().getBoolean(STAY_TAG) &&
                !(phoenix instanceof TamableAnimal t && t.isOrderedToSit())) {
            return;
        }
        phoenix.getPersistentData().remove(STAY_TAG);
        phoenix.getPersistentData().remove("pft_stay_x");
        phoenix.getPersistentData().remove("pft_stay_y");
        phoenix.getPersistentData().remove("pft_stay_z");
        phoenix.setNoGravity(false);
        if (phoenix instanceof TamableAnimal tamable) tamable.setOrderedToSit(false);
    }
}
