package net.phoenix.core.client;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlag;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.api.registry.registrate.provider.GTBlockstateProvider;
import com.gregtechceu.gtceu.data.pack.event.RegisterDynamicResourcesEvent;

import net.minecraft.client.particle.SuspendedTownParticle;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.client.command.TerrainPreviewCommand;
import net.phoenix.core.client.renderer.machine.*;
import net.phoenix.core.client.worldfx.WorldFXShaders;
import net.phoenix.core.common.data.materials.PhoenixMaterialFlags;
import net.phoenix.core.integration.conflux.ConfluxDataType;
import net.phoenix.core.integration.conflux.ConfluxRegistry;
import net.phoenix.core.integration.conflux.client.ConfluxEditorCommand;
import net.phoenix.core.integration.conflux.dimension.particles.DimensionParticleTypes;
import net.phoenix.core.integration.conflux.tools.capture.ExportSpritesCommand;
import net.phoenix.core.integration.conflux.tools.capture.SpriteCaptureRegistry;
import net.phoenix.core.integration.conflux.tools.capture.bakers.ConfluxPhoenixBaker;
import net.phoenix.core.integration.conflux.tools.capture.bakers.ConfluxSculkBaker;
import net.phoenix.core.integration.conflux.tools.capture.bakers.ConfluxSealedBaker;
import net.phoenix.core.integration.conflux.tools.capture.bakers.ConfluxVoidBaker;
import net.phoenix.core.integration.recipe_helper.RecipeBuilderScreen;
import net.phoenix.core.integration.vocal_vibrancy.VocalVibrancyClient;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID, value = Dist.CLIENT)
public class PhoenixClient {

    private PhoenixClient() {}

    public static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.register(VocalVibrancyClientTick.class);
        net.phoenix.core.integration.gregvaults.client.GregTechVaultsClient.init(modBus);
        modBus.addListener(PhoenixClient::registerDynamicPipeModels);
        modBus.addListener(WorldFXShaders::onRegisterShaders);

