package net.phoenix.core.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class PlacedItemEntity extends Entity {

    private static final EntityDataAccessor<ItemStack> ITEM = SynchedEntityData.defineId(PlacedItemEntity.class,
            EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> FACE = SynchedEntityData.defineId(PlacedItemEntity.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SPIN = SynchedEntityData.defineId(PlacedItemEntity.class,
            EntityDataSerializers.INT);

    public static final float BLOCK_SCALE = 0.3f;
    public static final float ITEM_SCALE = 0.4f;

    public PlacedItemEntity(EntityType<? extends PlacedItemEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static PlacedItemEntity create(Level level, ItemStack stack, Direction face, Vec3 point) {
        PlacedItemEntity e = new PlacedItemEntity(PhoenixEntities.PLACED_ITEM.get(), level);
        e.entityData.set(ITEM, stack);
        e.entityData.set(FACE, face.get3DDataValue());
        e.setPos(point);
        e.setXRot(face == Direction.UP ? -90.0f : face == Direction.DOWN ? 90.0f : 0.0f);
        e.setYRot(face.getAxis().isHorizontal() ? face.get2DDataValue() * 90.0f : 0.0f);
        return e;
    }

    private static final double[][] SLOTS = { { 0.5, 0.7 }, { 0.28, 0.3 }, { 0.72, 0.3 } };

    public static final int SLOT_COUNT = SLOTS.length;

    public static Vec3 slot(BlockPos block, Direction face, int slot) {
        double across = SLOTS[slot][0];
        double up = SLOTS[slot][1];

        Vec3 origin = new Vec3(block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5)
                .add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        Vec3 right;
        Vec3 upward;
        if (face.getAxis().isHorizontal()) {
            Direction r = face.getCounterClockWise();
            right = new Vec3(r.getStepX(), 0.0, r.getStepZ());
            upward = new Vec3(0.0, 1.0, 0.0);
        } else {
            right = new Vec3(1.0, 0.0, 0.0);
            upward = new Vec3(0.0, 0.0, face == Direction.UP ? -1.0 : 1.0);
        }
        return origin.add(right.scale(across - 0.5)).add(upward.scale(up - 0.5));
    }

    public ItemStack item() {
        return entityData.get(ITEM);
    }

    public Direction face() {
        return Direction.from3DDataValue(entityData.get(FACE));
    }

    public int spin() {
        return entityData.get(SPIN);
    }

    public boolean isBlock() {
        return item().getItem() instanceof BlockItem;
    }

    public float scale() {
        return isBlock() ? BLOCK_SCALE : ITEM_SCALE;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ITEM, ItemStack.EMPTY);
        builder.define(FACE, Direction.UP.get3DDataValue());
        builder.define(SPIN, 0);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key == ITEM || key == FACE) refreshDimensions();
    }

    @Override
    protected AABB makeBoundingBox() {
        Vec3 p = position();
        Direction face = face();
        double half = scale() * 0.5;
        double lift = isBlock() ? half : 0.03;
        Vec3 centre = p.add(face.getStepX() * lift, face.getStepY() * lift, face.getStepZ() * lift);
        return new AABB(centre.x - half, centre.y - half, centre.z - half, centre.x + half, centre.y + half,
                centre.z + half);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount % 20 == 0) {
            Vec3 into = position().subtract(face().getStepX() * 0.05, face().getStepY() * 0.05,
                    face().getStepZ() * 0.05);
            if (item().isEmpty() || level().getBlockState(BlockPos.containing(into)).isAir()) {
                drop();
            }
        }
    }

    private void drop() {
        if (!item().isEmpty()) spawnAtLocation(item().copy());
        discard();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) {

            if (!level().isClientSide) {
                if (!player.isCreative()) {
                    ItemStack back = item().copy();
                    if (!player.getInventory().add(back)) player.drop(back, false);
                }
                discard();
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (!level().isClientSide) entityData.set(SPIN, (spin() + 1) % 8);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (attacker instanceof Player player) {
            if (!level().isClientSide) {
                if (player.isCreative()) discard();
                else drop();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ItemStack.parse(registryAccess(), tag.getCompound("Item")).ifPresent(s -> entityData.set(ITEM, s));
        entityData.set(FACE, tag.getInt("Face"));
        entityData.set(SPIN, tag.getInt("Spin"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (!item().isEmpty()) tag.put("Item", item().save(registryAccess()));
        tag.putInt("Face", entityData.get(FACE));
        tag.putInt("Spin", spin());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity entity) {
        return new ClientboundAddEntityPacket(this, entity);
    }
}
