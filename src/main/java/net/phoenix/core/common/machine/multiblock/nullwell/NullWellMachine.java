package net.phoenix.core.common.machine.multiblock.nullwell;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.mui.MachineUIPanelBuilder;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ItemBusPartMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.phoenix.core.client.nullwell.NullWellHistoryPage;
import net.phoenix.core.client.nullwell.NullWellHudPage;
import net.phoenixvine.wiki.ui.hud.CanvasWidget;
import net.phoenixvine.wiki.ui.hud.HudPanel;
import net.phoenixvine.wiki.ui.hud.HudTheme;
import net.phoenixvine.wiki.ui.hud.TabStrip;

import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.PanelSyncManager;

import java.util.ArrayList;
import java.util.List;

/**
 * The Null Well. Fluid fed into the hole adds to its mass; drawing on it turns mass into energy, which goes out
 * through the energy output hatches. Above the horizon limit the field loses its grip, and at zero stability the hole
 * breaks out of the chamber.
 */
public class NullWellMachine extends MultiblockControllerMachine implements IMuiMachine {

    /** Mass above which the field starts to slip. */
    public static final double LIMIT = 200.0;
    /** Mass the field can hold with no strain at all. */
    public static final double COMFORT = 120.0;
    /** The most mass a single millibucket can add, however dense. */
    private static final double MASS_PER_MB = 0.0004;
    /** Energy that one unit of mass gives up as radiation. */
    public static final long EU_PER_MASS = 2_000_000L;
    /** The most energy a tick of full draw can ask for. */
    public static final long MAX_EMIT = 32_768L;
    /** Most fluid the well will take in a tick at full feed. */
    public static final int MAX_FEED_MB = 200;

    @SaveField
    @SyncToClient
    private int massX100;

    @SaveField
    @SyncToClient
    private int stabilityX10 = 1000;

    @SaveField
    @SyncToClient
    private int feed;

    @SaveField
    @SyncToClient
    private int draw;

    @SaveField
    @SyncToClient
    private int breaches;

    /** Set the first time the well is given power. Until then nothing is held and nothing runs. */
    @SaveField
    @SyncToClient
    private boolean lit;

    @SyncToClient
    private boolean powered = true;

    @SyncToClient
    private int energyUse;

    @SyncToClient
    private int emitting;

    @SyncToClient
    private int feeding;

    private double mass;
    private double stability = 100.0;
    private boolean loaded;
    private double feedBuffer;
    /** Energy radiated since the last condensate was made. */
    private double condensate;
    private ItemStack pending = ItemStack.EMPTY;
    /** Energy of radiation it takes to condense one item. */
    private static final double EU_PER_ITEM = 4_000_000.0;
    private final ConditionalSubscriptionHandler tickHandler;

    public NullWellMachine(BlockEntityCreationInfo info) {
        super(info);
        this.tickHandler = new ConditionalSubscriptionHandler(this, this::wellTick, this::isFormed);
    }

    private void load() {
        if (loaded) return;
        loaded = true;
        mass = massX100 / 100.0;
        stability = stabilityX10 / 10.0;
    }

    private double shownMass;
    private float shownStability;
    private long shownAt;
    private boolean shownSeeded;

    private void ease() {
        long now = System.nanoTime();
        float dt = shownSeeded ? Math.min(0.25f, (now - shownAt) / 1.0e9f) : 1.0f;
        if (dt <= 0.0f) return;
        shownAt = now;
        float k = shownSeeded ? 1.0f - (float) Math.exp(-dt * 9.0f) : 1.0f;
        shownSeeded = true;
        shownMass += (massX100 / 100.0 - shownMass) * k;
        shownStability += (stabilityX10 / 10.0f - shownStability) * k;
    }

    public double mass() {
        if (isRemote()) {
            ease();
            return shownMass;
        }
        return massX100 / 100.0;
    }

