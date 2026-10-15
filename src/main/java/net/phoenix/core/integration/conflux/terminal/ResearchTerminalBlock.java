package net.phoenix.core.integration.conflux.terminal;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.phoenix.core.integration.conflux.client.ResearchTerminalScreen;

import org.jetbrains.annotations.Nullable;

public class ResearchTerminalBlock extends BaseEntityBlock {

    public ResearchTerminalBlock(Properties props) {
        super(props);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ResearchTerminalBlockEntity(ConfluxTerminalRegistry.TERMINAL_BE.get(), pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {

            if (level.getBlockEntity(pos) instanceof ResearchTerminalBlockEntity terminal) {
                openScreen(terminal);
            }
            return InteractionResult.SUCCESS;
        }
        if (player instanceof net.minecraft.server.level.ServerPlayer sp &&
                level.getBlockEntity(pos) instanceof ResearchTerminalBlockEntity terminal) {
            terminal.adopt(net.phoenix.core.integration.conflux.research.ResearchTeamHelper.getTeamId(sp));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable net.minecraft.world.entity.LivingEntity placer,
                            net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof net.minecraft.server.level.ServerPlayer sp &&
                level.getBlockEntity(pos) instanceof ResearchTerminalBlockEntity terminal) {
            terminal.adopt(net.phoenix.core.integration.conflux.research.ResearchTeamHelper.getTeamId(sp));
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @OnlyIn(Dist.CLIENT)
    private static void openScreen(ResearchTerminalBlockEntity terminal) {
        Minecraft.getInstance().setScreen(new ResearchTerminalScreen(terminal));
    }
}
