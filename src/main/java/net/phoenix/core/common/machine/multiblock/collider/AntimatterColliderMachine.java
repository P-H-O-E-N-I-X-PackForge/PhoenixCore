package net.phoenix.core.common.machine.multiblock.collider;

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
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ItemBusPartMachine;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class AntimatterColliderMachine extends MultiblockControllerMachine {

    public static final int POWER = 0;
    public static final int ITEMS = 1;
    public static final int TRAP_CAP = 2000;

    public static final int AM_PER_ITEM = 50;

    public static final long EU_PER_AM = 100_000L;
    private static final int MATTER_MB = 100;
    private static final long BASE_UPKEEP = 4096L;
    private static final long BEAM_UPKEEP = 200L;
    private static final int MAX_BURN_PER_TICK = 5;

    @SaveField
    @SyncToClient
    private int beam;

    @SaveField
    @SyncToClient
    private int mode;

    @SaveField
    @SyncToClient
    private int trap;

    @SaveField
    @SyncToClient
    private boolean lit;

    @SyncToClient
    private int collisions;

    @SyncToClient
    private int itemsMade;

    @SyncToClient
    private boolean powered = true;

    @SyncToClient
    private boolean matterOk = true;

    @SyncToClient
    private int energyUse;

    @SyncToClient
    private int emitting;

    private int cooldown;
    private final Deque<ItemStack> pending = new ArrayDeque<>();
    private final ConditionalSubscriptionHandler tickHandler;

    public AntimatterColliderMachine(BlockEntityCreationInfo info) {
        super(info);
        this.tickHandler = new ConditionalSubscriptionHandler(this, this::colliderTick, this::isFormed);
    }

    public int beam() {
        return beam;
    }

    public int mode() {
        return mode;
    }

    public int trap() {
        return trap;
    }

    public boolean lit() {
        return lit;
    }

    public int collisions() {
        return collisions;
    }

    public int itemsMade() {
        return itemsMade;
    }

    public boolean powerOk() {
        return powered;
    }

    public boolean matterOk() {
        return matterOk;
    }

    public int energyUse() {
        return energyUse;
    }

    public int emitting() {
        return emitting;
    }

    public void control(int which, int value) {
        if (which == 0) {
            beam = Math.max(0, Math.min(100, value));
            syncDataHolder.markClientSyncFieldDirty("beam");
        } else if (which == 1) {
            mode = value == ITEMS ? ITEMS : POWER;
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

    private boolean takeMatter() {
        for (var part : getParts()) {
            if (!(part instanceof FluidHatchPartMachine hatch) || hatch.tank.getHandlerIO() != IO.IN) continue;
            for (int tank = 0; tank < hatch.tank.getTanks(); tank++) {
                FluidStack inside = hatch.tank.getFluidInTank(tank);
                if (inside.getAmount() < MATTER_MB) continue;
                FluidStack taken = hatch.tank.drainInternal(new FluidStack(inside.getFluid(), MATTER_MB),
                        IFluidHandler.FluidAction.EXECUTE);
                if (taken.getAmount() >= MATTER_MB) return true;
            }
        }
        return false;
    }

    private static ItemStack rareItem(RandomSource random) {
        int roll = random.nextInt(100);
        if (roll < 40) return ChemicalHelper.get(TagPrefix.dust, GTMaterials.Neutronium, 1);
        if (roll < 65) return ChemicalHelper.get(TagPrefix.ingot, GTMaterials.Naquadria, 1);
        if (roll < 80) return ChemicalHelper.get(TagPrefix.dust, GTMaterials.Naquadah, 2);
        if (roll < 92) return ChemicalHelper.get(TagPrefix.plate, GTMaterials.Neutronium, 1);
        return ChemicalHelper.get(TagPrefix.gem, GTMaterials.NetherStar, 1);
    }

    private void deliver() {
        while (!pending.isEmpty()) {
            ItemStack head = pending.pollFirst();
            for (var part : getParts()) {
                if (!(part instanceof ItemBusPartMachine bus) || bus.getInventory().getHandlerIO() != IO.OUT) continue;
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

    private void colliderTick() {
        if (getLevel() == null || getLevel().isClientSide) return;

        long need = BASE_UPKEEP + beam * BEAM_UPKEEP;
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

        if (cooldown > 0) cooldown--;
        if (ok && beam > 0 && cooldown <= 0) {
            if (takeMatter()) {
                matterOk = true;
                int made = 2 + beam / 20 + getLevel().random.nextInt(3);
                trap = Math.min(TRAP_CAP, trap + made);
                collisions++;
                syncDataHolder.markClientSyncFieldDirty("collisions");
                cooldown = Math.max(6, 40 - beam * 3 / 10);
            } else {
                matterOk = false;
            }
        }

        if (!ok) trap = Math.max(0, trap - 2);

        long out = 0;
        if (ok && trap > 0) {
            if (mode == POWER) {
                long put = giveEnergy(Math.min(trap, MAX_BURN_PER_TICK) * EU_PER_AM);
                int spent = (int) Math.ceil(put / (double) EU_PER_AM);
                trap = Math.max(0, trap - spent);
                out = put;
            } else if (getLevel().getGameTime() % 20 == 0 && trap >= AM_PER_ITEM && pending.size() < 8) {
                trap -= AM_PER_ITEM;
                pending.add(rareItem(getLevel().random));
                itemsMade++;
                syncDataHolder.markClientSyncFieldDirty("itemsMade");
            }
        }
        if (getLevel().getGameTime() % 10 == 0) deliver();

        powered = ok;
        energyUse = (int) Math.min(Integer.MAX_VALUE, need);
        emitting = (int) Math.min(Integer.MAX_VALUE, out);
        publish();
    }

    private void publish() {
        boolean push = getLevel() != null && getLevel().getGameTime() % 2 == 0;
        if (push) {
            for (String name : new String[] { "trap", "powered", "matterOk", "energyUse", "emitting" }) {
                syncDataHolder.markClientSyncFieldDirty(name);
            }
        }
    }

    @Override
    public net.minecraft.world.InteractionResult onUse(com.gregtechceu.gtceu.utils.ExtendedUseOnContext context) {
        if (isFormed() && net.phoenix.core.common.machine.multiblock.ScreenUse.opens(context.getItemInHand())) {
            if (context.getPlayer().isShiftKeyDown()) {

                if (!isRemote()) {
                    control(1, mode == POWER ? ITEMS : POWER);
                    context.getPlayer().displayClientMessage(Component.literal(
                            mode == POWER ? "Collider: annihilating the trap for POWER." :
                                    "Collider: condensing the trap into ITEMS."),
                            true);
                }
                return net.minecraft.world.InteractionResult.sidedSuccess(isRemote());
            }
            if (context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player) {
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                        new ColliderPackets.Open(getBlockPos()));
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(isRemote());
        }
        return super.onUse(context);
    }
}
