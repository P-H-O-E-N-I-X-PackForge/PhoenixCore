package net.phoenix.core.integration.conflux.terminal;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.integration.conflux.client.ResearchTerminalScreen;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

public class ResearchTerminalBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public ResearchTerminalBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ResearchTerminalBlockEntity(ConfluxTerminalRegistry.TERMINAL_BE.get(), pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
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
    public net.minecraft.network.chat.MutableComponent getName() {
        return net.minecraft.network.chat.Component.translatableWithFallback("block.phoenixcore.research_terminal",
                "Conflux Research Terminal");
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(ResearchTerminalBlock::new);
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
