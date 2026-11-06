package net.phoenix.core.integration.ars_nouveau.logistics;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.TieredMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.phoenix.core.client.gui.hud.Heroes;
import net.phoenix.core.client.gui.hud.HudModel;
import net.phoenix.core.client.gui.hud.HudTheme;
import net.phoenix.core.client.gui.hud.MachineHud;
import net.phoenix.core.integration.ars_nouveau.api.capability.ISourceProviderCapability;
import net.phoenix.core.integration.ars_nouveau.api.machine.trait.NotifiableSourceContainer;
import net.phoenix.core.utils.TeamUtils;

import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.BooleanSyncValue;
import brachy.modularui.value.sync.IntSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.hollingsworth.arsnouveau.client.particle.ParticleColor;
import com.hollingsworth.arsnouveau.client.particle.ParticleUtil;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A wireless Source relay. Set it to a channel and to sending or receiving: every sender on a team's channel gives its
 * Source to the receivers on the same channel, wherever they are. A sender takes Source from a jar or output hatch
 * beside it (or from a pipe that ends at it); a receiver hands it to whatever beside it takes Source, or lets a pipe
 * draw it out. The trip costs a little Source that grows with the distance, and more across dimensions.
 */
public class SourceRelayMachine extends TieredMachine implements ISourceProviderCapability, IMuiMachine {

    public static final int MAX_CHANNEL = 99;

    private static final float BASE_LOSS = 0.03f;
    private static final float LOSS_PER_KILOBLOCK = 0.01f;
    private static final float DIMENSION_LOSS = 0.25f;
    private static final float MAX_LOSS = 0.6f;

    @SaveField
    @SyncToClient
    private int channel = 1;

    @SaveField
    @SyncToClient
    private boolean sender = true;

    @SyncToClient
    private int linked = 0;

    private final NotifiableSourceContainer container;

    @Nullable
    private TickableSubscription tickSub;

    public SourceRelayMachine(BlockEntityCreationInfo info, int tier) {
        super(info, tier);
        this.container = attachTrait(new NotifiableSourceContainer(IO.BOTH, capacity(tier), rate(tier)));
    }

    public static int capacity(int tier) {
        return 4000 * tier;
    }

    /** Source a relay moves in one tick. */
    public static int rate(int tier) {
        return 1000 * tier;
    }

    @Override
    public ISourceTile getSource() {
        return container;
    }

    public boolean isSender() {
        return sender;
    }

    public int getChannel() {
        return channel;
    }

    // ------------------------------------------------------------------------------------------ the channel

    UUID team() {
        UUID owner = getOwnerUUID();
        UUID team = owner == null ? null : TeamUtils.getTeamIdOrPlayerFallback(owner);
        return team == null ? new UUID(0L, 0L) : team;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            Channels.join(this);
            tickSub = subscribeServerTick(tickSub, this::tick);
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        Channels.leave(this);
    }

    public void setChannel(int next) {
        int clamped = Math.max(1, Math.min(MAX_CHANNEL, next));
        if (clamped == channel) return;
        Channels.leave(this);
        channel = clamped;
        Channels.join(this);
        markDirty();
    }

    public void toggleSender() {
        sender = !sender;
        markDirty();
    }

    private void markDirty() {
        getSyncDataHolder().markClientSyncFieldDirty("channel");
        getSyncDataHolder().markClientSyncFieldDirty("sender");
        setChanged();
    }

    /** Relays on the same team and channel, found by those that are loaded. */
    static final class Channels {

        private record Key(UUID team, int channel) {}

        private static final Map<Key, Set<SourceRelayMachine>> RELAYS = new ConcurrentHashMap<>();
        private static final Map<SourceRelayMachine, Key> JOINED = new ConcurrentHashMap<>();

        static void join(SourceRelayMachine relay) {
            leave(relay);
            Key key = new Key(relay.team(), relay.channel);
            RELAYS.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(relay);
            JOINED.put(relay, key);
        }

