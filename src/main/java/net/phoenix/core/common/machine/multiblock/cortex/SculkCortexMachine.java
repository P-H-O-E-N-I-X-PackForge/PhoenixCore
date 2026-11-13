package net.phoenix.core.common.machine.multiblock.cortex;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ItemBusPartMachine;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SculkCortexMachine extends MultiblockControllerMachine {

    public static final int MAX_LEVEL = 8;

    private static final double HUNGER_DRAIN = 0.02;
    private static final int WITHER_AFTER = 600;
    private static final int MAX_PENDING = 12;

    @SaveField
    @SyncToClient
    private int level;

    @SaveField
    @SyncToClient
    private int xpX10;

    @SaveField
    @SyncToClient
    private int hungerX10 = 600;

    @SaveField
    @SyncToClient
    private int withers;

    @SaveField
    @SyncToClient
    private boolean lit;

    @SaveField
    private String engramData = "";

    @SyncToClient
    private int engramCount;

    @SyncToClient
    private String lastLearned = "";

    @SyncToClient
    private int digested;

    @SyncToClient
    private boolean starving;

    @SyncToClient
    private boolean powered = true;

    @SyncToClient
    private int energyUse;

    @SyncToClient
    private int emitting;

    private double xp;
    private double hunger = 60.0;
    private boolean loaded;
    private int cooldown;
    private int starve;
    private final Set<String> engrams = new LinkedHashSet<>();
    private final Deque<ItemStack> pending = new ArrayDeque<>();
    private final ConditionalSubscriptionHandler tickHandler;

    public SculkCortexMachine(BlockEntityCreationInfo info) {
        super(info);
        this.tickHandler = new ConditionalSubscriptionHandler(this, this::cortexTick, this::isFormed);
    }

    public static double xpToNext(int level) {
        return 40.0 * (level + 1) * (1.0 + 0.3 * level);
    }

    public static int capacity(int level) {
        return 4 + 4 * level;
    }

    private static long upkeep(int level) {
        return 256L * (1 + level);
    }

    private void load() {
        if (loaded) return;
        loaded = true;
        xp = xpX10 / 10.0;
        hunger = hungerX10 / 10.0;
        engrams.clear();
        if (!engramData.isEmpty()) for (String s : engramData.split(";")) if (!s.isEmpty()) engrams.add(s);
    }

    public int level() {
        return level;
    }

    public double xp() {
        return xpX10 / 10.0;
    }

    public double hunger() {
        return hungerX10 / 10.0;
    }

    public int withers() {
        return withers;
    }

    public boolean lit() {
        return lit;
    }

    public int engramCount() {
        return engramCount;
    }

    public int capacity() {
        return capacity(level);
    }

    public String lastLearned() {
        return lastLearned;
    }

    public int digested() {
        return digested;
    }

    public boolean starving() {
        return starving;
    }

    public boolean powerOk() {
        return powered;
    }

    public int energyUse() {
        return energyUse;
    }

    public int emitting() {
        return emitting;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        tickHandler.initialize(getLevel());
    }

    @Override
    public void onUnload() {
        tickHandler.unsubscribe();
        super.onUnload();
    }

    @Override
    public void formStructure(String substructureName) {
        super.formStructure(substructureName);
        tickHandler.updateSubscription();
    }

    @Override
    public void invalidateStructure(String substructureName) {
        super.invalidateStructure(substructureName);
        tickHandler.updateSubscription();
    }

    private List<IEnergyContainer> energyHatches(IO io) {
        List<IEnergyContainer> found = new ArrayList<>();
        for (var part : getParts()) {
            for (var list : part.getRecipeHandlers()) {
                if (!list.isValid(io)) continue;
                for (IRecipeHandler<?> handler : list.getCapability(EURecipeCapability.CAP)) {
                    if (handler instanceof IEnergyContainer container) found.add(container);
                }
            }
        }
        return found;
    }

    private long inputCapacity() {
        long total = 0L;
        for (var c : energyHatches(IO.IN)) total += c.getEnergyCapacity();
        return total;
    }

    private long fitToHatches(long wanted) {
        long cap = inputCapacity();
        return cap <= 0 ? wanted : Math.min(wanted, Math.max(1L, cap * 3L / 4L));
    }

    private List<ItemBusPartMachine> buses(IO io) {
        List<ItemBusPartMachine> found = new ArrayList<>();
        for (var part : getParts()) {
            if (part instanceof ItemBusPartMachine bus && bus.getInventory().getHandlerIO() == io) found.add(bus);
        }
        return found;
    }

    private long energyAvailable() {
        long total = 0L;
        for (var hatch : energyHatches(IO.IN)) total += hatch.getEnergyStored();
        return total;
    }

    private void takeEnergy(long amount) {
        long left = amount;
        for (var hatch : energyHatches(IO.IN)) {
            if (left <= 0) break;
            long take = Math.min(left, hatch.getEnergyStored());
            if (take > 0) {
                hatch.changeEnergy(-take);
                left -= take;
            }
        }
    }

    private long giveEnergy(long amount) {
        long left = amount;
        for (var hatch : energyHatches(IO.OUT)) {
            if (left <= 0) break;
            long room = Math.max(0L,
                    hatch.getEnergyCapacity() - hatch.getEnergyStored());
            long put = Math.min(left, room);
            if (put > 0) {
                hatch.changeEnergy(put);
                left -= put;
            }
        }
        return amount - left;
    }

    private static int worth(ItemStack stack) {
        Rarity rarity = stack.getRarity();
        return rarity == Rarity.EPIC ? 8 : rarity == Rarity.RARE ? 4 : rarity == Rarity.UNCOMMON ? 2 : 1;
    }

    private ItemStack takeOne() {
        for (ItemBusPartMachine bus : buses(IO.IN)) {
            var inv = bus.getInventory();
            for (int slot = 0; slot < inv.getSlots(); slot++) {
                if (inv.extractItem(slot, 1, true).isEmpty()) continue;
                ItemStack got = inv.extractItem(slot, 1, false);
                if (!got.isEmpty()) return got;
            }
        }
        return ItemStack.EMPTY;
    }

    private void digest() {
        ItemStack eaten = takeOne();
        if (eaten.isEmpty()) return;
        Item item = eaten.getItem();
        String id = BuiltInRegistries.ITEM.getKey(item).toString();
        int value = worth(eaten);
        boolean familiar = engrams.contains(id);
        if (!familiar && engrams.size() < capacity(level)) {
            engrams.add(id);
            lastLearned = item.getDescription().getString();
            syncDataHolder.markClientSyncFieldDirty("lastLearned");
            engramData = String.join(";", engrams);
        }
        xp += value * (familiar ? 1.0 : 1.5);
        hunger = Math.min(100.0, hunger + value * 10.0);

        int sculk = 1 + (familiar ? 1 : 0) + level / 3;
        pending.add(new ItemStack(Items.SCULK, sculk));
        var random = getLevel().random;
        if (level >= 2 && random.nextInt(6) == 0) pending.add(new ItemStack(Items.ECHO_SHARD));
        if (level >= 4 && random.nextInt(25) == 0) pending.add(new ItemStack(Items.SCULK_CATALYST));
        int base = Math.max(10, 60 - 5 * level);
        cooldown = familiar ? Math.max(4, base / 2) : base;
        digested++;
        syncDataHolder.markClientSyncFieldDirty("digested");

        while (level < MAX_LEVEL && xp >= xpToNext(level)) {
            xp -= xpToNext(level);
            level++;
            syncDataHolder.markClientSyncFieldDirty("level");
        }
    }

    private void wither() {
        level = Math.max(0, level - 1);
        xp = 0.0;
        hunger = 30.0;
        starve = 0;
        withers++;
        List<String> kept = new ArrayList<>();
        int i = 0;
        for (String e : engrams) if (i++ % 2 == 0 && kept.size() < capacity(level)) kept.add(e);
        engrams.clear();
        engrams.addAll(kept);
        engramData = String.join(";", engrams);
        syncDataHolder.markClientSyncFieldDirty("level");
        syncDataHolder.markClientSyncFieldDirty("withers");
    }

    private void deliver() {
        while (!pending.isEmpty()) {
            ItemStack head = pending.peekFirst();
            for (ItemBusPartMachine bus : buses(IO.OUT)) {
                var inv = bus.getInventory();
                for (int slot = 0; slot < inv.getSlots() && !head.isEmpty(); slot++) {
                    head = inv.insertItem(slot, head, false);
                }
                if (head.isEmpty()) break;
            }
            if (head.isEmpty()) {
                pending.pollFirst();
            } else {
                pending.pollFirst();
                pending.addFirst(head);
                return;
            }
        }
    }

    private void cortexTick() {
        if (getLevel() == null || getLevel().isClientSide) return;
        load();

        long need = upkeep(level);
        need = fitToHatches(need);
        boolean ok = inputCapacity() > 0 && energyAvailable() >= need;
        if (!lit) {
            if (!ok) {
                powered = false;
                energyUse = 0;
                emitting = 0;
                publish();
                return;
            }
            lit = true;
            syncDataHolder.markClientSyncFieldDirty("lit");
        }
        if (ok) takeEnergy(need);

        hunger = Math.max(0.0, hunger - (ok ? HUNGER_DRAIN : HUNGER_DRAIN * 3.0));
        if (cooldown > 0) cooldown--;
        if (ok && cooldown <= 0 && pending.size() < MAX_PENDING) digest();

        long out = 0;
        if (ok && hunger > 0.0 && !engrams.isEmpty()) {
            out = giveEnergy(engrams.size() * (long) (level + 1) * 48L);
        }

        if (hunger <= 0.0) {
            if (++starve >= WITHER_AFTER) wither();
        } else {
            starve = 0;
        }
        if (getLevel().getGameTime() % 10 == 0) deliver();

        powered = ok;
        energyUse = (int) Math.min(Integer.MAX_VALUE, need);
        emitting = (int) Math.min(Integer.MAX_VALUE, out);
        starving = starve > 0;
        publish();
    }

    private void publish() {
        boolean push = getLevel() != null && getLevel().getGameTime() % 2 == 0;
        xpX10 = (int) Math.round(xp * 10.0);
        hungerX10 = (int) Math.round(hunger * 10.0);
        engramCount = engrams.size();
        if (push) {
            for (String name : new String[] { "xpX10", "hungerX10", "engramCount", "powered", "energyUse", "emitting",
                    "starving" }) {
                syncDataHolder.markClientSyncFieldDirty(name);
            }
        }
    }

    @Override
    public net.minecraft.world.InteractionResult onUse(com.gregtechceu.gtceu.utils.ExtendedUseOnContext context) {
        if (isFormed() && !context.getPlayer().isShiftKeyDown() &&
                net.phoenix.core.common.machine.multiblock.ScreenUse.opens(context.getItemInHand())) {
            if (context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player) {
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                        new S2CCortexOpenPacket(getBlockPos()));
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(isRemote());
        }
        return super.onUse(context);
    }
}
