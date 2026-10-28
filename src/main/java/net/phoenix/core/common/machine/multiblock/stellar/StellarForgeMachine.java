package net.phoenix.core.common.machine.multiblock.stellar;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.machine.multiblock.part.EnergyHatchPartMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;

import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.phoenix.core.client.stellar.StellarForgeClient;

import com.gregtechceu.gtceu.api.machine.mui.MachineUIPanelBuilder;

import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.PanelSyncManager;
import net.phoenix.core.client.gui.hud.CanvasWidget;
import net.phoenix.core.client.gui.hud.HudPanel;
import net.phoenix.core.client.gui.hud.TabStrip;
import net.phoenix.core.client.stellar.ui.StellarGraphsPage;
import net.phoenix.core.client.stellar.ui.StellarHudPage;
import net.phoenix.core.client.stellar.ui.StellarMathPage;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Stellar Forge: a chamber that gathers a cloud of gas, lights it as a star, and, fed something heavy enough,
 * lets it fall in as a black hole, all of it shown inside the machine while it is made. The rules are in
 * {@link StellarForgeState}; this connects them to hatches (energy and matter in, plasma out), keeps them over restarts
 * and tells clients what to draw.
 */
public class StellarForgeMachine extends MultiblockControllerMachine implements IMuiMachine {

    /** Mass in M that a millibucket of each feed adds. */
    private static double massPerMb(FluidStack stack) {
        Fluid fluid = stack.getFluid();
        if (isFluid(fluid, GTMaterials.Neutronium.getFluid())) return 1.0;
        if (isFluid(fluid, GTMaterials.Helium.getFluid())) return 0.02;
        if (isFluid(fluid, GTMaterials.Hydrogen.getFluid())) return 0.01;
        return 0.0;
    }

    private static boolean isFluid(Fluid a, @Nullable Fluid b) {
        return b != null && a.isSame(b);
    }

    @SaveField
    @SyncToClient
    private int stageOrdinal;

    /** Mass times a hundred, to keep it in an int. */
    @SaveField
    @SyncToClient
    private int massX100;

    @SaveField
    @SyncToClient
    private int heatX10;

    @SaveField
    @SyncToClient
    private int stabilityX10;

    @SaveField
    @SyncToClient
    private int feed;

    @SaveField
    @SyncToClient
    private int field;

    @SaveField
    @SyncToClient
    private int spin;

    @SaveField
    @SyncToClient
    private boolean collapseArmed;

    @SyncToClient
    private boolean powered = true;

    @SyncToClient
    private int energyUse;

    @SyncToClient
    private int plasmaRateX10;

    private @Nullable StellarForgeState state;
    private double feedBuffer;
    private double plasmaBuffer;
    private final ConditionalSubscriptionHandler tickHandler;

    public StellarForgeMachine(BlockEntityCreationInfo info) {
        super(info);
        this.tickHandler = new ConditionalSubscriptionHandler(this, this::forgeTick, this::isFormed);
    }

    // ------------------------------------------------------------------------------------------------ what is seen

    public StellarForgeState.Stage stage() {
        return StellarForgeState.Stage.of(stageOrdinal);
    }

    public double mass() {
        return massX100 / 100.0;
    }

    public float heat() {
        return heatX10 / 10.0f;
    }

    public float stability() {
        return stabilityX10 / 10.0f;
    }

    public int feedSetting() {
        return feed;
    }

    public int fieldSetting() {
        return field;
    }

    public int spinSetting() {
        return spin;
    }

    public boolean armed() {
        return collapseArmed;
    }

    public boolean powerOk() {
        return powered;
    }

    public int energyUse() {
        return energyUse;
    }

    public float plasmaPerTick() {
        return plasmaRateX10 / 10.0f;
    }

    /** The chamber's middle, in the world: the room inside is the middle of the structure. */
    public net.minecraft.world.phys.Vec3 chamberCentre() {
        Direction back = getFrontFacing().getOpposite();
        return new net.minecraft.world.phys.Vec3(getBlockPos().getX() + 0.5 + back.getStepX() * 2.0,
                getBlockPos().getY() + 2.5, getBlockPos().getZ() + 0.5 + back.getStepZ() * 2.0);
    }

    public double massFraction() {
        return view().massFraction();
    }

