package net.phoenix.core.integration.astral.ritual;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.phoenix.core.common.data.materials.AstralMaterials;
import net.phoenix.core.integration.astral.AstralBlocks;
import net.phoenix.core.integration.astral.item.AstralThreadCellItem;

import java.util.Map;

public class AstralRitualPedestalBlockEntity extends BlockEntity {

    private static final Direction[] RING_OFFSETS_DIRECTIONS = { Direction.NORTH, Direction.SOUTH, Direction.EAST,
            Direction.WEST };
    public static final int RING_RADIUS = 2;

    public record RitualResult(int threadAmount, int filamentCount) {}

    public static final Map<Item, RitualResult> RITUAL_TABLE = Map.of(
            Items.AMETHYST_SHARD, new RitualResult(500, 2),
            Items.GLOWSTONE_DUST, new RitualResult(250, 1));

    public AstralRitualPedestalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean isRingFormed() {
        if (level == null) return false;
        for (Direction dir : RING_OFFSETS_DIRECTIONS) {
            BlockPos ringPos = worldPosition.relative(dir, RING_RADIUS);
            if (!level.getBlockState(ringPos).is(AstralBlocks.ASTRAL_RUNE_BLOCK.get())) return false;
        }
        return true;
    }

    private void showMissingRingPositions(net.minecraft.server.level.ServerLevel serverLevel) {
        for (Direction dir : RING_OFFSETS_DIRECTIONS) {
            BlockPos ringPos = worldPosition.relative(dir, RING_RADIUS);
            if (serverLevel.getBlockState(ringPos).is(AstralBlocks.ASTRAL_RUNE_BLOCK.get())) continue;
            serverLevel.sendParticles(ParticleTypes.WITCH, ringPos.getX() + 0.5, ringPos.getY() + 1.0,
                    ringPos.getZ() + 0.5, 20, 0.2, 0.3, 0.2, 0.01);
        }
    }

    private boolean isNight() {
        if (level == null) return false;
        long time = level.getDayTime() % 24000;
        return time >= 13000 && time <= 23000;
    }

    public boolean tryActivateRitual(Player player, ItemStack catalyst) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return false;

        if (!isRingFormed()) {
            player.displayClientMessage(
                    Component.literal("The rune ring is incomplete - place Astral Rune Blocks 2 blocks north, " +
                            "south, east, and west (marked)."),
                    true);
            showMissingRingPositions(serverLevel);
            return false;
        }
        if (!isNight()) {
            player.displayClientMessage(Component.literal("The stars are not yet right - try at night."), true);
            return false;
        }

        RitualResult result = RITUAL_TABLE.get(catalyst.getItem());
        if (result == null) {
            player.displayClientMessage(Component.literal("This catalyst has no effect."), true);
            return false;
        }

        catalyst.shrink(1);

        ItemStack cell = findThreadCell(player);
        if (!cell.isEmpty()) {
            AstralThreadCellItem.addThread(cell, result.threadAmount());
        } else {
            dropFilament(serverLevel, result.filamentCount());
        }

        serverLevel.sendParticles(ParticleTypes.END_ROD, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0,
                worldPosition.getZ() + 0.5, 30, 0.4, 0.6, 0.4, 0.02);
        serverLevel.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6f, 1.4f);
        return true;
    }

    private ItemStack findThreadCell(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof AstralThreadCellItem &&
                    AstralThreadCellItem.getThread(stack) < AstralThreadCellItem.CAPACITY) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private void dropFilament(net.minecraft.server.level.ServerLevel level, int count) {
        ItemStack stack = com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper.get(
                com.gregtechceu.gtceu.api.data.tag.TagPrefix.dust, AstralMaterials.ASTRAL_FILAMENT, count);
        if (!stack.isEmpty()) {
            Vec3 pos = Vec3.atCenterOf(worldPosition).add(0, 1, 0);
            var entity = new net.minecraft.world.entity.item.ItemEntity(level, pos.x, pos.y, pos.z, stack);
            level.addFreshEntity(entity);
        }
    }
}
