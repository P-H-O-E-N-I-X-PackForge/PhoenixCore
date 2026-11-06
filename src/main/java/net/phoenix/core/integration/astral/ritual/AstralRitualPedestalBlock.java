package net.phoenix.core.integration.astral.ritual;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.phoenix.core.integration.astral.item.AstralWandItem;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

public class AstralRitualPedestalBlock extends BaseEntityBlock {

    public AstralRitualPedestalBlock(Properties props) {
        super(props);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AstralRitualPedestalBlockEntity(
                net.phoenix.core.integration.astral.AstralBlocks.ASTRAL_RITUAL_PEDESTAL_BE
                        .get(),
                pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof AstralWandItem)) {
            player.displayClientMessage(Component.literal("The pedestal needs a Wand to channel."), true);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(AstralRitualPedestalBlock::new);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