    /** A copy of the numbers as they are now, for the bands the screen draws. */
    public StellarForgeState view() {
        StellarForgeState view = new StellarForgeState(getBlockPos().asLong());
        view.stage = stage();
        view.mass = mass();
        view.heat = heat();
        view.stability = stability();
        view.feed = feed;
        view.field = field;
        view.spin = spin;
        return view;
    }

    // ------------------------------------------------------------------------------------------------ the controls

    private StellarForgeState state() {
        if (state == null) {
            state = new StellarForgeState(getBlockPos().asLong());
            state.stage = StellarForgeState.Stage.of(stageOrdinal);
            state.mass = massX100 / 100.0;
            state.heat = heatX10 / 10.0f;
            state.stability = stabilityX10 / 10.0f;
            state.feed = feed;
            state.field = field;
            state.spin = spin;
            state.collapseArmed = collapseArmed;
        }
        return state;
    }

    public void setFeed(int value) {
        feed = state().feed = Math.max(0, Math.min(100, value));
    }

    public void setField(int value) {
        field = state().field = Math.max(0, Math.min(100, value));
    }

    public void setSpin(int value) {
        spin = state().spin = Math.max(0, Math.min(100, value));
    }

    public void armCollapse() {
        StellarForgeState s = state();
        if (s.stage == StellarForgeState.Stage.STAR) {
            s.collapseArmed = !s.collapseArmed;
            collapseArmed = s.collapseArmed;
        }
    }

    public void disperse() {
        state().disperse();
        publish();
    }

