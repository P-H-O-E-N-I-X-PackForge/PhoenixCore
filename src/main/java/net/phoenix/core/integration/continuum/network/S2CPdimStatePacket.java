package net.phoenix.core.integration.continuum.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.continuum.client.pdim.PdimClientState;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class S2CPdimStatePacket {

    private final boolean inside;
    private final float gravity;
    private final Map<ResourceLocation, Float> mine;
    private final Map<ResourceLocation, net.phoenix.core.integration.continuum.pdim.PdimCosts.Cost> costs;
    private final boolean everyVisit;

    public S2CPdimStatePacket(boolean inside, float gravity, Map<ResourceLocation, Float> mine,
                              Map<ResourceLocation, net.phoenix.core.integration.continuum.pdim.PdimCosts.Cost> costs,
                              boolean everyVisit) {
        this.inside = inside;
        this.gravity = gravity;
        this.mine = mine;
        this.costs = costs;
        this.everyVisit = everyVisit;
    }

    public S2CPdimStatePacket(FriendlyByteBuf buf) {
        this.inside = buf.readBoolean();
        this.gravity = buf.readFloat();
        int count = buf.readVarInt();
        this.mine = new HashMap<>();
        for (int i = 0; i < count; i++) mine.put(buf.readResourceLocation(), buf.readFloat());
        int costCount = buf.readVarInt();
        this.costs = new HashMap<>();
        for (int i = 0; i < costCount; i++) {
            ResourceLocation body = buf.readResourceLocation();
            costs.put(body, new net.phoenix.core.integration.continuum.pdim.PdimCosts.Cost(
                    buf.readResourceLocation(), buf.readVarInt()));
        }
        this.everyVisit = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(inside);
        buf.writeFloat(gravity);
        buf.writeVarInt(mine.size());
        mine.forEach((body, g) -> {
            buf.writeResourceLocation(body);
            buf.writeFloat(g);
        });
        buf.writeVarInt(costs.size());
        costs.forEach((body, cost) -> {
            buf.writeResourceLocation(body);
            buf.writeResourceLocation(cost.item());
            buf.writeVarInt(cost.count());
        });
        buf.writeBoolean(everyVisit);
    }

    public static void handle(S2CPdimStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient())
                PdimClientState.accept(msg.inside, msg.gravity, msg.mine, msg.costs, msg.everyVisit);
        });
        ctx.get().setPacketHandled(true);
    }
}
