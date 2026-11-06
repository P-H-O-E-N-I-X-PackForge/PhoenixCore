package net.phoenix.core.common.data.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.phoenix.core.PhoenixCore;

public final class PftItemTweaks {

    private PftItemTweaks() {}

    private static final int SUGAR_CANE_BURN_TICKS = 300;

    @EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Components {

        private Components() {}

        @SubscribeEvent
        public static void onModify(ModifyDefaultComponentsEvent event) {
            event.modify(Items.ENDER_PEARL, builder -> builder
                    .set(DataComponents.MAX_STACK_SIZE, 64)
                    .set(DataComponents.FIRE_RESISTANT, Unit.INSTANCE)
                    .set(DataComponents.RARITY, Rarity.UNCOMMON));

            food(event, "phoenixcore:honey_ingot", new FoodProperties.Builder().nutrition(2).saturationModifier(50f)
                    .build());
            food(event, "phoenixcore:honey_treat", new FoodProperties.Builder().nutrition(4).saturationModifier(200f)
                    .fast().build());
        }

        private static void food(ModifyDefaultComponentsEvent event, String id, FoodProperties food) {
            ResourceLocation key = ResourceLocation.parse(id);
            if (!BuiltInRegistries.ITEM.containsKey(key)) {
                PhoenixCore.LOGGER.warn("[PFT] item tweak: no item {}", id);
                return;
            }
            Item item = BuiltInRegistries.ITEM.get(key);
            event.modify(item, builder -> builder.set(DataComponents.FOOD, food));
        }
    }

    @EventBusSubscriber(modid = PhoenixCore.MOD_ID)
    public static final class Fuel {

        private Fuel() {}

        @SubscribeEvent
        public static void onBurnTime(FurnaceFuelBurnTimeEvent event) {
            if (event.getItemStack().is(Items.SUGAR_CANE)) event.setBurnTime(SUGAR_CANE_BURN_TICKS);
        }
    }
}
