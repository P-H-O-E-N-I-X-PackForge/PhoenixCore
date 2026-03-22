package net.phoenix.core.common.data.item;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.item.ComponentItem;
import com.gregtechceu.gtceu.api.item.component.IInteractionItem;
import com.gregtechceu.gtceu.api.item.component.IItemUIFactory;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;

import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.phoenix.core.client.SoulMapWidget;
import net.phoenix.core.saveddata.SoulSavedData;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.annotation.Nullable;

public class SoulLensItem extends ComponentItem implements IItemUIFactory, IInteractionItem {

    public SoulLensItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult onItemUseFirst(@NotNull ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        MetaMachine machine = MetaMachine.getMachine(level, pos);

        // 1. Compatibility: Act like a Data Stick if the machine supports it
        if (machine instanceof IDataStickInteractable interactable) {
            return player.isShiftKeyDown() ?
                    interactable.onDataStickShiftUse(player, stack) :
                    interactable.onDataStickUse(player, stack);
        }

        // 2. Safety: If it's any other GT Machine, we return SUCCESS (to consume click)
        // or PASS (to allow the machine GUI to open).
        // IMPORTANT: Never call super.onItemUseFirst() here to avoid StackOverflow.
        if (machine != null) {
            // If you want the Soul Map to open even when clicking a machine,
            // you could trigger the UI here, but usually, PASS is safer.
            return InteractionResult.PASS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player,
                                                           @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Open the Soul Map UI on right-click (Air or Block)
        // ComponentItem handles the factory registration, but we call it manually here for 'use'
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            HeldItemUIFactory.INSTANCE.openUI(new HeldItemUIFactory.HeldItemHolder(player, hand), serverPlayer);
        }

        // Using sidedSuccess prevents the "swinging" animation from repeating unnecessarily on the server
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public ModularUI createUI(HeldItemUIFactory.HeldItemHolder holder, Player player) {
        ItemStack stack = holder.getHeld();
        CompoundTag tag = stack.getOrCreateTag();

        ModularUI ui = new ModularUI(220, 140, holder, player).background(GuiTextures.BACKGROUND);
        ui.widget(new LabelWidget(8, 6, "Soul Field Topology").setTextColor(0x8F00FF));

        // Map Widget
        WidgetGroup mapGroup = new WidgetGroup(10, 20, 110, 110);
        mapGroup.setBackground(GuiTextures.DISPLAY);
        mapGroup.addWidget(new SoulMapWidget(4, 4, stack));
        ui.widget(mapGroup);

        // Info Panel
        WidgetGroup infoGroup = new WidgetGroup(125, 20, 85, 110);
        String biomeName = tag.getString("BiomeName");
        float current = tag.getFloat("CurrentSoul");
        float max = tag.getFloat("MaxSoul");

        infoGroup.addWidget(new LabelWidget(0, 0,
                "Target: §6" + (biomeName.length() > 10 ? biomeName.substring(0, 10) : biomeName)));
        if (biomeName.length() > 10) infoGroup.addWidget(new LabelWidget(0, 10, "§6" + biomeName.substring(10)));

        int yOffset = biomeName.length() > 10 ? 25 : 15;
        infoGroup.addWidget(new LabelWidget(0, yOffset, "Density: §d" + String.format("%.2fx", current)));
        infoGroup.addWidget(new LabelWidget(0, yOffset + 15, "Status: " + getStatusString(current, max)));

        ui.widget(infoGroup);
        return ui;
    }

    private String getStatusString(float current, float max) {
        if (max <= 0) return "§7Unknown";
        float percentage = current / max;
        if (percentage >= 0.90f) return "§aVibrant";
        if (percentage >= 0.40f) return "§bStable";
        return "§cDepleted";
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, Level level, @NotNull Entity entity, int slotId,
                              boolean isSelected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) return;

        boolean isUIOpen = player.containerMenu instanceof com.lowdragmc.lowdraglib.gui.modular.ModularUIContainer;
        if (level.getGameTime() % 20 != 0 && !isUIOpen) return;

        CompoundTag tag = stack.getOrCreateTag();
        SoulSavedData data = SoulSavedData.get((ServerLevel) level);
        ChunkPos center = new ChunkPos(entity.blockPosition());

        var entry = data.getSoulMap().get(center);
        if (entry != null) {
            tag.putFloat("CurrentSoul", entry.currentSoul);
            tag.putFloat("MaxSoul", entry.maxCapacity);
        }

        ResourceLocation biomeId = level.registryAccess().registryOrThrow(Registries.BIOME)
                .getKey(level.getBiome(entity.blockPosition()).value());
        if (biomeId != null) tag.putString("BiomeName", formatBiomeName(biomeId.getPath()));

        ListTag mapList = new ListTag();
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                ChunkPos currentPos = new ChunkPos(center.x + x, center.z + z);
                CompoundTag chunkTag = new CompoundTag();
                chunkTag.putInt("relX", x);
                chunkTag.putInt("relZ", z);
                chunkTag.putFloat("density", data.getMultiplier(currentPos));
                mapList.add(chunkTag);
            }
        }
        tag.put("MapData", mapList);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip,
                                @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("item.ars_nouveau.soul_lens.tooltip").withStyle(ChatFormatting.GRAY));
    }

    private String formatBiomeName(String path) {
        if (path == null || path.isEmpty()) return "Unknown";
        String[] words = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    @Override
    public boolean onEntitySwing(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        return false;
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack, @NotNull Player player,
                                                           @NotNull LivingEntity target,
                                                           @NotNull InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }
}
