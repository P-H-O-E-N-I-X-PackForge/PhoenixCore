package net.phoenix.core.integration.continuum.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.PacketDistributor;
import net.phoenix.core.integration.continuum.network.S2COpenMapPacket;
import net.phoenix.core.network.PhoenixNetwork;

/**
 * Where missions are launched from. Right-clicking opens the Continuum map with launching enabled; the map can also be
 * opened for viewing only with {@code /continuum map}. This is a plain block for milestone 3 - the GT-style, tier-gated
 * multiblock pad described in the design doc replaces it later.
 */
public class LaunchPadBlock extends Block {

    public LaunchPadBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            PhoenixNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new S2COpenMapPacket(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
