package net.phoenix.core.integration.gregvaults.common.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.phoenix.core.integration.gregvaults.common.items.WirelessTerminalItem;
import net.phoenix.core.integration.gregvaults.common.registry.VaultRegistry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class EmitterUpgradeRecipe extends CustomRecipe {

    private final WirelessTerminalItem.EmitterTier tier;
    private final Item emitterItem;

    public EmitterUpgradeRecipe(WirelessTerminalItem.EmitterTier tier, Item emitterItem) {
        super(CraftingBookCategory.MISC);
        this.tier = tier;
        this.emitterItem = emitterItem;
    }

    @Override
    public boolean matches(CraftingInput inv, Level level) {
        boolean foundTerminal = false;
        boolean foundEmitter = false;

        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof WirelessTerminalItem) {
                if (WirelessTerminalItem.getEmitterTier(stack).level == tier.level - 1) {
                    foundTerminal = true;
                } else {
                    return false;
                }
            } else if (stack.getItem() == emitterItem) {
                foundEmitter = true;
            } else {
                return false;
            }
        }

        return foundTerminal && foundEmitter;
    }

    @Override
    public ItemStack assemble(CraftingInput inv, HolderLookup.Provider registries) {
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() instanceof WirelessTerminalItem) {
                ItemStack result = stack.copy();
                WirelessTerminalItem.setEmitterTier(result, tier);
                result.setCount(1);
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        ItemStack result = new ItemStack(VaultRegistry.WIRELESS_VAULT_TERMINAL.get());
        WirelessTerminalItem.setEmitterTier(result, tier);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return w * h >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return VaultRecipes.EMITTER_UPGRADE_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<EmitterUpgradeRecipe> {

        private static final MapCodec<EmitterUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.fieldOf("tier").forGetter(r -> r.tier.level),
                ResourceLocation.CODEC.fieldOf("emitter").forGetter(r -> BuiltInRegistries.ITEM.getKey(r.emitterItem)))
                .apply(i, (level, itemId) -> new EmitterUpgradeRecipe(
                        WirelessTerminalItem.EmitterTier.fromLevel(level), BuiltInRegistries.ITEM.get(itemId))));

        private static final StreamCodec<RegistryFriendlyByteBuf, EmitterUpgradeRecipe> STREAM_CODEC = StreamCodec.of(
                (buf, recipe) -> {
                    buf.writeInt(recipe.tier.level);
                    buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(recipe.emitterItem));
                },
                buf -> new EmitterUpgradeRecipe(WirelessTerminalItem.EmitterTier.fromLevel(buf.readInt()),
                        BuiltInRegistries.ITEM.get(buf.readResourceLocation())));

        @Override
        public MapCodec<EmitterUpgradeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, EmitterUpgradeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
