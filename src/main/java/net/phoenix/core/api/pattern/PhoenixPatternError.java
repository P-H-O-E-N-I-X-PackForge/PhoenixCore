package net.phoenix.core.api.pattern;

import com.gregtechceu.gtceu.api.multiblock.error.PatternError;
import com.gregtechceu.gtceu.api.multiblock.error.PatternErrorType;
import com.gregtechceu.gtceu.api.multiblock.error.PatternErrorUI;
import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import brachy.modularui.api.drawable.Text;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * An error for a multiblock's structure with a heading, a hint on how to fix it and, optionally, the place it is about.
 * It is a pattern error like GT's own, so it shows in the unformed UI and can be highlighted in the world.
 */
public class PhoenixPatternError extends PatternError {

    // spotless:off
    public static final MapCodec<PhoenixPatternError> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("title").forGetter(e -> e.title),
            ComponentSerialization.CODEC.optionalFieldOf("hint").forGetter(e -> Optional.ofNullable(e.hint)),
            BlockPos.CODEC.fieldOf("pos").forGetter(PatternError::pos)
    ).apply(instance, (title, hint, pos) -> new PhoenixPatternError(title, hint.orElse(null), pos)));
    //spotless:on

    private final Component title;
    private final @Nullable Component hint;

    public PhoenixPatternError(Component title, @Nullable Component hint, BlockPos pos) {
        super(pos);
        this.title = title;
        this.hint = hint;
    }

    /** An error about the controller itself rather than a block of the structure. */
    public PhoenixPatternError(Component title, @Nullable Component hint) {
        this(title, hint, CONTROLLER);
    }

    /** "Expected this at that place", with the blocks that would do as the hint. */
    public static PhoenixPatternError expected(String what, BlockPos pos, List<BlockInfo> candidates) {
        String names = candidates.stream().map(c -> c.getItemStackForm().getHoverName().getString()).distinct()
                .limit(6).collect(Collectors.joining(", "));
        return new PhoenixPatternError(
                Component.literal("Needs " + what + " at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()),
                names.isEmpty() ? null : Component.literal("One of: " + names), pos);
    }

    @Override
    public PatternErrorUI getPatternErrorUIModifier() {
        return parent -> {
            parent.child(Text.of(title.copy().withStyle(ChatFormatting.YELLOW)).asWidget());
            if (hint != null) parent.child(Text.of(hint.copy().withStyle(ChatFormatting.GRAY)).asWidget());
        };
    }

    @Override
    public PatternErrorType type() {
        return PhoenixPatternErrors.PHOENIX_ERROR.value();
    }
}
