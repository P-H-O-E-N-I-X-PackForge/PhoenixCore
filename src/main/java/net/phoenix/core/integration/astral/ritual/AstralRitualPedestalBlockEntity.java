package net.phoenix.core.integration.astral.ritual;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.phoenix.core.integration.astral.puzzle.AstralSurveyItem;

import java.util.Map;

public class AstralRitualPedestalBlockEntity extends BlockEntity {

    private static final Direction[] RING_OFFSETS_DIRECTIONS = { Direction.NORTH, Direction.SOUTH, Direction.EAST,
            Direction.WEST };
    public static final int RING_RADIUS = 2;
    public static final int CHANNEL_TICKS = 80;

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

    private void showMissingRingPositions(ServerLevel serverLevel) {
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

    private float moonFullness() {
        if (level == null) return 0.0f;
        int phase = level.getMoonPhase();
        int away = Math.min(phase, 8 - phase);
        return 1.0f - away / 4.0f;
    }

    public boolean canBegin(Player player) {
        if (!(level instanceof ServerLevel serverLevel)) return false;
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
        if (!RITUAL_TABLE.containsKey(player.getOffhandItem().getItem())) {
            player.displayClientMessage(Component.literal("Hold a catalyst in your off hand."), true);
            return false;
        }
        player.displayClientMessage(Component.literal("§dYou begin to channel... keep holding."), true);
        return true;
    }

    public void channelTick(Player player, int ticks) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!RITUAL_TABLE.containsKey(player.getOffhandItem().getItem())) {
            player.stopUsingItem();
            return;
        }
        float progress = ticks / (float) CHANNEL_TICKS;
        Vec3 center = Vec3.atCenterOf(worldPosition).add(0, 0.8, 0);
        if (ticks % 2 == 0) {
            for (Direction dir : RING_OFFSETS_DIRECTIONS) {
                BlockPos rune = worldPosition.relative(dir, RING_RADIUS);
                Vec3 from = Vec3.atCenterOf(rune).add(0, 0.6, 0);
                Vec3 flow = center.subtract(from).normalize();
                serverLevel.sendParticles(ParticleTypes.ENCHANT, from.x, from.y, from.z, 0, flow.x * 2.5,
                        flow.y * 2.5 + 0.5, flow.z * 2.5, 1.0);
            }
        }
        if (ticks % 10 == 0) {
            serverLevel.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 2 + (int) (progress * 8),
                    0.25, 0.25, 0.25, 0.01);
            serverLevel.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.5f,
                    0.7f + progress * 0.9f);
        }
    }

    public void complete(Player player) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        ItemStack catalyst = player.getOffhandItem();
        RitualResult result = RITUAL_TABLE.get(catalyst.getItem());
        if (result == null || !isRingFormed()) {
            player.displayClientMessage(Component.literal("The ritual fizzles."), true);
            return;
        }
        float fullness = moonFullness();
        float multiplier = 1.0f + 0.5f * fullness;
        boolean amethyst = catalyst.getItem() == Items.AMETHYST_SHARD;
        catalyst.shrink(1);

        int thread = Math.round(result.threadAmount() * multiplier);
        int filament = Math.max(1, Math.round(result.filamentCount() * multiplier));
        ItemStack cell = findThreadCell(player);
        if (!cell.isEmpty()) {
            AstralThreadCellItem.addThread(cell, thread);
        } else {
            dropFilament(serverLevel, filament);
        }

        int lines = 1 + (fullness >= 0.5f ? 1 : 0) + (amethyst ? 1 : 0);
        for (ItemStack stack : player.getInventory().items) {
            if (AstralSurveyItem.isSurvey(stack) && AstralSurveyItem.complete(stack) &&
                    !AstralSurveyItem.phaseKnown(stack)) {
                AstralSurveyItem.revealPhase(stack);
                player.displayClientMessage(Component.literal("§dThe stars show you the moon this machine favours."),
                        true);
                break;
            }
            if (!AstralSurveyItem.isSurvey(stack) || AstralSurveyItem.complete(stack)) continue;
            int shown = AstralSurveyItem.reveal(stack, serverLevel.getRandom(), lines);
            if (shown > 0) {
                player.displayClientMessage(Component.literal("§bThe stars show you " + shown + " more line" +
                        (shown == 1 ? "" : "s") + " (" + AstralSurveyItem.known(stack).length + "/" +
                        AstralSurveyItem.total(stack) + ")."), true);
                break;
            }
        }

        serverLevel.sendParticles(ParticleTypes.END_ROD, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0,
                worldPosition.getZ() + 0.5, 40, 0.4, 0.7, 0.4, 0.04);
        serverLevel.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.7f,
                1.2f + fullness * 0.4f);
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

    private void dropFilament(ServerLevel level, int count) {
        ItemStack stack = com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper.get(
                com.gregtechceu.gtceu.api.data.tag.TagPrefix.dust, AstralMaterials.ASTRAL_FILAMENT, count);
        if (!stack.isEmpty()) {
            Vec3 pos = Vec3.atCenterOf(worldPosition).add(0, 1, 0);
            var entity = new net.minecraft.world.entity.item.ItemEntity(level, pos.x, pos.y, pos.z, stack);
            level.addFreshEntity(entity);
        }
    }
}
