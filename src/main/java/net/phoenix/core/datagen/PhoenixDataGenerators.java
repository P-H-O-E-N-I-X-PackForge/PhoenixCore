package net.phoenix.core.datagen;

import com.gregtechceu.gtceu.api.registry.registrate.SoundEntryBuilder;
import com.gregtechceu.gtceu.common.data.GTMaterialBlocks;

import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.data.materials.PhoenixMaterialFlags;

import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.loot.RegistrateLootTableProvider;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class PhoenixDataGenerators {

    static {

        PhoenixDatagen.init();

        REGISTRATE.addDataGenerator(ProviderType.LOOT,
                prov -> prov.addLootAction(RegistrateLootTableProvider.LootType.BLOCK, blockLoot -> {
                    for (var entry : GTMaterialBlocks.MATERIAL_BLOCKS.row(PhoenixMaterialFlags.crystal_rose)
                            .values()) {
                        if (!entry.getId().getNamespace().equals(PhoenixCore.MOD_ID)) continue;
                        blockLoot.dropSelf(entry.get());
                    }
                }));

        REGISTRATE.addDataGenerator(ProviderType.LANG, prov -> {
            prov.add("tooltip.phoenixcore.cinder_core.target", "Target: %s");
            prov.add("tooltip.phoenixcore.cinder_core.unconfigured",
                    "Not configured - shift-right-click to pick a multiblock");
            prov.add("tooltip.phoenixcore.cinder_core.materials", "Stocked: %s material types, %s items total");
            prov.add("tooltip.phoenixcore.cinder_core.hint",
                    "Shift-right-click: configure  •  Right-click a block: preview / place");
            prov.add("tooltip.phoenixcore.cinder_core.ready", "All materials stocked - ready to build");
            prov.add("tooltip.phoenixcore.cinder_core.missing_entry", "  %s: %s/%s");
            prov.add("tooltip.phoenixcore.cinder_core.missing_more", "  ...and %s more");
            prov.add("key.phoenixcore.cinder_atlas_radial", "Cinder Atlas Loadout Wheel");
            prov.add("entity.phoenixcore.teto_slime", "Teto Slime");
            prov.add("item.phoenixcore.teto_slime_spawn_egg", "Teto Slime Spawn Egg");
        });
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        PackOutput packOutput = event.getGenerator().getPackOutput();

        if (event.includeClient()) {

            net.phoenix.core.client.renderer.machine.multiblock.PhoenixDynamicRenderHelpers.registerAll();

            event.getGenerator().addProvider(
                    true,
                    new SoundEntryBuilder.SoundEntryProvider(packOutput, PhoenixCore.MOD_ID));

        }
    }
}
