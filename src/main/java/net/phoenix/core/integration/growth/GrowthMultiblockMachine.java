package net.phoenix.core.integration.growth;

import net.phoenix.core.client.gui.hud.MachineHud;
import net.phoenix.core.client.gui.hud.HudRecipes;
import net.phoenix.core.client.gui.hud.HudModel;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.sync_system.annotations.ClientFieldChangeListener;
import com.gregtechceu.gtceu.api.sync_system.annotations.RerenderOnChanged;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.phoenix.core.api.gui.widget.ActionButtons;
import net.phoenix.core.api.gui.widget.StageTrackerWidget;
import net.phoenix.core.client.worldfx.IWorldFXEmitter;
import net.phoenix.core.client.worldfx.PhoenixScreenEffect;
import net.phoenix.core.client.worldfx.PhoenixSkyLayer;
import net.phoenix.core.client.worldfx.WorldFXManager;
import net.phoenix.core.client.worldfx.builtin.NebulaSkyLayer;
import net.phoenix.core.common.machine.multiblock.unique.UniqueWorkableElectricMultiblockMachine;
import net.phoenix.core.integration.growth.tendril.GrowthWardBlock;
import net.phoenix.core.integration.growth.tendril.TendrilShape;
import net.phoenix.core.integration.growth.tendril.TendrilShapeRegistry;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.drawable.ItemDrawable;
import brachy.modularui.screen.RichTooltip;
import brachy.modularui.value.sync.PanelSyncManager;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class GrowthMultiblockMachine extends UniqueWorkableElectricMultiblockMachine
                                              implements IWorldFXEmitter {

    public enum GrowthResult {
        SUCCESS,
        PROGRESSED,
        MAX_STAGE,
        OBSTRUCTED,
        INSUFFICIENT_COST,

        PAUSED
    }

    @Getter
    @SaveField
    @SyncToClient
    @RerenderOnChanged
    protected int growthStage = 0;

    @Getter
    @SaveField
    @SyncToClient
    protected float growthProgress = 0f;

    @Getter
    @SaveField
    @SyncToClient
    protected int maturedTendrils = 0;

    public static final int STALL_NONE = 0;
    public static final int STALL_BLOCKED = 1;
    public static final int STALL_COST = 2;

    @Getter
    @SaveField
    @SyncToClient
    protected int growthStall = STALL_NONE;

    @Getter
    @SaveField
    @SyncToClient
    protected boolean growthPaused = false;

    @Getter
    @SaveField
    @SyncToClient
    protected int targetStage = 99;

    @Getter
    @SaveField
    @SyncToClient
    protected boolean tendrilsEnabled = true;

    @Getter
    @SaveField
    @SyncToClient
    protected int anchorMask = 0xFFFF;

    @Getter
    @SaveField
    @SyncToClient
    protected int tendrilBlockCount = 0;

    public static final String MIN_STAGE_KEY = "min_growth_stage";

    protected GrowthMultiblockMachine(BlockEntityCreationInfo holder) {
        super(holder);
    }

    protected abstract List<GrowthStage> getGrowthStages();

    public GrowthStage currentStage() {
        List<GrowthStage> stages = getGrowthStages();
        return stages.get(Math.max(0, Math.min(growthStage, stages.size() - 1)));
    }

    public static ModifierFunction stageModifier(MetaMachine machine, GTRecipe recipe) {
        if (!(machine instanceof GrowthMultiblockMachine growing)) return ModifierFunction.IDENTITY;
        GrowthStage stage = growing.currentStage();
        float speed = growing.totalSpeedBonus();
        float extra = growing.totalOutputBonus();
        if (speed <= 0.0f && extra <= 0.0f && stage.batch() <= 1) return ModifierFunction.IDENTITY;

        var builder = ModifierFunction.builder();
        if (speed > 0.0f) builder.durationMultiplier(Math.max(0.1, 1.0 - speed));
        double batch = Math.max(1, stage.batch());
        double output = batch * (1.0 + Math.max(0.0f, extra));
        if (batch > 1) {
            builder.inputModifier(ContentModifier.multiplier(batch)).eutMultiplier(batch);
        }
        if (output != 1.0) builder.outputModifier(ContentModifier.multiplier(output));
        return builder.build();
    }

    public float totalSpeedBonus() {
        return Math.min(0.75f, currentStage().speedBonus() + tendrilSpeedBonus());
    }

    public float totalOutputBonus() {
        return Math.max(0.0f, currentStage().outputBonus() + tendrilOutputBonus());
    }

    public float tendrilSpeedBonus() {
        return Math.min(getMaxTendrilSpeedBonus(), tendrilBlockCount * getTendrilSpeedPerBlock());
    }

    public float tendrilOutputBonus() {
        return Math.min(getMaxTendrilOutputBonus(), tendrilBlockCount * getTendrilOutputPerBlock());
    }

    protected float getTendrilSpeedPerBlock() {
        return 0.004f;
    }

    protected float getMaxTendrilSpeedBonus() {
        return 0.25f;
    }

    protected float getTendrilOutputPerBlock() {
        return 0.005f;
    }

    protected float getMaxTendrilOutputBonus() {
        return 0.30f;
    }

    protected boolean overridesBlocks() {
        return true;
    }

    public boolean isControlled() {
        for (var part : getParts()) {
            if (part instanceof GrowthControlHatch) return true;
        }
        return false;
    }

    private boolean atCap(int next, List<GrowthStage> stages) {
        return next >= stages.size() || (isControlled() && next > targetStage);
    }

    private boolean blocksGrowth(ServerLevel level, BlockPos pos, BlockState existing) {
        if (isWarded(level, pos)) return true;
        if (existing.isAir() || existing.canBeReplaced()) return false;
        return isControlled() || !overridesBlocks() || existing.getDestroySpeed(level, pos) < 0;
    }

    public void togglePaused() {
        if (!isControlled()) return;
        growthPaused = !growthPaused;
        getSyncDataHolder().markClientSyncFieldDirty("growthPaused");
        setChanged();
    }

    public void adjustTargetStage(int delta) {
        if (!isControlled()) return;
        targetStage = Math.max(0, Math.min(maxStage(), Math.min(targetStage, maxStage()) + delta));
        getSyncDataHolder().markClientSyncFieldDirty("targetStage");
        setChanged();
    }

    public void toggleTendrils() {
        if (!isControlled()) return;
        tendrilsEnabled = !tendrilsEnabled;
        getSyncDataHolder().markClientSyncFieldDirty("tendrilsEnabled");
        setChanged();
    }

    public void toggleAnchor(int index) {
        if (!isControlled()) return;
        anchorMask ^= 1 << index;
        getSyncDataHolder().markClientSyncFieldDirty("anchorMask");
        setChanged();
    }

    @Override
    public boolean beforeWorking(@Nullable GTRecipe recipe) {
        if (recipe != null && recipe.data.contains(MIN_STAGE_KEY) &&
                growthStage < recipe.data.getInt(MIN_STAGE_KEY)) {
            return false;
        }
        return super.beforeWorking(recipe);
    }

    protected abstract BlockState getShellBlockState();

    protected boolean chargeOnAutoGrowth() {
        return false;
    }

    public int maxStage() {
        return Math.max(0, getGrowthStages().size() - 1);
    }

    public int growingTendrils() {
        return activeTendrils.size();
    }

    private void setStall(int stall) {
        if (growthStall == stall) return;
        growthStall = stall;
        getSyncDataHolder().markClientSyncFieldDirty("growthStall");
        setChanged();
    }

    @Override
    public void afterWorking() {
        super.afterWorking();
        if (!getLevel().isClientSide) {
            tryGrow();
            tryGrowTendril();
        }
    }

    protected float getGrowthPerRecipe() {
        return 1.0f;
    }

    public GrowthResult tryGrow() {
        if (isControlled() && (growthPaused || growthStage >= targetStage)) return GrowthResult.PAUSED;
        return addGrowthProgress(getGrowthPerRecipe(), false);
    }

    private GrowthResult addGrowthProgress(float amount, boolean paid) {
        List<GrowthStage> stages = getGrowthStages();
        int next = growthStage + 1;
        if (atCap(next, stages)) return GrowthResult.MAX_STAGE;
        if (amount <= 0f) return GrowthResult.PROGRESSED;

        growthProgress += amount;
        if (growthProgress < 1f) {
            getSyncDataHolder().markClientSyncFieldDirty("growthProgress");
            setChanged();
            return GrowthResult.PROGRESSED;
        }

        growthProgress -= 1f;

        if (!paid && chargeOnAutoGrowth() && getLevel() instanceof ServerLevel serverLevel) {
            BlockPos obstruction = firstObstruction(serverLevel, stages, next);
            if (obstruction != null) {
                onGrowthObstructed(obstruction);
                growthProgress = Math.min(growthProgress + 1f, 0.999f);
                setStall(STALL_BLOCKED);
                getSyncDataHolder().markClientSyncFieldDirty("growthProgress");
                setChanged();
                return GrowthResult.OBSTRUCTED;
            }
            List<ItemStack> cost = stages.get(next).cost();
            if (!cost.isEmpty() && !consumeCost(cost)) {
                growthProgress = Math.min(growthProgress + 1f, 0.999f);
                setStall(STALL_COST);
                getSyncDataHolder().markClientSyncFieldDirty("growthProgress");
                setChanged();
                return GrowthResult.INSUFFICIENT_COST;
            }
        }

        GrowthResult result = advanceStage();
        setStall(result == GrowthResult.SUCCESS ? STALL_NONE :
                result == GrowthResult.OBSTRUCTED ? STALL_BLOCKED : growthStall);
        if (result != GrowthResult.SUCCESS) {

            growthProgress = Math.min(growthProgress + 1f, 0.999f);
        }
        getSyncDataHolder().markClientSyncFieldDirty("growthProgress");
        setChanged();
        return result;
    }

    public GrowthResult tryManualGrow() {
        return tryManualGrow(1.0f);
    }

    public GrowthResult tryManualGrow(float amount) {
        if (!(getLevel() instanceof ServerLevel)) return GrowthResult.MAX_STAGE;
        if (amount <= 0f) return GrowthResult.PROGRESSED;

        List<GrowthStage> stages = getGrowthStages();
        int next = growthStage + 1;
        if (atCap(next, stages)) return GrowthResult.MAX_STAGE;

        List<ItemStack> cost = scaleCost(stages.get(next).cost(), amount);
        if (!cost.isEmpty() && !consumeCost(cost)) return GrowthResult.INSUFFICIENT_COST;

        return addGrowthProgress(amount, true);
    }

    private List<ItemStack> scaleCost(List<ItemStack> cost, float amount) {
        if (amount >= 1f || cost.isEmpty()) return cost;
        List<ItemStack> scaled = new ArrayList<>(cost.size());
        for (ItemStack required : cost) {
            ItemStack copy = required.copy();
            copy.setCount(Math.max(1, (int) Math.ceil(required.getCount() * amount)));
            scaled.add(copy);
        }
        return scaled;
    }

    private @Nullable BlockPos firstObstruction(ServerLevel level, List<GrowthStage> stages, int next) {
        IntList from = stages.get(growthStage).bounds();
        IntList to = stages.get(next).bounds();
        for (BlockPos pos : GrowthPatternHelper.diffShellPositions(getBlockPos(), getFrontFacing(), getUpwardsFacing(),
                from, to)) {
            if (blocksGrowth(level, pos, level.getBlockState(pos))) return pos;
        }
        return null;
    }

    private void growthEffects(ServerLevel level, List<BlockPos> positions) {
        if (positions.isEmpty()) positions = List.of(getBlockPos().above());
        RandomSource random = level.getRandom();
        for (int i = 0; i < Math.min(24, positions.size()); i++) {
            BlockPos pos = positions.get(random.nextInt(positions.size()));
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3,
                    0.4, 0.4, 0.4, 0.02);
        }
        level.playSound(null, getBlockPos(), SoundEvents.MOSS_PLACE, SoundSource.BLOCKS, 1.0f, 0.8f);
        level.playSound(null, getBlockPos(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.7f, 1.4f);
    }

    private GrowthResult advanceStage() {
        List<GrowthStage> stages = getGrowthStages();
        int next = growthStage + 1;
        if (atCap(next, stages)) return GrowthResult.MAX_STAGE;
        if (!(getLevel() instanceof ServerLevel level)) return GrowthResult.MAX_STAGE;

        IntList from = stages.get(growthStage).bounds();
        IntList to = stages.get(next).bounds();

        List<BlockPos> newPositions = GrowthPatternHelper.diffShellPositions(
                getBlockPos(), getFrontFacing(), getUpwardsFacing(), from, to);

        for (BlockPos pos : newPositions) {
            if (blocksGrowth(level, pos, level.getBlockState(pos))) {
                onGrowthObstructed(pos);
                return GrowthResult.OBSTRUCTED;
            }
        }

        BlockState shell = getShellBlockState();
        for (BlockPos pos : newPositions) {
            BlockState existing = level.getBlockState(pos);
            if (!existing.isAir() && !existing.canBeReplaced()) level.destroyBlock(pos, true);
            level.setBlock(pos, shell, 3);
        }
        growthEffects(level, newPositions);

        growthStage = next;
        getSyncDataHolder().markClientSyncFieldDirty("growthStage");
        setChanged();

        invalidateStructureCaches();
        checkAndFormStructure();
        return GrowthResult.SUCCESS;
    }

    private boolean consumeCost(List<ItemStack> cost) {
        IItemHandlerModifiable[] handlers = getCapabilitiesFlat(IO.IN, ItemRecipeCapability.CAP).stream()
                .filter(IItemHandlerModifiable.class::isInstance)
                .map(IItemHandlerModifiable.class::cast)
                .toArray(IItemHandlerModifiable[]::new);
        CombinedInvWrapper inv = new CombinedInvWrapper(handlers);

        for (ItemStack required : cost) {
            if (countAvailable(inv, required) < required.getCount()) return false;
        }
        for (ItemStack required : cost) {
            extractExact(inv, required);
        }
        return true;
    }

    private int countAvailable(CombinedInvWrapper inv, ItemStack required) {
        int found = 0;
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack slot = inv.getStackInSlot(i);
            if (ItemStack.isSameItemSameComponents(slot, required)) found += slot.getCount();
        }
        return found;
    }

    private void extractExact(CombinedInvWrapper inv, ItemStack required) {
        int remaining = required.getCount();
        for (int i = 0; i < inv.getSlots() && remaining > 0; i++) {
            ItemStack slot = inv.getStackInSlot(i);
            if (ItemStack.isSameItemSameComponents(slot, required)) {
                int extracted = inv.extractItem(i, remaining, false).getCount();
                remaining -= extracted;
            }
        }
    }

    protected void onGrowthObstructed(BlockPos pos) {}

    private static final class TendrilInstance {

        final BlockPos anchor;
        final TendrilShape shape;
        int progress;
        boolean blocked;

        TendrilInstance(BlockPos anchor, TendrilShape shape) {
            this.anchor = anchor;
            this.shape = shape;
        }
    }

    private final List<TendrilInstance> activeTendrils = new ArrayList<>();

    @SaveField
    private final INBTSerializable<Tag> tendrilStore = new TendrilStore();

    private final class TendrilStore implements INBTSerializable<Tag> {

        @Override
        public Tag serializeNBT(net.minecraft.core.HolderLookup.Provider provider) {
            ListTag list = new ListTag();
            for (TendrilInstance tendril : activeTendrils) {
                CompoundTag tag = new CompoundTag();
                tag.putLong("Anchor", tendril.anchor.asLong());
                tag.putString("Shape", tendril.shape.id().toString());
                tag.putInt("Progress", tendril.progress);
                tag.putBoolean("Blocked", tendril.blocked);
                list.add(tag);
            }
            CompoundTag whole = new CompoundTag();
            whole.put("Active", list);
            whole.put("Placed", new LongArrayTag(placedTendrils.toLongArray()));
            return whole;
        }

        @Override
        public void deserializeNBT(net.minecraft.core.HolderLookup.Provider provider, Tag nbt) {
            activeTendrils.clear();
            placedTendrils.clear();
            ListTag list = new ListTag();
            if (nbt instanceof CompoundTag whole) {
                list = whole.getList("Active", Tag.TAG_COMPOUND);
                for (long packed : whole.getLongArray("Placed")) placedTendrils.add(packed);
            } else if (nbt instanceof ListTag old) {
                list = old;
            }
            tendrilBlockCount = placedTendrils.size();
            for (Tag entry : list) {
                if (!(entry instanceof CompoundTag tag)) continue;
                TendrilShape shape = TendrilShapeRegistry.get(ResourceLocation.parse(tag.getString("Shape")));
                if (shape == null) continue;
                TendrilInstance tendril = new TendrilInstance(BlockPos.of(tag.getLong("Anchor")), shape);
                tendril.progress = tag.getInt("Progress");
                tendril.blocked = tag.getBoolean("Blocked");
                activeTendrils.add(tendril);
            }
        }
    }

    private final LongOpenHashSet placedTendrils = new LongOpenHashSet();

    private static final int MAX_TRACKED_TENDRIL_BLOCKS = 4096;

    private void validateTendrilBlocks(ServerLevel level) {
        if (placedTendrils.isEmpty()) return;
        long[] sample = placedTendrils.toLongArray();
        RandomSource random = level.getRandom();
        boolean changed = false;
        for (int i = 0; i < Math.min(12, sample.length); i++) {
            long packed = sample[random.nextInt(sample.length)];
            BlockPos pos = BlockPos.of(packed);
            if (level.isLoaded(pos) && level.getBlockState(pos).getBlock() != getTendrilBlockState().getBlock()) {
                changed |= placedTendrils.remove(packed);
            }
        }
        if (changed) syncTendrilBlocks();
    }

    private void syncTendrilBlocks() {
        tendrilBlockCount = placedTendrils.size();
        getSyncDataHolder().markClientSyncFieldDirty("tendrilBlockCount");
        setChanged();
    }

    protected int getMaxMaturedTendrils() {
        return 24;
    }

    protected List<BlockPos> getTendrilAnchors() {
        return List.of();
    }

    protected List<BlockPos> roofAnchors() {
        IntList b = currentStage().bounds();
        int up = b.getInt(0);
        int left = b.getInt(2);
        int right = b.getInt(3);
        int back = b.getInt(5);
        int near = Math.min(1, back);
        int far = Math.max(back - 1, near);
        int sideLeft = -Math.max(left - 1, 0);
        int sideRight = Math.max(right - 1, 0);
        return List.of(
                new BlockPos(near, up + 1, sideLeft), new BlockPos(near, up + 1, sideRight),
                new BlockPos(far, up + 1, sideLeft), new BlockPos(far, up + 1, sideRight));
    }

    protected int getMaxTendrils() {
        return 3;
    }

    protected int getMaxTendrilLength() {
        return 12;
    }

    protected BlockState getTendrilBlockState() {
        return getShellBlockState();
    }

    public void tryGrowTendril() {
        if (!(getLevel() instanceof ServerLevel level)) return;
        validateTendrilBlocks(level);
        if (isControlled() && !tendrilsEnabled) return;
        List<BlockPos> anchors = getTendrilAnchors();
        if (anchors.isEmpty()) return;

        int before = activeTendrils.size();
        activeTendrils.removeIf(t -> t.blocked || t.progress >= t.shape.steps().size());
        if (activeTendrils.size() != before) {
            maturedTendrils += before - activeTendrils.size();
            getSyncDataHolder().markClientSyncFieldDirty("maturedTendrils");
            setChanged();
        }

        for (TendrilInstance tendril : activeTendrils) {
            extendTendril(level, tendril);
            setChanged();
            return;
        }

        if (activeTendrils.size() >= getMaxTendrils() || maturedTendrils >= getMaxMaturedTendrils()) return;

        RandomSource random = level.getRandom();
        TendrilShape shape = TendrilShapeRegistry.pickWeighted(this, random);
        if (shape == null) return;

        List<Integer> usable = new ArrayList<>();
        for (int i = 0; i < anchors.size(); i++) {
            if (!isControlled() || (anchorMask & (1 << i)) != 0) usable.add(i);
        }
        if (usable.isEmpty()) return;
        BlockPos anchorOffset = anchors.get(usable.get(random.nextInt(usable.size())));
        BlockPos anchor = GrowthPatternHelper.rotateToWorld(anchorOffset, getFrontFacing(), getUpwardsFacing())
                .offset(getBlockPos());
        activeTendrils.add(new TendrilInstance(anchor, shape));
        setChanged();
    }

    private void extendTendril(ServerLevel level, TendrilInstance tendril) {
        if (tendril.progress >= tendril.shape.steps().size() || tendril.progress >= getMaxTendrilLength()) {
            tendril.blocked = true;
            return;
        }

        BlockPos localStep = tendril.shape.steps().get(tendril.progress);
        BlockPos target = GrowthPatternHelper.rotateToWorld(localStep, getFrontFacing(), getUpwardsFacing())
                .offset(tendril.anchor);

        if (isWarded(level, target)) {
            tendril.blocked = true;
            return;
        }

        BlockState existing = level.getBlockState(target);
        if (!existing.isAir() && !existing.canBeReplaced()) {
            if (isControlled() || !overridesBlocks() || existing.getDestroySpeed(level, target) < 0) {
                tendril.blocked = true;
                return;
            }
            level.destroyBlock(target, true);
        }

        level.setBlock(target, getTendrilBlockState(), 3);
        if (placedTendrils.size() < MAX_TRACKED_TENDRIL_BLOCKS) {
            placedTendrils.add(target.asLong());
            syncTendrilBlocks();
        }
        level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, target.getX() + 0.5, target.getY() + 0.5,
                target.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.01);
        level.playSound(null, target, SoundEvents.MOSS_PLACE, SoundSource.BLOCKS, 0.4f, 1.2f);
        tendril.progress++;
        setChanged();
    }

    private boolean isWarded(ServerLevel level, BlockPos pos) {
        int r = GrowthWardBlock.PROTECTION_RADIUS;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-r, -r, -r), pos.offset(r, r, r))) {
            if (level.getBlockState(p).getBlock() instanceof GrowthWardBlock) return true;
        }
        return false;
    }

    @ClientFieldChangeListener(fieldName = "isFormed")
    protected void onFormedChangedClient() {
        if (isFormed()) {
            WorldFXManager.register(getBlockPos(), this);
        } else {
            WorldFXManager.unregister(getBlockPos());
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (getLevel() != null && getLevel().isClientSide) {
            WorldFXManager.unregister(getBlockPos());
        }
    }

    @Override
    public @Nullable PhoenixSkyLayer createSkyLayer() {
        float t = getGrowthStages().isEmpty() ? 0f : growthStage / (float) Math.max(1, getGrowthStages().size() - 1);

        return new NebulaSkyLayer(
                new float[] { 0.30f + 0.05f * t, 0.55f + 0.40f * t, 0.35f + 0.25f * t },
                new float[] { 0.20f + 0.65f * t, 0.35f, 0.30f + 0.65f * t },
                0.35f + 0.35f * t,
                1.0f + 0.5f * t,
                getBlockPos().asLong() % 1000);
    }

    @Override
    public @Nullable PhoenixScreenEffect createScreenEffect() {
        return null;
    }

    @Override
    public float getEffectRadius() {
        return 32f + growthStage * 8f;
    }

    private static final int ID_PAUSE = 1;
    private static final int ID_TENDRILS = 4;
    private static final int ID_GROW = 5;
    private static final int ID_TARGET = 20;
    private static final int ID_ANCHOR = 30;

    /** A screen control pressed on the server. */
    private void hudAction(int id, int value) {
        if (id == ID_PAUSE) togglePaused();
        else if (id == ID_TENDRILS) toggleTendrils();
        else if (id == ID_GROW) tryManualGrow();
        else if (id == ID_TARGET && isControlled()) {
            targetStage = Math.max(0, Math.min(maxStage(), value));
            getSyncDataHolder().markClientSyncFieldDirty("targetStage");
        } else if (id >= ID_ANCHOR && id < ID_ANCHOR + 16) toggleAnchor(id - ID_ANCHOR);
        else HudRecipes.handle(this, id);
    }

    @Override
    public brachy.modularui.screen.ModularPanel<?> buildUI(brachy.modularui.factory.PosGuiData data,
                                                           PanelSyncManager syncManager,
                                                           brachy.modularui.screen.UISettings settings) {
        return MachineHud.build(this, data, syncManager, settings, this::hudAction, this::hudModel);
    }

    private HudModel.Badge growthBadge() {
        if (!isFormed()) return new HudModel.Badge("NOT FORMED", HudModel.RED);
        if (growthStage >= maxStage()) return new HudModel.Badge("FULLY GROWN", HudModel.GOLD);
        if (isControlled() && growthPaused) return new HudModel.Badge("PAUSED", HudModel.GOLD);
        if (growthStall == STALL_COST) return new HudModel.Badge("NEEDS MATERIALS", HudModel.RED);
        if (growthStall == STALL_BLOCKED) return new HudModel.Badge("BLOCKED", HudModel.RED);
        return new HudModel.Badge("GROWING", HudModel.GREEN);
    }

    private HudModel hudModel() {
        return HudModel.of("Growth Chamber")
                .formed(this::isFormed)
                .badge(this::growthBadge)
                .subtitle(() -> isControlled() ? "Growth control hatch fitted" :
                        "No growth control hatch: growth is wild and breaks what is in its way")
                .cells(() -> {
                    var cells = new java.util.ArrayList<HudModel.Cell>();
                    GrowthStage stage = currentStage();
                    cells.add(HudModel.cell("STAGE", growthStage + " / " + maxStage(),
                            growthStage >= maxStage() ? "fully grown" : "of " + (maxStage() + 1) + " stages",
                            HudModel.CYAN));
                    cells.add(HudModel.cell("SPEED", "+" + Math.round(totalSpeedBonus() * 100) + "%",
                            "stage +" + Math.round(stage.speedBonus() * 100) + "%", HudModel.GREEN));
                    cells.add(HudModel.cell("OUTPUT", "+" + Math.round(totalOutputBonus() * 100) + "%",
                            "stage +" + Math.round(stage.outputBonus() * 100) + "%", HudModel.GOLD));
                    cells.add(HudModel.cell("BATCH", "x" + Math.max(1, stage.batch()), "per cycle", HudModel.PURPLE));
                    cells.add(HudModel.cell("TENDRIL BLOCKS", String.valueOf(tendrilBlockCount),
                            "+" + Math.round(tendrilSpeedBonus() * 100) + "% speed", HudModel.BLUE));
                    cells.add(HudModel.cell("TENDRILS", activeTendrils.size() + " growing",
                            maturedTendrils + " of " + getMaxMaturedTendrils() + " grown", HudModel.ORANGE));
                    return cells;
                })
                .bars(() -> {
                    var bars = new java.util.ArrayList<HudModel.Bar>();
                    bars.add(new HudModel.Bar("Growth to the next stage", getGrowthProgress(),
                            Math.round(getGrowthProgress() * 100) + "%", HudModel.GREEN));
                    bars.add(new HudModel.Bar("Stages", growthStage / (float) Math.max(1, maxStage()),
                            growthStage + " / " + maxStage(), HudModel.CYAN));
                    return bars;
                })
                .slider("TARGET", ID_TARGET, () -> Math.min(targetStage, maxStage()), 0, Math.max(1, maxStage()), null,
                        "")
                .button(() -> growthPaused ? "RESUME" : "PAUSE", ID_PAUSE, HudModel.GOLD, this::isControlled)
                .button(() -> tendrilsEnabled ? "TENDRILS ON" : "TENDRILS OFF", ID_TENDRILS, HudModel.GREEN,
                        this::isControlled)
                .button("GROW NOW", ID_GROW, HudModel.CYAN, this::isFormed)
                .notes(() -> {
                    List<GrowthStage> stages = getGrowthStages();
                    if (growthStage + 1 >= stages.size()) {
                        return java.util.List.of(HudModel.line("Fully grown.", HudModel.GOLD));
                    }
                    StringBuilder text = new StringBuilder("Next stage needs:");
                    List<ItemStack> cost = stages.get(growthStage + 1).cost();
                    if (cost.isEmpty()) text.append(" nothing");
                    for (ItemStack required : cost) {
                        text.append(" ").append(required.getCount()).append("x ")
                                .append(required.getHoverName().getString());
                    }
                    var lines = new java.util.ArrayList<HudModel.Line>();
                    lines.add(HudModel.line(text.toString(), HudModel.MID));
                    if (growthStall == STALL_COST) {
                        lines.add(HudModel.line("Waiting: that is not in the input buses.", HudModel.RED));
                    } else if (growthStall == STALL_BLOCKED) {
                        lines.add(HudModel.line("Waiting: something is in the way.", HudModel.RED));
                    }
                    return lines;
                })
                .math(() -> java.util.List.of(
                        HudModel.heading("BONUSES"),
                        HudModel.line("speed = min(0.75, stage + tendrils)", HudModel.MID),
                        HudModel.line(String.format(java.util.Locale.ROOT, "= min(0.75, %.2f + %.2f) = %.2f",
                                currentStage().speedBonus(), tendrilSpeedBonus(), totalSpeedBonus()), HudModel.WHITE),
                        HudModel.line("output = stage + tendrils", HudModel.MID),
                        HudModel.line(String.format(java.util.Locale.ROOT, "= %.2f + %.2f = %.2f",
                                currentStage().outputBonus(), tendrilOutputBonus(), totalOutputBonus()), HudModel.WHITE),
                        HudModel.heading("TENDRILS"),
                        HudModel.line("tendril speed = min(max, blocks * per block)", HudModel.MID),
                        HudModel.line(String.format(java.util.Locale.ROOT, "%d blocks: +%.1f%% speed, +%.1f%% output",
                                tendrilBlockCount, tendrilSpeedBonus() * 100, tendrilOutputBonus() * 100),
                                HudModel.WHITE),
                        HudModel.heading("STAGES"),
                        HudModel.line("stage " + growthStage + " of " + maxStage() + (isControlled() ?
                                ", stops at " + Math.min(targetStage, maxStage()) : ""), HudModel.WHITE)))
                .graph("growth %", HudModel.GREEN, () -> getGrowthProgress() * 100.0f, 100.0f)
                .graph("stage", HudModel.CYAN, () -> (float) growthStage, Math.max(1, maxStage()))
                .graph("speed %", HudModel.GOLD, () -> totalSpeedBonus() * 100.0f, 0.0f)
                .graph("tendril blocks", HudModel.BLUE, () -> (float) tendrilBlockCount, 0.0f);
    }
}
