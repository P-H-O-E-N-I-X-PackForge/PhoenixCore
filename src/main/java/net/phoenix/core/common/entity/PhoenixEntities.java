package net.phoenix.core.common.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.phoenix.core.PhoenixCore;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class PhoenixEntities {

    private PhoenixEntities() {}

    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister
            .create(ForgeRegistries.ENTITY_TYPES, PhoenixCore.MOD_ID);
    private static final DeferredRegister<Item> SPAWN_EGGS = DeferredRegister.create(Registries.ITEM,
            PhoenixCore.MOD_ID);

    public static final RegistryObject<EntityType<TetoSlime>> TETO_SLIME = ENTITY_TYPES.register("teto_slime",
            () -> EntityType.Builder.<TetoSlime>of(TetoSlime::new, MobCategory.CREATURE).sized(2.04f, 2.04f)
                    .clientTrackingRange(10).build("teto_slime"));

    public static final RegistryObject<Item> TETO_SLIME_SPAWN_EGG = SPAWN_EGGS.register("teto_slime_spawn_egg",
            () -> new ForgeSpawnEggItem(TETO_SLIME, 0xC8283C, 0xFFB3C0, new Item.Properties()));

    public static void init(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        SPAWN_EGGS.register(modEventBus);
    }

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(TETO_SLIME.get(), Monster.createMonsterAttributes().build());
    }
}
