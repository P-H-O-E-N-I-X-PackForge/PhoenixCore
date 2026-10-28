package net.phoenix.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.common.entity.PlacedItemEntity;

/** The held item put on the block face the player is looking at (the "place item" key). */
public class C2SPlaceItemPacket implements CustomPacketPayload {

    public static final Type<C2SPlaceItemPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2s_place_item"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SPlaceItemPacket> STREAM_CODEC = StreamCodec
            .ofMember(C2SPlaceItemPacket::encode, C2SPlaceItemPacket::new);

    /** How many can stand on one face of a block. */
    public static final int MAX_PER_FACE = 2;

    private final BlockPos pos;
    private final Direction face;
    private final Vec3 hit;

    public C2SPlaceItemPacket(BlockPos pos, Direction face, Vec3 hit) {
        this.pos = pos;
        this.face = face;
        this.hit = hit;
    }

    public C2SPlaceItemPacket(RegistryFriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.face = buf.readEnum(Direction.class);
        this.hit = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeEnum(face);
        buf.writeDouble(hit.x);
        buf.writeDouble(hit.y);
        buf.writeDouble(hit.z);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SPlaceItemPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            place(player, msg);
        });
    }

    private static boolean clear(Vec3 point, float scale, java.util.List<PlacedItemEntity> there) {
        for (PlacedItemEntity other : there) {
            if (other.position().distanceTo(point) < (other.scale() + scale) * 0.5 * 0.7) return false;
        }
        return true;
    }

    private static double along(BlockPos pos, Direction.Axis axis) {
        return axis == Direction.Axis.X ? pos.getX() : axis == Direction.Axis.Y ? pos.getY() : pos.getZ();
    }

    /**
     * Where to put it: where you pointed, or, if something is there, the nearest free spot on the same face (searched
     * across the whole face, so it does not matter exactly where you click), so a block and an item can stand together.
     */
    private static Vec3 freeSpot(BlockPos pos, Direction face, Vec3 wanted, float scale,
                                 java.util.List<PlacedItemEntity> there) {
        if (clear(wanted, scale, there)) return wanted;
        Direction.Axis[] axes = java.util.Arrays.stream(Direction.Axis.values()).filter(a -> a != face.getAxis())
                .toArray(Direction.Axis[]::new);
        Vec3 best = null;
        for (int i = 0; i <= 14; i++) {
            for (int j = 0; j <= 14; j++) {
                Vec3 candidate = wanted.with(axes[0], along(pos, axes[0]) + 0.15 + 0.05 * i)
                        .with(axes[1], along(pos, axes[1]) + 0.15 + 0.05 * j);
                if (!clear(candidate, scale, there)) continue;
                if (best == null || candidate.distanceToSqr(wanted) < best.distanceToSqr(wanted)) best = candidate;
            }
        }
        return best;
    }

    private static void place(ServerPlayer player, C2SPlaceItemPacket msg) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || player.isSpectator()) return;
        var level = player.serverLevel();
        if (!level.isLoaded(msg.pos) || level.getBlockState(msg.pos).isAir()) return;
        if (player.distanceToSqr(Vec3.atCenterOf(msg.pos)) > 64.0) return;
        if (!player.mayInteract(level, msg.pos)) return;
        // the spot in front of the face has to be free of another block
        if (level.getBlockState(msg.pos.relative(msg.face)).isSolidRender(level, msg.pos.relative(msg.face))) return;
        // what is already on this face of the block
        java.util.List<PlacedItemEntity> there = new java.util.ArrayList<>();
        for (PlacedItemEntity other : level.getEntitiesOfClass(PlacedItemEntity.class,
                new net.minecraft.world.phys.AABB(msg.pos).inflate(0.1))) {
            if (other.face() == msg.face && BlockPos.containing(other.position().subtract(msg.face.getStepX() * 0.05,
                    msg.face.getStepY() * 0.05, msg.face.getStepZ() * 0.05)).equals(msg.pos)) {
                there.add(other);
            }
        }
        // up to two on one face of a block, blocks and items alike
        if (there.size() >= MAX_PER_FACE) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "Only " + MAX_PER_FACE + " items fit on one side of a block."), true);
            return;
        }
        float scale = held.getItem() instanceof net.minecraft.world.item.BlockItem ? PlacedItemEntity.BLOCK_SCALE :
                PlacedItemEntity.ITEM_SCALE;
        Vec3 point = freeSpot(msg.pos, msg.face, PlacedItemEntity.snap(msg.pos, msg.face, msg.hit), scale, there);
        if (point == null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("No room left on that side."), true);
            return;
        }
        ItemStack one = held.copyWithCount(1);
        level.addFreshEntity(PlacedItemEntity.create(level, one, msg.face, point));
        if (!player.isCreative()) held.shrink(1);
        level.playSound(null, msg.pos, net.minecraft.sounds.SoundEvents.ITEM_FRAME_ADD_ITEM,
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
    }
}
