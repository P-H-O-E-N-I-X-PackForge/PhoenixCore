package net.phoenix.core.network.packet;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.common.block.cinema.CinemaPage;
import net.phoenix.core.common.block.cinema.CinemaScreenBlockEntity;

import java.util.ArrayList;
import java.util.List;

public class C2SCinemaGivePacket implements CustomPacketPayload {

    public static final Type<C2SCinemaGivePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2s_cinema_give"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SCinemaGivePacket> STREAM_CODEC = StreamCodec
            .ofMember(C2SCinemaGivePacket::encode, C2SCinemaGivePacket::new);

    private static final double MAX_DISTANCE_SQ = 12.0 * 12.0;
    private static final String CLAIMED_TAG = "phoenixcore_cinema_gives";

    private final BlockPos pos;
    private final String action;

    public C2SCinemaGivePacket(BlockPos pos, String action) {
        this.pos = pos;
        this.action = action;
    }

    private C2SCinemaGivePacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.action = buf.readUtf(256);
    }

    private void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(action, 256);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SCinemaGivePacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5) >
                    MAX_DISTANCE_SQ) {
                return;
            }
            if (!player.level().isLoaded(msg.pos)) return;
            if (!(player.level().getBlockEntity(msg.pos) instanceof CinemaScreenBlockEntity screen)) return;
            String wanted = msg.action.trim();
            if (!offers(screen, wanted)) return;

            String[] words = wanted.substring("give:".length()).trim().split("\\s+");
            ResourceLocation id = ResourceLocation.tryParse(words[0]);
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
                player.displayClientMessage(Component.literal("This screen offers an item that does not exist: " +
                        words[0]).withStyle(ChatFormatting.RED), true);
                return;
            }
            int count = 1;
            boolean repeat = false;
            for (int i = 1; i < words.length; i++) {
                if (words[i].equalsIgnoreCase("repeat")) repeat = true;
                else if (words[i].equalsIgnoreCase("once")) repeat = false;
                else {
                    try {
                        count = Integer.parseInt(words[i]);
                    } catch (NumberFormatException ignored) {}
                }
            }
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
            count = Math.max(1, Math.min(stack.getMaxStackSize(), count));
            stack.setCount(count);

            String key = player.level().dimension().location() + "|" + msg.pos.asLong() + "|" + wanted;
            if (!repeat) {
                CompoundTag data = persisted(player);
                ListTag claimed = data.getList(CLAIMED_TAG, Tag.TAG_STRING);
                if (claimed.contains(StringTag.valueOf(key))) {
                    player.displayClientMessage(Component.literal("You have already taken this.")
                            .withStyle(ChatFormatting.GRAY), true);
                    return;
                }
                claimed.add(StringTag.valueOf(key));
                data.put(CLAIMED_TAG, claimed);
            }
            player.getInventory().placeItemBackInInventory(stack.copy());
            player.displayClientMessage(Component.literal("Received " + count + "x ").append(stack.getHoverName())
                    .withStyle(ChatFormatting.GREEN), true);
        });
    }

    private static boolean offers(CinemaScreenBlockEntity screen, String action) {
        if (!action.startsWith("give:")) return false;
        List<String> actions = new ArrayList<>();
        actions.add(screen.getAction().trim());
        for (Component line : screen.getLines()) {
            CinemaPage page = CinemaPage.parse(line.getString());
            for (CinemaPage.Button button : page.buttons()) actions.add(button.action().trim());
            for (CinemaPage.Picture picture : page.pictures()) actions.add(picture.action().trim());
        }
        return actions.contains(action);
    }

    private static CompoundTag persisted(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }
}