        static void leave(SourceRelayMachine relay) {
            Key key = JOINED.remove(relay);
            if (key == null) return;
            Set<SourceRelayMachine> set = RELAYS.get(key);
            if (set != null) {
                set.remove(relay);
                if (set.isEmpty()) RELAYS.remove(key);
            }
        }

        static List<SourceRelayMachine> on(SourceRelayMachine relay) {
            Set<SourceRelayMachine> set = RELAYS.get(new Key(relay.team(), relay.channel));
            return set == null ? List.of() : new ArrayList<>(set);
        }
    }

    // ------------------------------------------------------------------------------------------ moving Source

    private void tick() {
        if (!(getLevel() instanceof ServerLevel level) || isRemote()) return;
        if (sender) {
            drawFromNeighbours(level);
            beam(level);
        } else {
            giveToNeighbours(level);
        }
    }

    /** A sender takes from whatever beside it can give. */
    private void drawFromNeighbours(ServerLevel level) {
        int room = Math.min(container.getMaxSource() - container.getSource(), rate(tier));
        if (room <= 0) return;
        for (Direction side : Direction.values()) {
            if (room <= 0) break;
            SourceEndpoint ep = SourceEndpoint.find(level, getBlockPos().relative(side));
            if (ep == null || !ep.canGive()) continue;
            int taken = ep.give(Math.min(room, ep.stored()));
            if (taken > 0) {
                container.addSource(taken);
                room -= taken;
            }
        }
    }

    /** A receiver hands its Source to whatever beside it takes Source in. */
    private void giveToNeighbours(ServerLevel level) {
        int budget = Math.min(container.getSource(), rate(tier));
        if (budget <= 0) return;
        for (Direction side : Direction.values()) {
            if (budget <= 0) break;
            SourceEndpoint ep = SourceEndpoint.find(level, getBlockPos().relative(side));
            if (ep == null || !ep.canTake()) continue;
            int put = ep.take(Math.min(budget, ep.space()));
            if (put > 0) {
                container.removeSource(put);
                budget -= put;
            }
        }
    }

    private float lossTo(SourceRelayMachine other) {
        if (other.getLevel() == null || getLevel() == null) return MAX_LOSS;
        float loss = BASE_LOSS;
        if (other.getLevel().dimension() != getLevel().dimension()) {
            loss += DIMENSION_LOSS;
        } else {
            double blocks = Math.sqrt(other.getBlockPos().distSqr(getBlockPos()));
            loss += (float) (blocks / 1000.0) * LOSS_PER_KILOBLOCK;
        }
        return Math.min(MAX_LOSS, loss);
    }

    /** Sends Source to the receivers on this channel, emptiest first. */
    private void beam(ServerLevel level) {
        List<SourceRelayMachine> receivers = new ArrayList<>();
        for (SourceRelayMachine other : Channels.on(this)) {
            if (other != this && !other.sender && !other.isRemote() && other.getLevel() != null) {
                receivers.add(other);
            }
        }
        if (receivers.size() != linked) {
            linked = receivers.size();
            getSyncDataHolder().markClientSyncFieldDirty("linked");
        }
        int budget = Math.min(container.getSource(), rate(tier));
        if (budget <= 0 || receivers.isEmpty()) return;

        receivers.sort(Comparator.comparingDouble(r -> r.container.getSource() / (double) Math.max(1,
                r.container.getMaxSource())));
        long now = level.getGameTime();
        for (SourceRelayMachine target : receivers) {
            if (budget <= 0) break;
            float loss = lossTo(target);
            int space = target.container.getMaxSource() - target.container.getSource();
            if (space <= 0) continue;
            int room = Math.min(budget, Math.min(rate(Math.min(tier, target.tier)),
                    (int) Math.ceil(space / (1f - loss))));
            if (room <= 0) continue;
            int taken = container.removeSource(room);
            if (taken <= 0) break;
            int delivered = target.container.addSource((int) Math.floor(taken * (1f - loss)));
            budget -= taken;
            if (delivered > 0 && now % 20 == 0 && target.getLevel() == level) {
                BlockPos from = getBlockPos();
                BlockPos to = target.getBlockPos();
                ParticleUtil.spawnFollowProjectile(level, from, to, ParticleColor.defaultParticleColor());
            }
        }
    }

