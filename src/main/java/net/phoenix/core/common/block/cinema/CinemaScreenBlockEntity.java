package net.phoenix.core.common.block.cinema;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class CinemaScreenBlockEntity extends BlockEntity {

    public enum TextAlign { LEFT, CENTER, RIGHT }

    public enum Background { VOID_GALAXY, NEBULA, SCULK_ABYSS, SEALED_INDUSTRIAL, SEALED_CHAOS, SUNFLARE }

    private static final int DEFAULT_COLOR = 0xFFFFFFFF;
    private static final float DEFAULT_SCALE = 0.015f;

    private static final int[] COLOR_PRESETS = {
            0xFFFFFFFF, 0xFFB983FF, 0xFF8C33BF, 0xFF6FA8FF,
            0xFFFF5C5C, 0xFFFFD65C, 0xFF5CFF8F, 0xFF00FFFF,
    };

    private final List<Component> lines = new ArrayList<>();
    private int currentLine = 0;
    private int textColor = DEFAULT_COLOR;
    private float textScale = DEFAULT_SCALE;
    private TextAlign textAlign = TextAlign.CENTER;
    private Background background = Background.VOID_GALAXY;

    public CinemaScreenBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);

        lines.add(Component.literal("Cinema"));
        lines.add(Component.literal("Right-click to advance"));
        lines.add(Component.literal("Shift-right-click to edit"));
    }

    public void applyConfig(List<Component> newLines, int color, float scale, int alignOrdinal,
                             int backgroundOrdinal) {
        lines.clear();
        lines.addAll(newLines);
        currentLine = 0;
        textColor = color;
        textScale = scale;
        TextAlign[] alignValues = TextAlign.values();
        textAlign = alignValues[Math.max(0, Math.min(alignValues.length - 1, alignOrdinal))];
        Background[] backgroundValues = Background.values();
        background = backgroundValues[Math.max(0, Math.min(backgroundValues.length - 1, backgroundOrdinal))];
        syncToClients();
    }

    public void cycleBackground(boolean forward) {
        Background[] values = Background.values();
        int next = (background.ordinal() + (forward ? 1 : -1) + values.length) % values.length;
        background = values[next];
        syncToClients();
    }

    public void advanceLine() {
        if (lines.isEmpty()) return;
        currentLine = (currentLine + 1) % lines.size();
        syncToClients();
    }

    public void removeCurrentLine() {
        if (lines.size() <= 1) return;
        lines.remove(currentLine);
        if (currentLine >= lines.size()) currentLine = lines.size() - 1;
        syncToClients();
    }

    public void cycleColor() {
        int index = 0;
        for (int i = 0; i < COLOR_PRESETS.length; i++) {
            if (COLOR_PRESETS[i] == textColor) {
                index = i;
                break;
            }
        }
        textColor = COLOR_PRESETS[(index + 1) % COLOR_PRESETS.length];
        syncToClients();
    }

    public Component getCurrentLine() {
        return lines.isEmpty() ? Component.empty() : lines.get(currentLine);
    }

    public int getCurrentLineIndex() {
        return currentLine;
    }

    public List<Component> getLines() {
        return lines;
    }

    public int getTextColor() {
        return textColor;
    }

    public float getTextScale() {
        return textScale;
    }

    public TextAlign getTextAlign() {
        return textAlign;
    }

    public Background getBackground() {
        return background;
    }

    private void syncToClients() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("CurrentLine", currentLine);
        tag.putInt("TextColor", textColor);
        tag.putFloat("TextScale", textScale);
        tag.putString("TextAlign", textAlign.name());
        tag.putString("Background", background.name());
        ListTag linesTag = new ListTag();
        for (Component line : lines) {
            linesTag.add(StringTag.valueOf(Component.Serializer.toJson(line)));
        }
        tag.put("Lines", linesTag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        currentLine = tag.getInt("CurrentLine");
        textColor = tag.contains("TextColor") ? tag.getInt("TextColor") : DEFAULT_COLOR;
        textScale = tag.contains("TextScale") ? tag.getFloat("TextScale") : DEFAULT_SCALE;
        if (tag.contains("TextAlign")) {
            try {
                textAlign = TextAlign.valueOf(tag.getString("TextAlign"));
            } catch (IllegalArgumentException e) {
                textAlign = TextAlign.CENTER;
            }
        }
        if (tag.contains("Background")) {
            try {
                background = Background.valueOf(tag.getString("Background"));
            } catch (IllegalArgumentException e) {
                background = Background.VOID_GALAXY;
            }
        }
        lines.clear();
        if (tag.contains("Lines", Tag.TAG_LIST)) {
            ListTag linesTag = tag.getList("Lines", Tag.TAG_STRING);
            for (int i = 0; i < linesTag.size(); i++) {
                Component parsed = Component.Serializer.fromJson(linesTag.getString(i));
                lines.add(parsed != null ? parsed : Component.empty());
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
