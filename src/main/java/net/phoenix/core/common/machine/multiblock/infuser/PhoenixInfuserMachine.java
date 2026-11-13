package net.phoenix.core.common.machine.multiblock.infuser;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ItemBusPartMachine;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class PhoenixInfuserMachine extends MultiblockControllerMachine {

    public static final int BURN = 0;
    public static final int RENEW = 1;
    public static final int MAX_ASH = 200;

    public static final double RENEW_MIN_HEAT = 40.0;
    private static final double RENEW_HEAT_COST = 15.0;
    private static final long UPKEEP = 2048L;

    private static final long EU_PER_WORTH = 24_000L;
    private static final long MAX_RESERVE = 40_000_000L;
    private static final long MAX_OUT = 16_384L;

    @SaveField
    @SyncToClient
    private int heatX10;

    @SaveField
    @SyncToClient
    private int ash;

    @SaveField
    @SyncToClient
    private int mode;

    @SaveField
    @SyncToClient
    private boolean lit;

    @SaveField
    private long reserve;

    @SyncToClient
    private int burned;

    @SyncToClient
    private int renewed;

    @SyncToClient
    private int reserveKilo;

    @SyncToClient
    private boolean powered = true;

    @SyncToClient
    private int energyUse;

    @SyncToClient
    private int emitting;

    private double heat;
    private boolean loaded;
    private int cooldown;
    private final Deque<ItemStack> pending = new ArrayDeque<>();
    private final ConditionalSubscriptionHandler tickHandler;

    public PhoenixInfuserMachine(BlockEntityCreationInfo info) {
        super(info);
        this.tickHandler = new ConditionalSubscriptionHandler(this, this::infuserTick, this::isFormed);
    }

    private void load() {
        if (loaded) return;
        loaded = true;
        heat = heatX10 / 10.0;
    }

    public double heat() {
        return heatX10 / 10.0;
    }

    public int ash() {
        return ash;
    }

    public int mode() {
        return mode;
    }

    public boolean lit() {
        return lit;
    }

    public int burned() {
        return burned;
    }

    public int renewed() {
        return renewed;
    }

    public int reserveKilo() {
        return reserveKilo;
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

    public void control(int which, int value) {
        if (which == 0) {
            mode = value == RENEW ? RENEW : BURN;
            syncDataHolder.markClientSyncFieldDirty("mode");
        }
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

    public static int renewCost(ItemStack stack) {
        if (!stack.isDamaged() || stack.getMaxDamage() <= 0) return 0;
        return Math.max(1, (int) Math.ceil(8.0 * stack.getDamageValue() / stack.getMaxDamage()));
    }

    private boolean burnOne() {
        for (ItemBusPartMachine bus : buses(IO.IN)) {
            var inv = bus.getInventory();
            for (int slot = 0; slot < inv.getSlots(); slot++) {
                ItemStack got = inv.extractItem(slot, 1, false);
                if (got.isEmpty()) continue;
                int value = worth(got);
                heat = Math.min(100.0, heat + value * 6.0);
                ash = Math.min(MAX_ASH, ash + value);
                reserve = Math.min(MAX_RESERVE, reserve + value * EU_PER_WORTH);
                if (getLevel().random.nextInt(4) == 0) {
                    pending.add(ChemicalHelper.get(TagPrefix.dust, GTMaterials.Ash, 1));
                }
                burned++;
                syncDataHolder.markClientSyncFieldDirty("burned");
                cooldown = 8;
                return true;
            }
        }
        return false;
    }

    private boolean renewOne() {
        if (heat < RENEW_MIN_HEAT) return false;
        for (ItemBusPartMachine bus : buses(IO.IN)) {
            var inv = bus.getInventory();
            for (int slot = 0; slot < inv.getSlots(); slot++) {
                ItemStack look = inv.getStackInSlot(slot);
                int cost = renewCost(look);
                if (cost <= 0 || cost > ash) continue;
                ItemStack got = inv.extractItem(slot, 1, false);
                if (got.isEmpty()) continue;
                got.setDamageValue(0);
                pending.add(got);
                ash -= cost;
                heat = Math.max(0.0, heat - RENEW_HEAT_COST);
                renewed++;
                syncDataHolder.markClientSyncFieldDirty("renewed");
                cooldown = 20;
                return true;
            }
        }
        return false;
    }

    private void deliver() {
        while (!pending.isEmpty()) {
            ItemStack head = pending.pollFirst();
            for (ItemBusPartMachine bus : buses(IO.OUT)) {
                var inv = bus.getInventory();
                for (int slot = 0; slot < inv.getSlots() && !head.isEmpty(); slot++) {
                    head = inv.insertItem(slot, head, false);
                }
                if (head.isEmpty()) break;
            }
            if (!head.isEmpty()) {
                pending.addFirst(head);
                return;
            }
        }
    }

    private void infuserTick() {
        if (getLevel() == null || getLevel().isClientSide) return;
        load();

        long upkeepNow = fitToHatches(UPKEEP);
        boolean ok = inputCapacity() > 0 && energyAvailable() >= upkeepNow;
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
        if (ok) takeEnergy(upkeepNow);

        heat = Math.max(0.0, heat - 0.04);
        if (cooldown > 0) cooldown--;
        if (ok && cooldown <= 0 && pending.size() < 8) {
            if (mode == BURN) burnOne();
            else renewOne();
        }

        long out = 0;
        if (ok && reserve > 0) {
            long give = Math.min(reserve, MAX_OUT);
            out = giveEnergy(give);
            reserve -= out;
        }
        if (getLevel().getGameTime() % 10 == 0) deliver();

        powered = ok;
        energyUse = (int) UPKEEP;
        emitting = (int) Math.min(Integer.MAX_VALUE, out);
        publish();
    }

    private void publish() {
        boolean push = getLevel() != null && getLevel().getGameTime() % 2 == 0;
        heatX10 = (int) Math.round(heat * 10.0);
        reserveKilo = (int) Math.min(Integer.MAX_VALUE, reserve / 1000L);
        if (push) {
            for (String name : new String[] { "heatX10", "ash", "reserveKilo", "powered", "energyUse", "emitting" }) {
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
                        new InfuserPackets.Open(getBlockPos()));
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(isRemote());
        }
        return super.onUse(context);
    }
}