        net.phoenix.core.client.renderer.machine.multiblock.PhoenixDynamicRenderHelpers.registerAll();
    }

    public static void registerDynamicPipeModels(RegisterDynamicResourcesEvent event) {
        GTBlockstateProvider provider = com.gregtechceu.gtceu.utils.data.RuntimeBlockstateProvider.INSTANCE;
        for (ConfluxDataType dt : ConfluxDataType.values()) {
            ConfluxRegistry.PIPES.get(dt).get()
                    .createPipeModel(provider)
                    .dynamicModel();
        }
        for (net.phoenix.core.integration.ars_nouveau.logistics.SourcePipeTier tier : net.phoenix.core.integration.ars_nouveau.logistics.SourcePipeTier
                .values()) {
            net.phoenix.core.integration.ars_nouveau.logistics.SourceLogistics.PIPES.get(tier).get()
                    .createPipeModel(provider).dynamicModel();
        }
    }

    public static class VocalVibrancyClientTick {

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            VocalVibrancyClient.tick();
        }
    }

    @SubscribeEvent
    public static void registerConfluxPipeBlockColors(RegisterColorHandlersEvent.Block event) {
        for (ConfluxDataType dt : ConfluxDataType.values()) {
            event.register((state, level, pos, tint) -> {
                if (level != null && pos != null &&
                        level.getBlockEntity(
                                pos) instanceof net.phoenix.core.integration.conflux.pipe.ConfluxPipeBlockEntity pipe) {
                    return 0xFF000000 | (pipe.isPainted() ? pipe.getPaintingColor() :
                            net.phoenix.core.integration.conflux.client.ConfluxTheme.typeColor(dt) & 0xFFFFFF);
                }
                return net.phoenix.core.integration.conflux.client.ConfluxTheme.typeColor(dt);
            }, ConfluxRegistry.PIPES.get(dt).get());
        }
    }

    @SubscribeEvent
    public static void registerSourcePipeBlockColors(RegisterColorHandlersEvent.Block event) {
        for (net.phoenix.core.integration.ars_nouveau.logistics.SourcePipeTier tier : net.phoenix.core.integration.ars_nouveau.logistics.SourcePipeTier
                .values()) {
            event.register((state, level, pos, tint) -> {
                if (level != null && pos != null && level.getBlockEntity(
                        pos) instanceof net.phoenix.core.integration.ars_nouveau.logistics.SourcePipeBlockEntity pipe) {
                    return 0xFF000000 | (pipe.isPainted() ? pipe.getPaintingColor() : tier.color() & 0xFFFFFF);
                }
                return tier.color();
            }, net.phoenix.core.integration.ars_nouveau.logistics.SourceLogistics.PIPES.get(tier).get());
        }
    }

    @SubscribeEvent
    public static void registerSourcePipeItemColors(RegisterColorHandlersEvent.Item event) {
        for (net.phoenix.core.integration.ars_nouveau.logistics.SourcePipeTier tier : net.phoenix.core.integration.ars_nouveau.logistics.SourcePipeTier
                .values()) {
            event.register((stack, tint) -> tier.color(),
                    net.phoenix.core.integration.ars_nouveau.logistics.SourceLogistics.PIPES.get(tier).get());
        }
    }

    @SubscribeEvent
    public static void registerConfluxPipeItemColors(RegisterColorHandlersEvent.Item event) {
        for (ConfluxDataType dt : ConfluxDataType.values()) {
            event.register((stack, tint) -> net.phoenix.core.integration.conflux.client.ConfluxTheme.typeColor(dt),
                    ConfluxRegistry.PIPES.get(dt).get());
        }
    }

    @SubscribeEvent
    public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(DimensionParticleTypes.VOLCANIC_ASH.get(), SuspendedTownParticle.Provider::new);
        event.registerSpriteSet(DimensionParticleTypes.HEAT_SHIMMER.get(), SuspendedTownParticle.Provider::new);
        event.registerSpriteSet(DimensionParticleTypes.LAVA_SPARK.get(), SuspendedTownParticle.Provider::new);
        event.registerSpriteSet(DimensionParticleTypes.SCULK_SPORE.get(), SuspendedTownParticle.Provider::new);
        event.registerSpriteSet(DimensionParticleTypes.BIOLUM_GLOW.get(), SuspendedTownParticle.Provider::new);
        event.registerSpriteSet(DimensionParticleTypes.SCULK_TENDRIL.get(), SuspendedTownParticle.Provider::new);
        event.registerSpriteSet(DimensionParticleTypes.COSMIC_DUST.get(), SuspendedTownParticle.Provider::new);
        event.registerSpriteSet(DimensionParticleTypes.VOID_SHIMMER.get(), SuspendedTownParticle.Provider::new);
        event.registerSpriteSet(DimensionParticleTypes.STAR_SPARKLE.get(), SuspendedTownParticle.Provider::new);
        event.registerSpriteSet(DimensionParticleTypes.NEON_SPARK.get(), SuspendedTownParticle.Provider::new);
        event.registerSpriteSet(DimensionParticleTypes.INDUSTRIAL_SMOKE.get(), SuspendedTownParticle.Provider::new);
        event.registerSpriteSet(DimensionParticleTypes.ENERGY_BOLT.get(), SuspendedTownParticle.Provider::new);
        event.registerSpriteSet(DimensionParticleTypes.GLITCH_EFFECT.get(), SuspendedTownParticle.Provider::new);
        event.registerSpriteSet(DimensionParticleTypes.REALITY_TEAR.get(), SuspendedTownParticle.Provider::new);
        event.registerSpriteSet(DimensionParticleTypes.INVERTED_GLOW.get(), SuspendedTownParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone(EyeOfHarmonyRender.SPACE_SHELL_MODEL_RL));
        event.register(ModelResourceLocation.standalone(EyeOfHarmonyRender.STAR_MODEL_RL));
        EyeOfHarmonyRender.ORBIT_OBJECTS_RL.forEach(rl -> event.register(ModelResourceLocation.standalone(rl)));
        event.register(ModelResourceLocation.standalone(ArtificialStarRender.ARTIFICIAL_STAR_MODEL_RL));
        event.register(ModelResourceLocation.standalone(PlasmaArcFurnaceRender.RINGS_MODEL_RL));
        event.register(ModelResourceLocation.standalone(PlasmaArcFurnaceRender.SPHERE_MODEL_RL));
    }

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiLayersEvent event) {
        event.registerAboveAll(net.phoenix.core.PhoenixCore.id("spray_can_info"),
                net.phoenix.core.client.renderer.SprayCanHudOverlay.HUD_SPRAY_CAN);
        event.registerAboveAll(net.phoenix.core.PhoenixCore.id("shader_profiler"),
                net.phoenix.core.client.renderer.ShaderProfilerOverlay.HUD_SHADER_PROFILER);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void registerBeeMaterialColors(RegisterColorHandlersEvent.Item event) {
        registerBeeTierColors(event, PhoenixMaterialFlags.GENERATE_TIER_ONE_BEE,
                PhoenixMaterialFlags.tier_one_bee.get());
        registerBeeTierColors(event, PhoenixMaterialFlags.GENERATE_TIER_TWO_BEE,
                PhoenixMaterialFlags.tier_two_bee.get());
        registerBeeTierColors(event, PhoenixMaterialFlags.GENERATE_TIER_THREE_BEE,
                PhoenixMaterialFlags.tier_three_bee.get());
    }

    private static void registerBeeTierColors(RegisterColorHandlersEvent.Item event, MaterialFlag flag,
                                              TagPrefix prefix) {
        GTRegistries.MATERIALS.stream()
                .filter(mat -> mat.hasFlag(flag))
                .forEach(mat -> {
                    ItemStack stack = ChemicalHelper.get(prefix, mat);
                    if (stack.isEmpty()) return;
                    event.register((s, tintIndex) -> switch (tintIndex) {
                        case 1 -> mat.getMaterialARGB();
                        case 2 -> mat.getMaterialSecondaryARGB();
                        default -> -1;
                    }, stack.getItem());
                });
    }

    @SubscribeEvent
    public static void registerMenuScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(PhoenixCore.RECIPE_BUILDER_MENU.get(), RecipeBuilderScreen::new);
    }

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        SpriteCaptureRegistry.register(new ConfluxVoidBaker());
        SpriteCaptureRegistry.register(new ConfluxPhoenixBaker());
        SpriteCaptureRegistry.register(new ConfluxSculkBaker());
        SpriteCaptureRegistry.register(new ConfluxSealedBaker());
        event.enqueueWork(SuiteButtons::register);
    }

    @EventBusSubscriber(modid = PhoenixCore.MOD_ID, value = Dist.CLIENT)
    public static class ForgeClientEvents {

        @SubscribeEvent
        public static void registerClientCommands(RegisterClientCommandsEvent event) {
            ConfluxEditorCommand.register(event);
            ExportSpritesCommand.register(event);
            TerrainPreviewCommand.register(event);
        }
    }
}
