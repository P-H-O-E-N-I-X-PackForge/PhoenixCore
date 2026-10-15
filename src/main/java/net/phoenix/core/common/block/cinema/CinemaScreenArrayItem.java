package net.phoenix.core.common.block.cinema;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class CinemaScreenArrayItem extends Item {

    private final int size;

    public CinemaScreenArrayItem(int size, Properties properties) {
        super(properties);
        this.size = size;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        Level level = context.getLevel();
        Direction facing = player.getDirection().getOpposite();
        Direction right = facing.getCounterClockWise();

        BlockPos base = context.getClickedPos().relative(context.getClickedFace());
        int half = (size - 1) / 2;

        List<BlockPos> positions = new ArrayList<>(size * size);
        for (int col = -half; col <= half; col++) {
            for (int row = -half; row <= half; row++) {
                positions.add(base.relative(right, col).relative(Direction.UP, row));
            }
        }

        for (BlockPos pos : positions) {
            if (!level.getBlockState(pos).canBeReplaced()) {
                return InteractionResult.FAIL;
            }
        }

        if (!level.isClientSide) {
            BlockState screenState = CinemaBlocks.CINEMA_SCREEN.get().defaultBlockState()
                    .setValue(CinemaScreenBlock.FACING, facing);
            for (BlockPos pos : positions) {
                level.setBlock(pos, screenState, 3);
            }
            if (!player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
