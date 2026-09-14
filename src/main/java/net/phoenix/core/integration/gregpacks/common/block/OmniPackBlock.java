package net.phoenix.core.integration.gregpacks.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import net.phoenix.core.integration.gregpacks.common.item.OmniPackTier;

import org.jetbrains.annotations.Nullable;

@SuppressWarnings("all")
public class OmniPackBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE_NS = Shapes.box(0.2 / 16, 0, 1.9 / 16, 15.8 / 16, 1, 14.8 / 16);
    private static final VoxelShape SHAPE_EW = Shapes.box(1.9 / 16, 0, 0.2 / 16, 14.8 / 16, 1, 15.8 / 16);

    private final OmniPackTier tier;

    public OmniPackBlock(OmniPackTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH));
    }

    public OmniPackTier getTier() {
        return tier;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OmniPackBlockEntity(pos, state, tier);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        Direction facing = state.getValue(FACING);
        return (facing == Direction.EAST || facing == Direction.WEST) ? SHAPE_EW : SHAPE_NS;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return getShape(state, level, pos, ctx);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        if (player.isShiftKeyDown()) {
            if (level.getBlockEntity(pos) instanceof OmniPackBlockEntity be) {
                ItemStack packItem = be.toItemStack();
                be.setPickedUp(true);
                level.removeBlock(pos, false);

                boolean added = false;
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    if (player.getInventory().getItem(i).isEmpty()) {
                        player.getInventory().setItem(i, packItem);
                        player.getInventory().setChanged();
                        added = true;
                        break;
                    }
                }
                if (!added) player.drop(packItem, false);
            }
            return InteractionResult.SUCCESS;
        }

        if (level.getBlockEntity(pos) instanceof OmniPackBlockEntity be) {
            net.phoenix.core.integration.gregpacks.common.upgrade.UpgradeEffects effects = new net.phoenix.core.integration.gregpacks.common.upgrade.UpgradeEffects(
                    tier, be.getUpgradeInventory());
            int realSlots = effects.totalSlots;

            NetworkHooks.openScreen(serverPlayer, be,
                    buf -> {
                        buf.writeShort(realSlots);
                        buf.writeByte(tier.defaultMaxUpgrades);
                        buf.writeByte(tier.ordinal());
                    });
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && stack.hasTag()) {
            if (level.getBlockEntity(pos) instanceof OmniPackBlockEntity omniPackBE) {
                omniPackBE.loadFromItemStack(stack);
                omniPackBE.setChanged();
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof OmniPackBlockEntity be) {
                be.setPickedUp(be.isPickedUp());
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public boolean dropFromExplosion(net.minecraft.world.level.Explosion explosion) {
        return true;
    }

    @Override
    public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos,
                                ItemStack tool, boolean dropExperience) {
        if (level.getBlockEntity(pos) instanceof OmniPackBlockEntity be && be.isPickedUp()) {
            return;
        }
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
    }
}
