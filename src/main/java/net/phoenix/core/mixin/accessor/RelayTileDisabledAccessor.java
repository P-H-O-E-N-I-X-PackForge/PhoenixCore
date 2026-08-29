package net.phoenix.core.mixin.accessor;

import com.hollingsworth.arsnouveau.common.block.tile.RelayTile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = RelayTile.class, remap = false)
public interface RelayTileDisabledAccessor {

    @Accessor("disabled")
    boolean phoenix$isDisabled();
}
