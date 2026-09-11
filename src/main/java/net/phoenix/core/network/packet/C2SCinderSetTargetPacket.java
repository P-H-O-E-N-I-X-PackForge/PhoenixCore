package net.phoenix.core.network.packet;

import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import net.phoenix.core.common.item.cinder.CinderCoreItem;
import net.phoenix.core.common.item.cinder.CinderSchemaData;

import java.util.function.Supplier;

/** Sets which multiblock a Cinder Core is configured for - the one part of configuration that stays
 *  a manual, in-person choice rather than something AE2 could ever automate (see Rebirth Cinder Core
 *  design notes on why). */
public class C2SCinderSetTargetPacket {

    private final InteractionHand hand;
    private final ResourceLocation targetId;

    public C2SCinderSetTargetPacket(InteractionHand hand, ResourceLocation targetId) {
        this.hand = hand;
        this.targetId = targetId;
    }

    public C2SCinderSetTargetPacket(FriendlyByteBuf buf) {
        this.hand = buf.readEnum(InteractionHand.class);
        this.targetId = buf.readResourceLocation();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(hand);
        buf.writeResourceLocation(targetId);
    }

    public static void handle(C2SCinderSetTargetPacket msg, Supplier<NetworkEvent.Context> ctxGetter) {
        NetworkEvent.Context ctx = ctxGetter.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ItemStack stack = player.getItemInHand(msg.hand);
            if (!(stack.getItem() instanceof CinderCoreItem)) return;

            MachineDefinition definition = GTRegistries.MACHINES.get(msg.targetId);
            if (definition instanceof MultiblockMachineDefinition multiblock) {
                CinderSchemaData.setTarget(stack, multiblock);
            }
        });
        ctx.setPacketHandled(true);
    }
}
