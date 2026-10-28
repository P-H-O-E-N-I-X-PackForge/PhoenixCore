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

    private static void place(ServerPlayer player, C2SPlaceItemPacket msg) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || player.isSpectator()) return;
        var level = player.serverLevel();
        if (!level.isLoaded(msg.pos) || level.getBlockState(msg.pos).isAir()) return;
        if (player.distanceToSqr(Vec3.atCenterOf(msg.pos)) > 64.0) return;
        if (!player.mayInteract(level, msg.pos)) return;
        // the spot in front of the face has to be free of another block
        if (level.getBlockState(msg.pos.relative(msg.face)).isSolidRender(level, msg.pos.relative(msg.face))) return;
        Vec3 point = PlacedItemEntity.snap(msg.pos, msg.face, msg.hit);
        for (PlacedItemEntity other : level.getEntitiesOfClass(PlacedItemEntity.class,
                new net.minecraft.world.phys.AABB(point, point).inflate(0.12))) {
            if (other.face() == msg.face && other.position().distanceToSqr(point) < 0.1) return;
        }
        ItemStack one = held.copyWithCount(1);
        level.addFreshEntity(PlacedItemEntity.create(level, one, msg.face, point));
        if (!player.isCreative()) held.shrink(1);
        level.playSound(null, msg.pos, net.minecraft.sounds.SoundEvents.ITEM_FRAME_ADD_ITEM,
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
    }
}
