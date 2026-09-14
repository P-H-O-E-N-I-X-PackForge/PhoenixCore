package net.phoenix.core.common.machine.multiblock.ward;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SanctumWardMachine extends WorkableElectricMultiblockMachine {

    public static final double WARD_RADIUS_XZ = 25.0;
    private static final double WARD_RADIUS_Y = 32.0;

    private static final int REPEL_INTERVAL_TICKS = 10;
    private static final double REPEL_STRENGTH = 0.35;

    public static final String WARD_MODE_KEY = "ward_mode";

    @SaveField
    @SyncToClient
    private WardMode wardMode = WardMode.NONE;

    private final ConditionalSubscriptionHandler wardHandler;
    private int repelTickCounter = 0;

    public SanctumWardMachine(BlockEntityCreationInfo holder) {
        super(holder, new RecipeLogic());
        this.wardHandler = new ConditionalSubscriptionHandler(this, this::wardTick, this::isFormed);
    }

    @Override
    public boolean beforeWorking(@Nullable GTRecipe recipe) {
        if (recipe != null && recipe.data.contains(WARD_MODE_KEY)) {
            try {
                wardMode = WardMode.valueOf(recipe.data.getString(WARD_MODE_KEY));
            } catch (IllegalArgumentException ignored) {}
        }
        return super.beforeWorking(recipe);
    }

    public WardMode getWardMode() {
        return wardMode;
    }

    @Override
    public void formStructure(@NotNull String substructureName) {
        super.formStructure(substructureName);
        wardHandler.updateSubscription();
        MobWardRegistry.register(this);
    }

    @Override
    public void invalidateStructure(@NotNull String substructureName) {
        super.invalidateStructure(substructureName);
        wardHandler.updateSubscription();
        MobWardRegistry.unregister(this);
    }

    @Override
    public void onUnload() {
        super.onUnload();
        MobWardRegistry.unregister(this);
    }

    public boolean isWardActive() {
        return isFormed() && recipeLogic.isActive();
    }

    public boolean contains(double x, double y, double z) {
        BlockPos center = getBlockPos();
        return Math.abs(x - (center.getX() + 0.5)) <= WARD_RADIUS_XZ &&
                Math.abs(z - (center.getZ() + 0.5)) <= WARD_RADIUS_XZ &&
                Math.abs(y - (center.getY() + 0.5)) <= WARD_RADIUS_Y;
    }

    private void wardTick() {
        if (!isWardActive() || wardMode == WardMode.NONE) return;
        if (!(getLevel() instanceof ServerLevel serverLevel)) return;

        repelTickCounter++;
        if (repelTickCounter < REPEL_INTERVAL_TICKS) return;
        repelTickCounter = 0;

        BlockPos center = getBlockPos();
        AABB area = new AABB(center).inflate(WARD_RADIUS_XZ, WARD_RADIUS_Y, WARD_RADIUS_XZ);

        for (Mob mob : serverLevel.getEntitiesOfClass(Mob.class, area)) {
            if (!wardMode.matches(mob)) continue;

            double dx = mob.getX() - (center.getX() + 0.5);
            double dz = mob.getZ() - (center.getZ() + 0.5);
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < 0.01) {
                dx = serverLevel.random.nextDouble() - 0.5;
                dz = serverLevel.random.nextDouble() - 0.5;
                dist = Math.max(0.01, Math.sqrt(dx * dx + dz * dz));
            }

            mob.setDeltaMovement(mob.getDeltaMovement().add(
                    (dx / dist) * REPEL_STRENGTH, 0.05, (dz / dist) * REPEL_STRENGTH));

            mob.hurtMarked = true;
        }
    }
}
