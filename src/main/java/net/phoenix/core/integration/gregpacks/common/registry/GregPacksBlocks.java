package net.phoenix.core.integration.gregpacks.common.registry;

import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.gregpacks.common.block.OmniPackBlock;
import net.phoenix.core.integration.gregpacks.common.item.OmniPackTier;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

@SuppressWarnings("all")
public class GregPacksBlocks {

    public static final BlockEntry<OmniPackBlock> BASIC_OMNIPACK_BLOCK = REGISTRATE.block("basic_omnipack",
            props -> new OmniPackBlock(OmniPackTier.BASIC, props))
            .properties(p -> p.strength(2f).sound(SoundType.METAL).noOcclusion())
            .blockstate((ctx, prov) -> {})
            .item(OmniPackBlockItem::new)
            .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), PhoenixCore.id("block/omnipack_base"))
                    .texture("pack", PhoenixCore.id("item/basic_omnipack"))
                    .texture("particle", PhoenixCore.id("item/basic_omnipack")))
            .build()
            .lang("§bBasic OmniPack")
            .register();

    public static final BlockEntry<OmniPackBlock> ADVANCED_OMNIPACK_BLOCK = REGISTRATE
            .block("advanced_omnipack",
                    props -> new OmniPackBlock(OmniPackTier.ADVANCED, props))
            .properties(p -> p.strength(2f).sound(SoundType.METAL).noOcclusion())
            .blockstate((ctx, prov) -> {})
            .item(OmniPackBlockItem::new)
            .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), PhoenixCore.id("block/omnipack_base"))
                    .texture("pack", PhoenixCore.id("item/advanced_omnipack"))
                    .texture("particle", PhoenixCore.id("item/advanced_omnipack")))
            .build()
            .lang("§eAdvanced OmniPack")
            .register();

    public static final BlockEntry<OmniPackBlock> ELITE_OMNIPACK_BLOCK = REGISTRATE.block("elite_omnipack",
            props -> new OmniPackBlock(OmniPackTier.ELITE, props))
            .properties(p -> p.strength(2f).sound(SoundType.METAL).noOcclusion())
            .blockstate((ctx, prov) -> {})
            .item(OmniPackBlockItem::new)
            .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), PhoenixCore.id("block/omnipack_base"))
                    .texture("pack", PhoenixCore.id("item/elite_omnipack"))
                    .texture("particle", PhoenixCore.id("item/elite_omnipack")))
            .build()
            .lang("§cElite OmniPack")
            .register();

    public static Item getItemForTier(OmniPackTier tier) {
        return switch (tier) {
            case BASIC -> BASIC_OMNIPACK_BLOCK.asItem();
            case ADVANCED -> ADVANCED_OMNIPACK_BLOCK.asItem();
            case ELITE -> ELITE_OMNIPACK_BLOCK.asItem();
        };
    }

    public static void init() {}
}
