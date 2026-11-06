package net.phoenix.core.integration.ars_nouveau.logistics;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.block.PipeBlock;
import com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.pipenet.IPipeNode;
import com.gregtechceu.gtceu.api.registry.registrate.provider.GTBlockstateProvider;
import com.gregtechceu.gtceu.client.model.pipe.PipeModel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/** A Source pipe of one tier. Pipes of different tiers join; the weakest pipe on a run is its limit. */
public class SourcePipeBlock extends PipeBlock<SourcePipeTier, SourcePipeData, SourceNetwork.LevelNet> {

    private final Supplier<BlockEntityType<SourcePipeBlockEntity>> beType;

    public SourcePipeBlock(Properties properties, SourcePipeTier tier,
                           Supplier<BlockEntityType<SourcePipeBlockEntity>> beType) {
        super(properties, tier);
        this.beType = beType;
    }

    @Override
    public SourceNetwork.LevelNet getWorldPipeNet(ServerLevel level) {
        return SourceNetwork.LevelNet.getOrCreate(level);
    }

    @Override
    public BlockEntityType<? extends PipeBlockEntity<SourcePipeTier, SourcePipeData>> getBlockEntityType() {
        return beType.get();
    }

    @Override
    public SourcePipeData createRawData(BlockState state, @Nullable ItemStack stack) {
        return SourcePipeData.of(pipeType);
    }

    @Override
    public SourcePipeData createProperties(IPipeNode<SourcePipeTier, SourcePipeData> pipeTile) {
        return SourcePipeData.of(pipeTile.getPipeType());
    }

    @Override
    public SourcePipeData getFallbackType() {
        return SourcePipeData.of(SourcePipeTier.BASIC);
    }

    @Override
    public boolean canPipesConnect(IPipeNode<SourcePipeTier, SourcePipeData> selfTile, Direction side,
                                   IPipeNode<SourcePipeTier, SourcePipeData> sideTile) {
        return selfTile instanceof SourcePipeBlockEntity && sideTile instanceof SourcePipeBlockEntity;
    }

    @Override
    public boolean canPipeConnectToBlock(IPipeNode<SourcePipeTier, SourcePipeData> selfTile, Direction side,
                                         Level level, BlockPos pos) {
        BlockEntity tile = level.getBlockEntity(pos);
        return tile != null && !(tile instanceof SourcePipeBlockEntity) && SourceEndpoint.find(level, pos) != null;
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                java.util.List<net.minecraft.network.chat.Component> tooltip,
                                net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(net.minecraft.network.chat.Component.literal("Carries Source between jars, hatches and relays")
                .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
        tooltip.add(net.minecraft.network.chat.Component.literal(String.format(java.util.Locale.ROOT,
                "%,d Source/t, loses %.2f%% per pipe", pipeType.throughput(), pipeType.lossPerBlock() * 100))
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.add(net.minecraft.network.chat.Component.literal(
                "Draws from jars and output hatches, fills input hatches first").withStyle(
                        net.minecraft.ChatFormatting.DARK_GRAY));
        tooltip.add(net.minecraft.network.chat.Component.literal("Different tiers join; the weakest sets the limit")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }

    @Override
    public PipeModel createPipeModel(GTBlockstateProvider provider) {
        return new PipeModel(this, provider, pipeType.getThickness(),
                GTCEu.id("block/pipe/pipe_side"),
                GTCEu.id("block/pipe/pipe_normal_in"));
    }
}