    // ------------------------------------------------------------------------------------------------ the ticking

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() != null && getLevel().isClientSide) StellarForgeClient.add(this);
        tickHandler.initialize(getLevel());
    }

    @Override
    public void onUnload() {
        if (getLevel() != null && getLevel().isClientSide) StellarForgeClient.remove(this);
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

    private List<FluidHatchPartMachine> hatches(IO io) {
        List<FluidHatchPartMachine> found = new ArrayList<>();
        for (var part : getParts()) {
            if (part instanceof FluidHatchPartMachine hatch && hatch.tank.getHandlerIO() == io) found.add(hatch);
        }
        return found;
    }

    private long energyAvailable() {
        long total = 0L;
        for (var part : getParts()) {
            if (part instanceof EnergyHatchPartMachine hatch && hatch.energyContainer.getHandlerIO() == IO.IN) {
                total += hatch.energyContainer.getEnergyStored();
            }
        }
        return total;
    }

    private void takeEnergy(long amount) {
        long left = amount;
        for (var part : getParts()) {
            if (left <= 0) break;
            if (part instanceof EnergyHatchPartMachine hatch && hatch.energyContainer.getHandlerIO() == IO.IN) {
                long take = Math.min(left, hatch.energyContainer.getEnergyStored());
                if (take > 0) {
                    hatch.energyContainer.changeEnergy(-take);
                    left -= take;
                }
            }
        }
    }

    private void forgeTick() {
        if (getLevel() == null || getLevel().isClientSide) return;
        StellarForgeState s = state();

        // energy first: without it the field slips
        long need = s.energyPerTick();
        boolean ok = need <= 0 || energyAvailable() >= need;
        if (ok && need > 0) takeEnergy(need);

        // then what is fed in
        double fed = 0.0;
        double want = s.stage == StellarForgeState.Stage.IDLE ?
                StellarForgeState.MAX_FEED_PER_TICK[1] * feed / 100.0 : s.feedPerTick();
        if (!ok && s.stage != StellarForgeState.Stage.IDLE) want = 0.0;
        feedBuffer += want;
        int whole = (int) feedBuffer;
        if (whole > 0) {
            for (FluidHatchPartMachine hatch : hatches(IO.IN)) {
                if (whole <= 0) break;
                for (int tank = 0; tank < hatch.tank.getTanks() && whole > 0; tank++) {
                    FluidStack inside = hatch.tank.getFluidInTank(tank);
                    double perMb = inside.isEmpty() ? 0.0 : massPerMb(inside);
                    if (perMb <= 0.0) continue;
                    FluidStack taken = hatch.tank.drainInternal(new FluidStack(inside.getFluid(), Math.min(whole,
                            inside.getAmount())), IFluidHandler.FluidAction.EXECUTE);
                    if (!taken.isEmpty()) {
                        fed += taken.getAmount() * perMb;
                        whole -= taken.getAmount();
                        feedBuffer -= taken.getAmount();
                    }
                }
            }
            // nothing in the hatches to take: the buffer does not pile up
            feedBuffer = Math.min(feedBuffer, 2.0);
        }

        // plasma out, and a full hatch backs the pressure up
        boolean blocked = false;
        double rate = s.plasmaPerTick();
        if (rate > 0.0) {
            plasmaBuffer += rate;
            int out = (int) plasmaBuffer;
            if (out > 0) {
                Fluid plasma = GTMaterials.Oxygen.getFluid(FluidStorageKeys.PLASMA);
                int left = out;
                if (plasma != null) {
                    for (FluidHatchPartMachine hatch : hatches(IO.OUT)) {
                        if (left <= 0) break;
                        left -= hatch.tank.fillInternal(new FluidStack(plasma, left), IFluidHandler.FluidAction.EXECUTE);
                    }
                }
                plasmaBuffer -= out - left;
                blocked = left > 0;
                plasmaBuffer = Math.min(plasmaBuffer, 64.0);
            }
        }

        s.tick(fed, ok, blocked);
        powered = ok;
        energyUse = (int) Math.min(Integer.MAX_VALUE, need);
        plasmaRateX10 = Math.round((float) rate * 10.0f);
        publish();
    }

    /** Copy the numbers to the fields that are saved and sent to clients (only changed ones cause a sync). */
    private void publish() {
        StellarForgeState s = state();
        stageOrdinal = s.stage.ordinal();
        massX100 = (int) Math.min(Integer.MAX_VALUE, Math.round(s.mass * 100.0));
        heatX10 = Math.round(s.heat * 10.0f);
        stabilityX10 = Math.round(s.stability * 10.0f);
        feed = s.feed;
        field = s.field;
        spin = s.spin;
        collapseArmed = s.collapseArmed;
    }

    // ------------------------------------------------------------------------------------------------ the screen

    private static final int UI_WIDTH = 264;
    private static final int UI_HEIGHT = 230;

    /** A setting or button pressed on the screen, done here on the server. */
    private void control(int which, int value) {
        switch (which) {
            case StellarControls.FEED -> setFeed(value);
            case StellarControls.FIELD -> setField(value);
            case StellarControls.SPIN -> setSpin(value);
            case StellarControls.ARM -> armCollapse();
            case StellarControls.DISPERSE -> disperse();
            default -> {}
        }
    }

    @Override
    public ModularPanel<?> buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        syncManager.registerServerSyncedAction("stellar_control", buf -> {
            int which = buf.readVarInt();
            int value = buf.readVarInt();
            control(which, value);
        });
        TabStrip screen = null;
        if (getLevel() != null && getLevel().isClientSide) {
            StellarControls controls = new StellarControls() {

                @Override
                public void set(int which, int value) {
                    syncManager.callSyncedAction("stellar_control", buf -> {
                        buf.writeVarInt(which);
                        buf.writeVarInt(value);
                    });
                }

                @Override
                public void press(int action) {
                    set(action, 0);
                }
            };
            screen = new TabStrip(List.of(new TabStrip.Tab("H", new StellarHudPage(this, controls)),
                    new TabStrip.Tab("V", new net.phoenix.core.client.stellar.ui.StellarViewPage(this)),
                    new TabStrip.Tab("M", new StellarMathPage(this)),
                    new TabStrip.Tab("~", new StellarGraphsPage(this))),
                    net.phoenix.core.client.gui.hud.HudTheme.DEEP_SPACE);
        }
        final TabStrip tabs = screen;
        var panel = MachineUIPanelBuilder.panelBuilder(this).attachInventory(false).addTitleBar(false)
                .drawGTLogo(false)
                .mainContents(main -> {
                    if (tabs != null) main.child(new CanvasWidget(tabs).size(UI_WIDTH, UI_HEIGHT));
                    else main.size(UI_WIDTH, UI_HEIGHT);
                }).build(syncManager, settings);
        if (tabs != null) HudPanel.apply(panel, tabs);
        return panel;
    }
}
