package net.phoenix.core.integration.jade.provider;


import net.phoenix.core.util.StackNbt;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderFluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient;
import com.gregtechceu.gtceu.integration.jade.GTElementHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.machine.multiblock.unique.BasicThreadedMachine;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.api.ui.ProgressStyle;
import snownee.jade.util.FluidTextHelper;

import java.util.ArrayList;
import java.util.List;

public class ThreadedRecipeOutputProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    public static final ResourceLocation UID = PhoenixCore.id("imbuer_threads_info");

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        MetaMachine machine = MetaMachine.getMachine(accessor.getLevel(), accessor.getPosition());
        if (!(machine instanceof BasicThreadedMachine imbuer)) return;

        BasicThreadedMachine.RecipeThread[] threads = imbuer.getThreads();

        ListTag threadsList = new ListTag();
        HolderLookup.Provider registries = accessor.getLevel().registryAccess();

        for (int i = 0; i < threads.length; i++) {
            BasicThreadedMachine.RecipeThread thread = threads[i];
            if (!thread.isActive() || thread.recipe == null) continue;

            CompoundTag threadTag = new CompoundTag();
            threadTag.putInt("ID", i + 1);
            threadTag.putFloat("Progress", thread.getProgressPercent());
            threadTag.putLong("CurrentTick", thread.progress);
            threadTag.putLong("MaxTick", thread.duration);
            captureOutputs(threadTag, thread.recipe, registries);
            threadsList.add(threadTag);
        }

        if (!threadsList.isEmpty()) {
            tag.put("ActiveThreads", threadsList);
        }
    }

    private void captureOutputs(CompoundTag tag, GTRecipe recipe, HolderLookup.Provider registries) {
        int runs = recipe.getTotalRuns();

        ListTag itemTags = new ListTag();
        for (var out : recipe.getOutputContents(ItemRecipeCapability.CAP)) {
            CompoundTag itemTag = new CompoundTag();
            ItemStack stack;
            if (out.content() instanceof IntProviderIngredient provider) {
                ItemStack[] inner = provider.getInner().getItems();
                if (inner.length == 0 || inner[0].isEmpty()) continue;
                stack = inner[0].copy();
                itemTag.putInt("Min", provider.getCountProvider().getMinValue());
                itemTag.putInt("Max", provider.getCountProvider().getMaxValue());
            } else {
                ItemStack[] stacks = ItemRecipeCapability.CAP.of(out.content()).getItems();
                if (stacks.length == 0 || stacks[0].isEmpty()) continue;
                stack = stacks[0].copy();
                if (out.chance() < out.maxChance()) {
                    double countD = (double) stack.getCount() * runs * out.chance() / out.maxChance();
                    stack.setCount(Math.max(1, (int) Math.round(countD)));
                }
            }
            itemTag.put("Stack", StackNbt.saveStack(stack));
            itemTags.add(itemTag);
        }
        if (!itemTags.isEmpty()) tag.put("OutputItems", itemTags);

        ListTag fluidTags = new ListTag();
        for (var out : recipe.getOutputContents(FluidRecipeCapability.CAP)) {
            CompoundTag fluidTag = new CompoundTag();
            FluidStack stack;
            if (out.content() instanceof IntProviderFluidIngredient provider) {
                FluidStack[] inner = provider.getInner().getStacks();
                if (inner.length == 0 || inner[0].isEmpty()) continue;
                stack = inner[0].copy();
                fluidTag.putInt("Min", provider.getCountProvider().getMinValue());
                fluidTag.putInt("Max", provider.getCountProvider().getMaxValue());
            } else {
                FluidStack[] stacks = FluidRecipeCapability.CAP.of(out.content()).getFluids();
                if (stacks.length == 0 || stacks[0].isEmpty()) continue;
                stack = stacks[0].copy();
                if (out.chance() < out.maxChance()) {
                    double amountD = (double) stack.getAmount() * runs * out.chance() / out.maxChance();
                    stack.setAmount(Math.max(1, (int) Math.round(amountD)));
                }
            }
            fluidTag.put("Stack", StackNbt.saveFluid(stack));
            fluidTags.add(fluidTag);
        }
        if (!fluidTags.isEmpty()) tag.put("OutputFluids", fluidTags);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains("ActiveThreads")) return;

        ListTag threadsNbt = data.getList("ActiveThreads", Tag.TAG_COMPOUND);
        if (threadsNbt.isEmpty()) return;

        IElementHelper helper = IElementHelper.get();

        for (int i = 0; i < threadsNbt.size(); i++) {
            CompoundTag tag = threadsNbt.getCompound(i);

            tooltip.add(Component.literal("§6Thread #" + tag.getInt("ID") + ":"));

            String timerText = formatTicks(tag.getLong("CurrentTick")) + " / " + formatTicks(tag.getLong("MaxTick"));
            Component timerComponent = Component.literal(timerText).withStyle(ChatFormatting.WHITE);

            ProgressStyle style = helper.progressStyle().color(0xFF4A90D9, 0xFF1A3A5C);

            tooltip.add(helper.progress(
                    tag.getFloat("Progress"),
                    timerComponent,
                    style,
                    snownee.jade.api.ui.BoxStyle.getNestedBox(),
                    true));

            boolean hasItems = tag.contains("OutputItems");
            boolean hasFluids = tag.contains("OutputFluids");

            if (hasItems || hasFluids) {
                tooltip.add(Component.translatable("gtceu.top.recipe_output"));
            }

            if (hasItems) {
                for (Tag it : tag.getList("OutputItems", Tag.TAG_COMPOUND)) {
                    CompoundTag ct = (CompoundTag) it;
                    ItemStack stack = StackNbt.loadStack(ct.getCompound("Stack"));
                    if (stack.isEmpty()) continue;
                    MutableComponent text = CommonComponents.space();
                    if (ct.contains("Min")) {
                        text.append(Component.translatable("gtceu.gui.content.range", String.valueOf(ct.getInt("Min")),
                                String.valueOf(ct.getInt("Max"))));
                    } else {
                        text.append(String.valueOf(stack.getCount()));
                    }
                    stack.setCount(1);
                    text.append(Component.translatable("gtceu.gui.content.times_item",
                            stack.getHoverName().copy().withStyle(ChatFormatting.WHITE)));
                    tooltip.add(helper.smallItem(stack));
                    tooltip.append(text);
                }
            }

            if (hasFluids) {
                for (Tag ft : tag.getList("OutputFluids", Tag.TAG_COMPOUND)) {
                    CompoundTag ct = (CompoundTag) ft;
                    FluidStack stack = StackNbt.loadFluid(ct.getCompound("Stack"));
                    if (stack.isEmpty()) continue;
                    MutableComponent text = CommonComponents.space();
                    if (ct.contains("Min")) {
                        text.append(Component.translatable("gtceu.gui.content.range",
                                FluidTextHelper.getUnicodeMillibuckets(ct.getInt("Min"), true),
                                FluidTextHelper.getUnicodeMillibuckets(ct.getInt("Max"), true)));
                    } else {
                        text.append(FluidTextHelper.getUnicodeMillibuckets(stack.getAmount(), true));
                    }
                    text.append(CommonComponents.space())
                            .append(ComponentUtils.wrapInSquareBrackets(stack.getHoverName()))
                            .withStyle(ChatFormatting.WHITE);
                    tooltip.add(GTElementHelper.smallFluid(JadeFluidObject.of(stack.getFluid(), stack.getAmount())));
                    tooltip.append(text);
                }
            }
        }
    }

    private static String formatTicks(long ticks) {
        long secs = ticks / 20;
        return String.format("%02d:%02d", secs / 60, secs % 60);
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