    // ------------------------------------------------------------------------------------------ the screen

    private static final int ID_CHANNEL = 1;
    private static final int ID_MODE = 2;

    private void hudAction(int id, int value) {
        if (id == ID_CHANNEL) setChannel(value);
        if (id == ID_MODE) toggleSender();
    }

    @Override
    public brachy.modularui.screen.ModularPanel<?> buildUI(PosGuiData data, PanelSyncManager syncManager,
                                                           UISettings settings) {
        syncManager.syncValue("channel", 0, new IntSyncValue(() -> this.channel, v -> this.channel = v));
        syncManager.syncValue("sender", 0, new BooleanSyncValue(() -> this.sender, v -> this.sender = v));
        syncManager.syncValue("linked", 0, new IntSyncValue(() -> this.linked, v -> this.linked = v));
        syncManager.syncValue("source", 0, new IntSyncValue(() -> container.getSource(),
                v -> container.setSource(v)));
        return MachineHud.build(this, data, syncManager, settings, this::hudAction, this::hudModel);
    }

    private HudModel hudModel() {
        int capacity = container.getMaxSource();
        return HudModel.of(com.gregtechceu.gtceu.api.GTValues.VNF[tier] + " Source Relay")
                .theme(HudTheme.DATA)
                .formed(() -> true)
                .hero(Heroes.arcGauge(() -> container.getSource() / (float) Math.max(1, capacity), 0xFFB070FF), 38)
                .badge(() -> sender ? new HudModel.Badge("SENDING", HudModel.PURPLE) :
                        new HudModel.Badge("RECEIVING", HudModel.CYAN))
                .subtitle(() -> "channel " + channel + (sender ? ", " + linked + " receiver(s) linked" : ""))
                .cells(() -> {
                    var cells = new ArrayList<HudModel.Cell>();
                    cells.add(HudModel.cell("CHANNEL", String.valueOf(channel), "of " + MAX_CHANNEL, HudModel.GOLD));
                    cells.add(HudModel.cell("SOURCE", String.format(Locale.ROOT, "%,d", container.getSource()),
                            "of " + String.format(Locale.ROOT, "%,d", capacity), HudModel.PURPLE));
                    cells.add(HudModel.cell("RATE", String.format(Locale.ROOT, "%,d /t", rate(tier)), "per tick",
                            HudModel.CYAN));
                    cells.add(HudModel.cell("LINKED", sender ? String.valueOf(linked) : "-", sender ?
                            "receivers on this channel" : "senders send to you", HudModel.GREEN));
                    return cells;
                })
                .bars(() -> List.of(new HudModel.Bar("Source", container.getSource() / (float) Math.max(1, capacity),
                        String.format(Locale.ROOT, "%,d / %,d", container.getSource(), capacity), HudModel.PURPLE)))
                .slider("CHANNEL", ID_CHANNEL, () -> channel, 1, MAX_CHANNEL, null, "")
                .button(() -> sender ? "MODE: SENDING (click to receive)" : "MODE: RECEIVING (click to send)",
                        ID_MODE, HudModel.PURPLE, () -> true)
                .notes(() -> List.of(HudModel.line(String.format(Locale.ROOT,
                        "Costs %.0f%% plus %.0f%% per 1,000 blocks, and %.0f%% more across dimensions.",
                        BASE_LOSS * 100, LOSS_PER_KILOBLOCK * 100, DIMENSION_LOSS * 100), HudModel.MID)));
    }
}
