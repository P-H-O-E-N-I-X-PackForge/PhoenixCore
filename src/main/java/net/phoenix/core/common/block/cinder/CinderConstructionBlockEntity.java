package net.phoenix.core.common.block.cinder;

import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenix.core.PhoenixCore;

import com.mojang.serialization.DataResult;

import java.util.HashMap;
import java.util.Map;

public class CinderConstructionBlockEntity extends BlockEntity {

    public static final int FALL_TICKS = 50;
    public static final float FALL_START_HEIGHT = 40f;

    private final Map<BlockPos, BlockInfo> pending = new HashMap<>();
    private BlockInfo anchorInfo = BlockInfo.EMPTY;
    private long fallStartGameTime = -1;
    private boolean finished = false;

    public CinderConstructionBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void beginConstruction(Map<BlockPos, BlockInfo> placements, BlockInfo anchorInfo) {
        pending.clear();
        this.anchorInfo = anchorInfo;
        fallStartGameTime = -1;
        finished = false;

        BlockPos self = getBlockPos();
        for (var entry : placements.entrySet()) {
            if (entry.getKey().equals(self)) continue;
            pending.put(entry.getKey(), entry.getValue());
        }
        setChanged();

        PhoenixCore.LOGGER.info("[CinderConstruction] begin at {}: {} blocks, {} ticks fall ({}s)", self,
                pending.size(), FALL_TICKS, FALL_TICKS / 20.0);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CinderConstructionBlockEntity be) {
        if (be.finished) return;

        if (be.fallStartGameTime < 0) {
            be.fallStartGameTime = level.getGameTime();
            be.setChanged();
            be.syncToClients();
            return;
        }
        if (level.getGameTime() - be.fallStartGameTime < FALL_TICKS) return;

        be.finished = true;

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (var entry : be.pending.entrySet()) {
            entry.getValue().apply(level, entry.getKey());
            minX = Math.min(minX, entry.getKey().getX());
            maxX = Math.max(maxX, entry.getKey().getX());
            minZ = Math.min(minZ, entry.getKey().getZ());
            maxZ = Math.max(maxZ, entry.getKey().getZ());
        }
        boolean hadBlocks = !be.pending.isEmpty();
        be.pending.clear();
        be.anchorInfo.apply(level, pos);

        if (level instanceof ServerLevel serverLevel) {
            int spanX = hadBlocks ? maxX - minX + 1 : 1;
            int spanZ = hadBlocks ? maxZ - minZ + 1 : 1;
            CinderVisualEffects.playImpact(serverLevel, pos, spanX, spanZ);
        }

        PhoenixCore.LOGGER.info("[CinderConstruction] impact at {}", pos);
    }

    private void syncToClients() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public Map<BlockPos, BlockInfo> getPendingBlocks() {
        return fallStartGameTime < 0 || finished ? Map.of() : pending;
    }

    public long getFallStartGameTime() {
        return fallStartGameTime;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("FallStartGameTime", fallStartGameTime);
        tag.putBoolean("Finished", finished);

        DataResult<Tag> anchorResult = BlockInfo.CODEC.encodeStart(NbtOps.INSTANCE, anchorInfo);
        anchorResult.result().ifPresent(t -> tag.put("AnchorInfo", t));

        ListTag pendingTag = new ListTag();
        for (var entry : pending.entrySet()) {
            DataResult<Tag> infoResult = BlockInfo.CODEC.encodeStart(NbtOps.INSTANCE, entry.getValue());
            infoResult.result().ifPresent(infoTag -> {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putLong("Pos", entry.getKey().asLong());
                entryTag.put("Info", infoTag);
                pendingTag.add(entryTag);
            });
        }
        tag.put("Pending", pendingTag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        fallStartGameTime = tag.getLong("FallStartGameTime");
        finished = tag.getBoolean("Finished");

        pending.clear();

        if (tag.contains("AnchorInfo", Tag.TAG_COMPOUND)) {
            BlockInfo.CODEC.parse(NbtOps.INSTANCE, tag.getCompound("AnchorInfo")).result()
                    .ifPresent(info -> anchorInfo = info);
        }

        if (tag.contains("Pending", Tag.TAG_LIST)) {
            ListTag pendingTag = tag.getList("Pending", Tag.TAG_COMPOUND);
            for (int i = 0; i < pendingTag.size(); i++) {
                CompoundTag entryTag = pendingTag.getCompound(i);
                BlockPos pos = BlockPos.of(entryTag.getLong("Pos"));
                BlockInfo.CODEC.parse(NbtOps.INSTANCE, entryTag.get("Info")).result()
                        .ifPresent(info -> pending.put(pos, info));
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