    public float stability() {
        if (isRemote()) {
            ease();
            return shownStability;
        }
        return stabilityX10 / 10.0f;
    }

    public int feedSetting() {
        return feed;
    }

    public int drawSetting() {
        return draw;
    }

    public int breaches() {
        return breaches;
    }

    /** Whether the field has ever been lit. A well that was never powered is dormant. */
    public boolean lit() {
        return lit;
    }

    public boolean powerOk() {
        return powered;
    }

    public int energyUse() {
        return energyUse;
    }

    /** Energy going out last tick. */
    public int emitting() {
        return emitting;
    }

    /** Fluid going in last tick, in millibuckets. */
    public int feeding() {
        return feeding;
    }

    public Vec3 chamberCentre() {
        Direction back = getFrontFacing().getOpposite();
        return new Vec3(getBlockPos().getX() + 0.5 + back.getStepX() * 3.0, getBlockPos().getY() + 3.5,
                getBlockPos().getZ() + 0.5 + back.getStepZ() * 3.0);
    }

    /** Containment costs more the more there is to contain. */
    public static long containmentCost(double mass) {
        return 2048L + (long) (mass * 64.0);
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

    private List<FluidHatchPartMachine> inputHatches() {
        List<FluidHatchPartMachine> found = new ArrayList<>();
        for (var part : getParts()) {
            if (part instanceof FluidHatchPartMachine hatch && hatch.tank.getHandlerIO() == IO.IN) found.add(hatch);
        }
        return found;
    }

    private List<IEnergyContainer> energyHatches(IO io) {
        // anything the machine can draw energy through or push it into: ordinary hatches, wireless ones, whatever it is
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

    /** What the input hatches can hold between them: a weak hatch cannot be asked for more than it can hold. */
    private long inputCapacity() {
        long total = 0L;
        for (var c : energyHatches(IO.IN)) total += c.getEnergyCapacity();
        return total;
    }

    /** The most a tick of upkeep can honestly be, given what the hatches can hold. */
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

    private List<ItemBusPartMachine> outputBuses() {
        List<ItemBusPartMachine> found = new ArrayList<>();
        for (var part : getParts()) {
            if (part instanceof ItemBusPartMachine bus && bus.getInventory().getHandlerIO() == IO.OUT) found.add(bus);
        }
        return found;
    }

    private static ItemStack condensateItem(net.minecraft.util.RandomSource random) {
        int roll = random.nextInt(100);
        var m = GTMaterials.Neutronium;
        if (roll < 40) return ChemicalHelper.get(TagPrefix.dust, m, 1);
        if (roll < 65) return ChemicalHelper.get(TagPrefix.nugget, m, 3);
        if (roll < 80) return ChemicalHelper.get(TagPrefix.ingot, m, 1);
        if (roll < 92) return ChemicalHelper.get(TagPrefix.plate, m, 1);
        return ChemicalHelper.get(TagPrefix.rod, m, 2);
    }

    /** Try to put the waiting item into an output bus. */
    private void deliver() {
        if (pending.isEmpty()) return;
        for (ItemBusPartMachine bus : outputBuses()) {
            var inv = bus.getInventory();
            for (int slot = 0; slot < inv.getSlots() && !pending.isEmpty(); slot++) {
                pending = inv.insertItem(slot, pending, false);
            }
            if (pending.isEmpty()) return;
        }
    }

    private long roomForEnergy() {
        long room = 0L;
        for (var hatch : energyHatches(IO.OUT)) {
            room += Math.max(0L, hatch.getEnergyCapacity() - hatch.getEnergyStored());
        }
        return room;
    }

    private void giveEnergy(long amount) {
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
    }

    /** How heavy a millibucket of this fluid is, in units of mass. Denser fluids weigh more, up to a point. */
    private static double massOf(FluidStack stack) {
        if (stack.isEmpty()) return 0.0;
        double density = Math.abs(stack.getFluidType().getDensity(stack));
        double weight = Math.max(0.25, Math.min(20.0, density / 1000.0));
        return MASS_PER_MB * weight;
    }

    private void wellTick() {
        if (getLevel() == null || getLevel().isClientSide) return;
        load();

        long need = containmentCost(mass);
        need = fitToHatches(need);
        boolean ok = inputCapacity() > 0 && energyAvailable() >= need;
        if (!lit) {
            // dormant: the field does not start, and nothing in the well changes, until it is first given power
            if (!ok) {
                powered = false;
                energyUse = 0;
                emitting = 0;
                feeding = 0;
                publish();
                return;
            }
            lit = true;
            syncDataHolder.markClientSyncFieldDirty("lit");
        }
        if (ok) takeEnergy(need);

        // feed: fluid in, mass up
        int fedMb = 0;
        if (ok && feed > 0) {
            feedBuffer += MAX_FEED_MB * feed / 100.0;
            int whole = (int) feedBuffer;
            if (whole > 0) {
                for (FluidHatchPartMachine hatch : inputHatches()) {
                    if (whole <= 0) break;
                    for (int tank = 0; tank < hatch.tank.getTanks() && whole > 0; tank++) {
                        FluidStack inside = hatch.tank.getFluidInTank(tank);
                        if (inside.isEmpty()) continue;
                        FluidStack taken = hatch.tank.drainInternal(
                                new FluidStack(inside.getFluid(), Math.min(whole, inside.getAmount())),
                                IFluidHandler.FluidAction.EXECUTE);
                        if (taken.isEmpty()) continue;
                        mass += taken.getAmount() * massOf(taken);
                        whole -= taken.getAmount();
                        feedBuffer -= taken.getAmount();
                        fedMb += taken.getAmount();
                    }
                }
                feedBuffer = Math.min(feedBuffer, 2.0);
            }
        } else {
            feedBuffer = 0.0;
        }

        // draw: mass down, energy out
        long emitted = 0L;
        if (ok && draw > 0 && mass > 0.0) {
            long want = Math.round(MAX_EMIT * (draw / 100.0) * Math.min(1.0, mass / 20.0));
            want = Math.min(want, roomForEnergy());
            long affordable = (long) Math.floor(mass * EU_PER_MASS);
            emitted = Math.max(0L, Math.min(want, affordable));
            if (emitted > 0) {
                giveEnergy(emitted);
                mass -= emitted / (double) EU_PER_MASS;
            }
        }
        mass = Math.max(0.0, mass);

        // the radiation condenses into neutronium
        if (emitted > 0) condensate += emitted;
        if (pending.isEmpty() && condensate >= EU_PER_ITEM && !outputBuses().isEmpty()) {
            condensate -= EU_PER_ITEM;
            pending = condensateItem(getLevel().random);
        }
        if (getLevel().getGameTime() % 10 == 0) deliver();
        if (outputBuses().isEmpty()) condensate = Math.min(condensate, EU_PER_ITEM);

        // the field: it holds while powered and under the comfort mass, slips above the limit
        if (!ok) {
            // with nothing in the chamber there is nothing to lose
            if (mass > 0.5) stability -= 0.4;
        } else if (mass > LIMIT) {
            stability -= 0.15 + 1.2 * Math.min(1.0, (mass - LIMIT) / LIMIT);
        } else if (mass > COMFORT) {
            stability -= 0.01 * (mass - COMFORT) / (LIMIT - COMFORT);
        } else {
            stability += 0.06;
        }
        if (feed > 85) stability -= 0.02;
        stability = Math.max(0.0, Math.min(100.0, stability));
        if (stability <= 0.0) breakOut();

        powered = ok;
        energyUse = (int) Math.min(Integer.MAX_VALUE, need);
        emitting = (int) Math.min(Integer.MAX_VALUE, emitted);
        feeding = fedMb;
        publish();
    }

    /** The hole breaks free: most of its mass goes, and it eats a ragged ball out of the walls. */
    private void breakOut() {
        var level = getLevel();
        if (level == null) return;
        Vec3 c = chamberCentre();
        int radius = Math.min(6, 3 + (int) (mass / 60.0));
        BlockPos centre = BlockPos.containing(c);
        BlockPos self = getBlockPos();
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-radius, -radius, -radius),
                centre.offset(radius, radius, radius))) {
            double d = Math.sqrt(pos.distSqr(centre));
            if (d > radius + level.random.nextDouble() * 1.5 - 0.5) continue;
            if (pos.equals(self)) continue;
            var state = level.getBlockState(pos);
            if (state.isAir() || state.getDestroySpeed(level, pos) < 0) continue;
            level.destroyBlock(pos.immutable(), false);
        }
        level.playSound(null, centre, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0f, 0.5f);
        mass *= 0.2;
        stability = 60.0;
        feed = 0;
        draw = 0;
        breaches++;
        syncDataHolder.markClientSyncFieldDirty("breaches");
        syncDataHolder.markClientSyncFieldDirty("feed");
        syncDataHolder.markClientSyncFieldDirty("draw");
    }

    private void publish() {
        boolean push = getLevel() != null && getLevel().getGameTime() % 2 == 0;
        massX100 = (int) Math.min(Integer.MAX_VALUE, Math.round(mass * 100.0));
        stabilityX10 = Math.round((float) stability * 10.0f);
        if (push) {
            for (String name : new String[] { "massX100", "stabilityX10", "powered", "energyUse", "emitting",
                    "feeding" }) {
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
                        new S2CNullWellOpenPacket(getBlockPos()));
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(isRemote());
        }
        return super.onUse(context);
    }

    public void control(int which, int value) {
        load();
        switch (which) {
            case NullWellControls.FEED -> {
                feed = Math.max(0, Math.min(100, value));
                syncDataHolder.markClientSyncFieldDirty("feed");
            }
            case NullWellControls.DRAW -> {
                draw = Math.max(0, Math.min(100, value));
                syncDataHolder.markClientSyncFieldDirty("draw");
            }
            case NullWellControls.VENT -> {
                // an emergency dump: half the mass is lost, none of it as usable energy
                mass *= 0.5;
                feed = 0;
                syncDataHolder.markClientSyncFieldDirty("feed");
                publish();
            }
            default -> {}
        }
    }

    private static final int UI_WIDTH = 264;
    private static final int UI_HEIGHT = 244;

    @Override
    public ModularPanel<?> buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        syncManager.registerServerSyncedAction("null_well_control", buf -> {
            int which = buf.readVarInt();
            int value = buf.readVarInt();
            control(which, value);
        });
        TabStrip screen = null;
        if (getLevel() != null && getLevel().isClientSide) {
            NullWellControls controls = new NullWellControls() {

                @Override
                public void set(int which, int value) {
                    {
                        var out = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),
                                getLevel().registryAccess());
                        out.writeVarInt(which);
                        out.writeVarInt(value);
                        syncManager.callSyncedAction("null_well_control", out);
                    }
                }

                @Override
                public void press(int action) {
                    set(action, 0);
                }
            };
            screen = new TabStrip(List.of(new TabStrip.Tab("W", new NullWellHudPage(this, controls)),
                    new TabStrip.Tab("~", new NullWellHistoryPage(this))), HudTheme.ARCANE);
        }
        final TabStrip tabs = screen;
        var panel = MachineUIPanelBuilder.panelBuilder(this).showPatternErrors().attachInventory(false)
                .addTitleBar(false)
                .drawGTLogo(false)
                .mainContents(main -> {
                    if (tabs != null) main.child(new CanvasWidget(tabs).size(UI_WIDTH, UI_HEIGHT));
                    else main.size(UI_WIDTH, UI_HEIGHT);
                }).build(data, syncManager, settings);
        if (tabs != null) HudPanel.apply(panel, tabs);
        return panel;
    }
}
