package net.phoenix.core.network.packet;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.mui.MultiblockSchemaInfo;
import com.gregtechceu.gtceu.api.multiblock.MultiPredicate;
import com.gregtechceu.gtceu.api.multiblock.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.multiblock.predicates.BasePredicate;
import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.common.item.cinder.CinderCoreItem;
import net.phoenix.core.common.item.cinder.CinderSchemaData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class C2SCinderConfigPacket {

    private final InteractionHand hand;
    private final int[] sliceKeys;
    private final int[] sliceValues;
    private final char[] prefChars;
    private final int[] prefBaseIndices;
    private final int[] prefCandidateIndices;

    private C2SCinderConfigPacket(InteractionHand hand, int[] sliceKeys, int[] sliceValues, char[] prefChars,
                                  int[] prefBaseIndices, int[] prefCandidateIndices) {
        this.hand = hand;
        this.sliceKeys = sliceKeys;
        this.sliceValues = sliceValues;
        this.prefChars = prefChars;
        this.prefBaseIndices = prefBaseIndices;
        this.prefCandidateIndices = prefCandidateIndices;
    }

    public static C2SCinderConfigPacket fromSchema(InteractionHand hand, MultiblockMachineDefinition definition,
                                                   MultiblockSchemaInfo schemaInfo) {
        var sliceRepeats = schemaInfo.getUserSliceRepeats();
        int[] sliceKeys = new int[sliceRepeats.size()];
        int[] sliceValues = new int[sliceRepeats.size()];
        int i = 0;
        for (var entry : sliceRepeats.int2IntEntrySet()) {
            sliceKeys[i] = entry.getIntKey();
            sliceValues[i] = entry.getIntValue();
            i++;
        }

        List<Character> chars = new ArrayList<>();
        List<Integer> baseIndices = new ArrayList<>();
        List<Integer> candidateIndices = new ArrayList<>();

        var structureHelper = schemaInfo.getStructureHelper();
        var blockPreferences = structureHelper != null ? structureHelper.getBlockPreferences() : null;
        var patternSupplier = definition.getStructurePatterns().get(MultiblockControllerMachine.DEFAULT_STRUCTURE);
        Object rawPattern = patternSupplier != null ? patternSupplier.get() : null;

        if (blockPreferences != null && !blockPreferences.isEmpty() && rawPattern instanceof BlockPattern pattern) {
            for (var cell : blockPreferences.cellSet()) {
                MultiPredicate predicate = cell.getRowKey();
                BasePredicate base = cell.getColumnKey();
                BlockInfo chosen = cell.getValue();
                if (predicate == null || base == null || chosen == null) continue;

                var charEntry = pattern.getPredicates().char2ObjectEntrySet().stream()
                        .filter(e -> e.getValue().equals(predicate))
                        .findFirst().orElse(null);
                if (charEntry == null) continue;

                chars.add(charEntry.getCharKey());
                baseIndices.add(predicate.predicates().indexOf(base));
                candidateIndices.add(base.getCandidates().indexOf(chosen));
            }
        }

        char[] prefChars = new char[chars.size()];
        int[] prefBaseIndices = new int[baseIndices.size()];
        int[] prefCandidateIndices = new int[candidateIndices.size()];
        for (int j = 0; j < chars.size(); j++) {
            prefChars[j] = chars.get(j);
            prefBaseIndices[j] = baseIndices.get(j);
            prefCandidateIndices[j] = candidateIndices.get(j);
        }

        return new C2SCinderConfigPacket(hand, sliceKeys, sliceValues, prefChars, prefBaseIndices,
                prefCandidateIndices);
    }

    public C2SCinderConfigPacket(FriendlyByteBuf buf) {
        this.hand = buf.readEnum(InteractionHand.class);

        int sliceCount = buf.readVarInt();
        this.sliceKeys = new int[sliceCount];
        this.sliceValues = new int[sliceCount];
        for (int i = 0; i < sliceCount; i++) {
            sliceKeys[i] = buf.readVarInt();
            sliceValues[i] = buf.readVarInt();
        }

        int prefCount = buf.readVarInt();
        this.prefChars = new char[prefCount];
        this.prefBaseIndices = new int[prefCount];
        this.prefCandidateIndices = new int[prefCount];
        for (int i = 0; i < prefCount; i++) {
            prefChars[i] = buf.readChar();
            prefBaseIndices[i] = buf.readVarInt();
            prefCandidateIndices[i] = buf.readVarInt();
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(hand);

        buf.writeVarInt(sliceKeys.length);
        for (int i = 0; i < sliceKeys.length; i++) {
            buf.writeVarInt(sliceKeys[i]);
            buf.writeVarInt(sliceValues[i]);
        }

        buf.writeVarInt(prefChars.length);
        for (int i = 0; i < prefChars.length; i++) {
            buf.writeChar(prefChars[i]);
            buf.writeVarInt(prefBaseIndices[i]);
            buf.writeVarInt(prefCandidateIndices[i]);
        }
    }

    public static void handle(C2SCinderConfigPacket msg, Supplier<NetworkEvent.Context> ctxGetter) {
        NetworkEvent.Context ctx = ctxGetter.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ItemStack stack = player.getItemInHand(msg.hand);
            if (!(stack.getItem() instanceof CinderCoreItem)) return;

            CinderSchemaData.applyConfiguration(stack, msg.sliceKeys, msg.sliceValues, msg.prefChars,
                    msg.prefBaseIndices, msg.prefCandidateIndices);
        });
        ctx.setPacketHandled(true);
    }
}
