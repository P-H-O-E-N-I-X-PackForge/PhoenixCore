package net.phoenix.core.integration.conflux.structure;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryObject;
import net.phoenix.core.PhoenixCore;

public final class ConfluxStructures {

    private static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister
            .create(Registries.STRUCTURE_TYPE, PhoenixCore.MOD_ID);
    private static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister
            .create(Registries.STRUCTURE_PIECE, PhoenixCore.MOD_ID);

    public static final RegistryObject<StructureType<ConfluxStructure>> CONFLUX = TYPES.register("conflux_structure",
            () -> () -> ConfluxStructure.CODEC);
    public static final RegistryObject<StructurePieceType> PIECE = PIECES.register("conflux_piece",
            () -> (StructurePieceType) ConfluxPiece::new);

    private ConfluxStructures() {}

    public static void init(IEventBus modBus) {
        TYPES.register(modBus);
        PIECES.register(modBus);
    }
}
