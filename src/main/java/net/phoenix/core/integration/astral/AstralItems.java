package net.phoenix.core.integration.astral;

import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.integration.astral.item.AstralCodexItem;
import net.phoenix.core.integration.astral.item.AstralThreadCellItem;
import net.phoenix.core.integration.astral.item.AstralWandItem;

import com.tterrag.registrate.util.entry.ItemEntry;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public class AstralItems {

    public static final ItemEntry<AstralWandItem> ASTRAL_WAND = REGISTRATE
            .item("astral_wand", AstralWandItem::new)
            .properties(p -> p.stacksTo(1))
            .model((ctx, prov) -> prov.handheld(ctx,
                    ResourceLocation.fromNamespaceAndPath("minecraft", "item/blaze_rod")))
            .lang("§dAstral Wand")
            .register();

    public static final ItemEntry<AstralThreadCellItem> ASTRAL_THREAD_CELL = REGISTRATE
            .item("astral_thread_cell", AstralThreadCellItem::new)
            .properties(p -> p.stacksTo(1))
            .model((ctx, prov) -> prov.generated(ctx,
                    ResourceLocation.fromNamespaceAndPath("minecraft", "item/ender_pearl")))
            .lang("§5Astral Thread Cell")
            .register();

    public static final ItemEntry<AstralCodexItem> ASTRAL_CODEX = REGISTRATE
            .item("astral_codex", AstralCodexItem::new)
            .properties(p -> p.stacksTo(1))
            .model((ctx, prov) -> prov.generated(ctx,
                    ResourceLocation.fromNamespaceAndPath("minecraft", "item/enchanted_book")))
            .lang("§dAstral Codex")
            .register();

    public static void init() {}
}
