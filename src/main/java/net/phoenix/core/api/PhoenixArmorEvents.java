package net.phoenix.core.api;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.data.item.PhoenixArmorItem;
import net.phoenix.core.saveddata.TeslaTeamEnergyData;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID) // Replace with your actual MOD ID
public class PhoenixArmorEvents {

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        var player = event.getEntity();

        // Check if the player is wearing the Phoenix Chestplate
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);

        if (chest.getItem() instanceof PhoenixArmorItem) {
            // If the player is in the air (flying or jumping)
            if (!player.onGround()) {
                // Multiply by 5 to negate the 1/5th (0.2) vanilla penalty
                event.setNewSpeed(event.getNewSpeed() * 5.0f);
            }

            // Optional: Also remove the underwater penalty if you want!
            if (player.isEyeInFluidType(net.minecraftforge.common.ForgeMod.WATER_TYPE.get()) &&
                    !net.minecraft.world.item.enchantment.EnchantmentHelper.hasAquaAffinity(player)) {
                event.setNewSpeed(event.getNewSpeed() * 5.0f);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);

            if (chest.getItem() instanceof PhoenixArmorItem) {
                DamageSource source = event.getSource();

                // FLY INTO WALL (Kinetic) or FALLING (Fall)
                if (source.is(DamageTypes.FLY_INTO_WALL) || source.is(DamageTypes.FALL)) {
                    // You can make this cost a tiny bit of energy or make it free
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof PhoenixArmorItem)) return;

        ServerLevel level = (ServerLevel) player.level();
        var source = event.getSource();
        var nbt = chest.getOrCreateTag();

        // --- A. KINETIC DAMPENERS (Free protection from your own wings) ---
        if (source.is(net.minecraft.world.damagesource.DamageTypes.FLY_INTO_WALL) ||
                source.is(net.minecraft.world.damagesource.DamageTypes.FALL)) {
            event.setCanceled(true);
            return;
        }

        // --- B. PLASMA SHIELD (Drains Tesla Energy) ---
        if (nbt.getBoolean("teslaMode")) {
            TeslaTeamEnergyData data = TeslaTeamEnergyData.get(level);
            var network = data.getOrCreate(player.getUUID());

            // Cost: 10,000 EU per 1 damage point (half heart)
            float damage = event.getAmount();
            java.math.BigInteger cost = java.math.BigInteger.valueOf((long) (damage * 10000L));

            if (network.stored.compareTo(cost) >= 0) {
                network.drain(cost);

                // Visuals: Electric shield sparks
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        player.getX(), player.getY() + 1, player.getZ(),
                        5, 0.3, 0.5, 0.3, 0.02);

                // Play a "shield hit" sound occasionally
                if (player.getRandom().nextFloat() < 0.3f) {
                    level.playSound(null, player.blockPosition(),
                            SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.5f, 1.5f);
                }

                event.setCanceled(true); // Player takes ZERO damage
            }
        }
    }
}
