package net.phoenix.core.integration.conflux.pipe;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.block.PipeBlock;
import com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.pipenet.IPipeNode;
import com.gregtechceu.gtceu.api.registry.registrate.provider.GTBlockstateProvider;
import com.gregtechceu.gtceu.client.model.pipe.PipeModel;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenix.core.integration.conflux.ConfluxDataType;

import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ConfluxPipeBlock extends PipeBlock<ConfluxPipeType, ConfluxPipeData, LevelConfluxPipeNet> {

    private final Supplier<BlockEntityType<ConfluxPipeBlockEntity>> beTypeSupplier;

    public ConfluxPipeBlock(Properties props, ConfluxPipeType pipeType,
                            Supplier<BlockEntityType<ConfluxPipeBlockEntity>> beTypeSupplier) {
        super(props, pipeType);
        this.beTypeSupplier = beTypeSupplier;
    }

    @Override
    public LevelConfluxPipeNet getWorldPipeNet(ServerLevel level) {
        return LevelConfluxPipeNet.getOrCreate(level);
    }

    @Override
    public BlockEntityType<? extends PipeBlockEntity<ConfluxPipeType, ConfluxPipeData>> getBlockEntityType() {
        return beTypeSupplier.get();
    }

    @Override
    public ConfluxPipeData createRawData(BlockState state, @Nullable ItemStack stack) {
        return ConfluxPipeData.INSTANCE;
    }

    @Override
    public ConfluxPipeData createProperties(IPipeNode<ConfluxPipeType, ConfluxPipeData> pipeTile) {
        return ConfluxPipeData.INSTANCE;
    }

    @Override
    public ConfluxPipeData getFallbackType() {
        return ConfluxPipeData.INSTANCE;
    }

    @Override
    public boolean canPipesConnect(IPipeNode<ConfluxPipeType, ConfluxPipeData> selfTile, Direction side,
                                   IPipeNode<ConfluxPipeType, ConfluxPipeData> sideTile) {
        return selfTile.getPipeType() == sideTile.getPipeType();
    }

    @Override
    public boolean canPipeConnectToBlock(IPipeNode<ConfluxPipeType, ConfluxPipeData> selfTile, Direction side,
                                         net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        BlockEntity tile = level.getBlockEntity(pos);
        if (tile == null) return false;
        ConfluxDataType dt = pipeType.dataType();

        if (ConfluxMultiHandlerCapability.find(tile, side.getOpposite()) != null) {
            return true;
        }

        IConfluxDataHandler h = ConfluxDataCapability.find(tile, side.getOpposite());
        return h != null && h.getDataType() == dt;
    }

    @Override
    public PipeModel createPipeModel(GTBlockstateProvider provider) {
        return new PipeModel(this, provider, pipeType.getThickness(),
                GTCEu.id("block/pipe/pipe_side"),
                GTCEu.id("block/pipe/pipe_normal_in"));
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack,
                                net.minecraft.world.item.Item.TooltipContext context,
                                java.util.List<net.minecraft.network.chat.Component> tooltip,
                                net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ConfluxDataType dt = pipeType.dataType();
        tooltip.add(net.minecraft.network.chat.Component.literal("Carries ").withStyle(
                net.minecraft.ChatFormatting.GRAY).append(dt.displayComponent()).append(
                        net.minecraft.network.chat.Component.literal(" data").withStyle(
                                net.minecraft.ChatFormatting.GRAY)));
        tooltip.add(net.minecraft.network.chat.Component.literal("Passes " + ConfluxPipeBlockEntity.THROUGHPUT +
                " u/t per pipe, routed to the nearest machine that takes it").withStyle(
                        net.minecraft.ChatFormatting.DARK_GRAY));
        tooltip.add(net.minecraft.network.chat.Component.literal("Joins only pipes of its own type").withStyle(
                net.minecraft.ChatFormatting.DARK_GRAY));
    }

    public ConfluxDataType getDataType() {
        return pipeType.dataType();
    }
}
