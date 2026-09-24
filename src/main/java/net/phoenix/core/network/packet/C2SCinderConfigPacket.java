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
    private final long[] posKeys;
    private final char[] posChars;
    private final int[] posBaseIndices;
    private final int[] posCandidateIndices;

    private C2SCinderConfigPacket(InteractionHand hand, int[] sliceKeys, int[] sliceValues, char[] prefChars,
                                  int[] prefBaseIndices, int[] prefCandidateIndices, long[] posKeys, char[] posChars,
                                  int[] posBaseIndices, int[] posCandidateIndices) {
        this.hand = hand;
        this.sliceKeys = sliceKeys;
        this.sliceValues = sliceValues;
        this.prefChars = prefChars;
        this.prefBaseIndices = prefBaseIndices;
        this.prefCandidateIndices = prefCandidateIndices;
        this.posKeys = posKeys;
        this.posChars = posChars;
        this.posBaseIndices = posBaseIndices;
        this.posCandidateIndices = posCandidateIndices;
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

        List<CinderSchemaData.PositionPreferenceEntry> positionPrefs = rawPattern instanceof BlockPattern pattern ?
                CinderSchemaData.encodePositionPreferences(pattern, schemaInfo) : List.of();
        long[] posKeys = new long[positionPrefs.size()];
        char[] posChars = new char[positionPrefs.size()];
        int[] posBaseIndices = new int[positionPrefs.size()];
        int[] posCandidateIndices = new int[positionPrefs.size()];
        for (int j = 0; j < positionPrefs.size(); j++) {
            var pref = positionPrefs.get(j);
            posKeys[j] = pref.pos();
            posChars[j] = pref.predicateChar();
            posBaseIndices[j] = pref.baseIndex();
            posCandidateIndices[j] = pref.candidateIndex();
        }

        return new C2SCinderConfigPacket(hand, sliceKeys, sliceValues, prefChars, prefBaseIndices,
                prefCandidateIndices, posKeys, posChars, posBaseIndices, posCandidateIndices);
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

        int posCount = buf.readVarInt();
        this.posKeys = new long[posCount];
        this.posChars = new char[posCount];
        this.posBaseIndices = new int[posCount];
        this.posCandidateIndices = new int[posCount];
        for (int i = 0; i < posCount; i++) {
            posKeys[i] = buf.readVarLong();
            posChars[i] = buf.readChar();
            posBaseIndices[i] = buf.readVarInt();
            posCandidateIndices[i] = buf.readVarInt();
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

        buf.writeVarInt(posKeys.length);
        for (int i = 0; i < posKeys.length; i++) {
            buf.writeVarLong(posKeys[i]);
            buf.writeChar(posChars[i]);
            buf.writeVarInt(posBaseIndices[i]);
            buf.writeVarInt(posCandidateIndices[i]);
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
                    msg.prefBaseIndices, msg.prefCandidateIndices, msg.posKeys, msg.posChars, msg.posBaseIndices,
                    msg.posCandidateIndices);
        });
        ctx.setPacketHandled(true);
    }
}
