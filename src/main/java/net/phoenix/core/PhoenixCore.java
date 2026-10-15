package net.phoenix.core;







import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.fml.ModContainer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.common.NeoForge;
import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.event.MaterialEvent;
import com.gregtechceu.gtceu.api.data.chemical.material.event.PostMaterialEvent;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;
import com.gregtechceu.gtceu.api.data.worldgen.GTOreDefinition;
import com.gregtechceu.gtceu.api.data.worldgen.IWorldGenLayer;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.condition.RecipeConditionType;
import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.MapIngredientTypeManager;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import com.gregtechceu.gtceu.api.sound.SoundEntry;
import com.gregtechceu.gtceu.common.data.GTCreativeModeTabs;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.phoenix.core.api.PhoenixSounds;
import net.phoenix.core.api.recipe.lookup.MapShieldIngredient;
import net.phoenix.core.client.PhoenixClient;
import net.phoenix.core.client.keybind.PhoenixKeybinds;
import net.phoenix.core.client.particle.PhoenixParticles;
import net.phoenix.core.common.block.PhoenixBlocks;
import net.phoenix.core.common.data.PhoenixRecipeTypes;
import net.phoenix.core.common.data.item.PhoenixItems;
import net.phoenix.core.common.data.materials.*;
import net.phoenix.core.common.data.recipeConditions.FluidInHatchCondition;
import net.phoenix.core.common.data.worldgen.CrystalRoseIndicatorGenerator;
import net.phoenix.core.common.machine.*;
import net.phoenix.core.common.machine.multiblock.Shield;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.ars_nouveau.api.recipe.lookup.MapSourceIngredient;
import net.phoenix.core.integration.ars_nouveau.client.gui.SourceHatchMenu;
import net.phoenix.core.integration.ars_nouveau.common.data.recipe.custom.SourceIngredient;
import net.phoenix.core.integration.ars_nouveau.common.data.recipeConditons.SoulCondition;
import net.phoenix.core.integration.ars_nouveau.common.event.SourceHatchJarTransferTick;
import net.phoenix.core.integration.astral.AstralBlocks;
import net.phoenix.core.integration.astral.AstralItems;
import net.phoenix.core.integration.astral.AstralMachines;
import net.phoenix.core.integration.conflux.ConfluxRegistry;
import net.phoenix.core.integration.conflux.multiblock.ConfluxMultiblockRegistry;
import net.phoenix.core.integration.conflux.network.ConfluxNetwork;
import net.phoenix.core.integration.conflux.producer.ConfluxProducerMachines;
import net.phoenix.core.integration.conflux.research.AxiomResearchCondition;
import net.phoenix.core.integration.conflux.research.ResearchTreeRegistry;
import net.phoenix.core.integration.gregpacks.client.OmniPackCurioRenderer;
import net.phoenix.core.integration.gregpacks.common.inventory.GregPacksMenus;
import net.phoenix.core.integration.gregpacks.common.inventory.OmniPackKeybind;
import net.phoenix.core.integration.gregpacks.common.inventory.OmniPackScreen;
import net.phoenix.core.integration.gregpacks.common.item.GregPacksItems;
import net.phoenix.core.integration.gregpacks.common.registry.GregPacksBlockEntities;
import net.phoenix.core.integration.gregpacks.common.registry.GregPacksBlocks;
import net.phoenix.core.integration.gregpacks.common.registry.GregPacksUpgrades;
import net.phoenix.core.integration.gregpacks.datagen.GregPacksDataGenerators;
import net.phoenix.core.integration.gregpacks.datagen.GregPacksDatagen;
import net.phoenix.core.integration.gregpacks.network.GregPacksNetwork;
import net.phoenix.core.integration.gregvaults.common.multiblock.VaultMachineDefinition;
import net.phoenix.core.integration.gregvaults.common.recipe.VaultRecipes;
import net.phoenix.core.integration.gregvaults.common.registry.VaultRegistry;
import net.phoenix.core.integration.gregvaults.datagen.VaultDatagen;
import net.phoenix.core.integration.gregvaults.network.VaultNetwork;
import net.phoenix.core.integration.growth.GrowthBlocks;
import net.phoenix.core.integration.growth.GrowthMachines;
import net.phoenix.core.integration.matter_manipulater.common.data.item.ManipulaterItems;
import net.phoenix.core.integration.phoenix_tesla_network.common.machine.PhoenixTeslaMachines;
import net.phoenix.core.integration.recipe_helper.RecipeBuilderMenu;
import net.phoenix.core.network.PhoenixNetwork;

import com.tterrag.registrate.util.entry.RegistryEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

@SuppressWarnings("all")
@Mod(PhoenixCore.MOD_ID)
public class PhoenixCore {

    public static final String MOD_ID = "phoenixcore";
    public static final Logger LOGGER = LogManager.getLogger();
    public static GTRegistrate PHOENIX_REGISTRATE = GTRegistrate.create(MOD_ID);

    public static RegistryEntry<CreativeModeTab> PHOENIX_CREATIVE_TAB = REGISTRATE
            .defaultCreativeTab(PhoenixCore.MOD_ID,
                    builder -> builder
                            .displayItems(new GTCreativeModeTabs.RegistrateDisplayItemsGenerator(PhoenixCore.MOD_ID,
                                    REGISTRATE))
                            .title(REGISTRATE.addLang("itemGroup", PhoenixCore.id("creative_tab"),
                                    "PhoenixCore (CoreMod)"))
                            .icon(() -> PhoenixMachines.HIGH_YIELD_PHOTON_EMISSION_REGULATOR.asStack())
                            .build())
            .register();

    public PhoenixCore(IEventBus modEventBus, ModContainer container) {

        PhoenixConfigs.init();
        CrystalRoseIndicatorGenerator.register();
        VaultDatagen.init();
        GregPacksDatagen.init();
        GregPacksUpgrades.init();

        PHOENIX_REGISTRATE.registerEventListeners(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerPayloads);

        PhoenixParticles.init(modEventBus);
        net.phoenix.core.common.entity.PhoenixEntities.init(modEventBus);
        net.phoenix.core.integration.conflux.structure.ConfluxStructures.init(modEventBus);
        if (FMLEnvironment.dist.isClient()) {
            modEventBus.addListener(PhoenixKeybinds::register);
            PhoenixClient.init(modEventBus);
            modEventBus.addListener(this::clientSetup);
            modEventBus.addListener(OmniPackKeybind::register);
            NeoForge.EVENT_BUS.register(OmniPackKeybind.class);
        }

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::modifyMaterials);

        // GregTech builds its content from its own constructor and so do we, in the same order
        registerMaterialIconSets();
        addMaterials();
        net.phoenix.core.common.data.materials.MaterialLang.register();
        registerSounds();
        registerConditions();
        registerRecipeTypes();
        registerWorldGenLayers();
        onRegisterBlocksAndItems();
        registerMachines();

        VaultRegistry.registerEventBus(modEventBus);
        VaultRecipes.SERIALIZERS.register(modEventBus);
        net.phoenix.core.common.item.cinder.CinderRecipes.SERIALIZERS.register(modEventBus);

        GregPacksItems.init();
        GregPacksBlocks.init();
        GregPacksMenus.MENUS.register(modEventBus);
        GregPacksBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        modEventBus.register(GregPacksDataGenerators.class);

        MENUS.register(modEventBus);

        ConfluxRegistry.register(modEventBus);
        modEventBus.addListener(ConfluxRegistry::registerCapabilities);
        modEventBus.addListener(net.phoenix.core.integration.gregpacks.common.fluid.GregPacksCapabilityHandler::register);
        modEventBus.addListener(net.phoenix.core.integration.ars_nouveau.api.capability.ISourceProviderCapability::register);
        net.phoenix.core.integration.conflux.dimension.ConfluxDimensionRegistry.register(modEventBus);
        net.phoenix.core.integration.conflux.dimension.particles.DimensionParticleTypes.register(modEventBus);
        net.phoenix.core.integration.conflux.dimension.EtherealSpawnGenerator.register();
        net.phoenix.core.integration.conflux.dimension.DisciplineChunkGenerator.register();
        AstralBlocks.registerDeferred(modEventBus);
        PhoenixAttachments.register(modEventBus);
        net.phoenix.core.common.item.PhoenixItemCaps.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new SourceHatchJarTransferTick());
        NeoForge.EVENT_BUS
                .register(net.phoenix.core.integration.conflux.dimension.ConfluxProgressionEvents.class);
        NeoForge.EVENT_BUS.register(net.phoenix.core.integration.conflux.dimension.EtherealSpawnManager.class);
        NeoForge.EVENT_BUS.register(net.phoenix.core.integration.conflux.dimension.PlayerSpawnHandler.class);
        NeoForge.EVENT_BUS.addListener(ResearchTreeRegistry::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(
                net.phoenix.core.integration.conflux.research.GatedRecipeRegistry::onAddReloadListeners);

        GregPacksBlockEntities.init();
    }

    private void onRegisterBlocksAndItems() {
        PhoenixBlocks.init();
        net.phoenix.core.common.block.cinema.CinemaBlocks.init();
        net.phoenix.core.integration.continuum.ContinuumRegistry.init();
        GrowthBlocks.init();
        net.phoenix.core.integration.growth.GrowthItems.init();
        AstralBlocks.init();
        AstralItems.init();
        PhoenixItems.init();
        ManipulaterItems.init();
        net.phoenix.core.integration.drone.DroneItems.init();
        net.phoenix.core.common.item.cinder.CinderItems.init();
        net.phoenix.core.common.block.cinder.CinderBlocks.init();
        net.phoenix.core.common.item.cinder.CinderRecipes.init();
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(GregPacksMenus.OMNIPACK_MENU.get(), OmniPackScreen::new));
        registerCurioRenderers();
    }

    private void registerCurioRenderers() {
        if (!net.neoforged.fml.ModList.get().isLoaded("curios")) return;
        top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(
                GregPacksBlocks.BASIC_OMNIPACK_BLOCK.asItem(), () -> OmniPackCurioRenderer.INSTANCE);
        top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(
                GregPacksBlocks.ADVANCED_OMNIPACK_BLOCK.asItem(), () -> OmniPackCurioRenderer.INSTANCE);
        top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(
                GregPacksBlocks.ELITE_OMNIPACK_BLOCK.asItem(), () -> OmniPackCurioRenderer.INSTANCE);
    }

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BuiltInRegistries.MENU,
            MOD_ID);

    public static final DeferredHolder<MenuType<SourceHatchMenu>, MenuType<SourceHatchMenu>> SOURCE_HATCH_MENU = MENUS.register("source_hatch",
            () -> IMenuTypeExtension.create((IContainerFactory<SourceHatchMenu>) SourceHatchMenu::fromNetwork));

    public static final DeferredHolder<MenuType<RecipeBuilderMenu>, MenuType<RecipeBuilderMenu>> RECIPE_BUILDER_MENU = MENUS.register(
            "recipe_builder",
            () -> IMenuTypeExtension.create(
                    (windowId, inv, data) -> new RecipeBuilderMenu(windowId, inv)));

    public void registerConditions() {
        FluidInHatchCondition.TYPE = new RecipeConditionType<>(
                FluidInHatchCondition::new,
                FluidInHatchCondition.CODEC);


        SoulCondition.TYPE = new RecipeConditionType<>(
                SoulCondition::new,
                SoulCondition.CODEC);


        AxiomResearchCondition.TYPE = new RecipeConditionType<>(
                AxiomResearchCondition::new,
                AxiomResearchCondition.CODEC);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        PhoenixNetwork.register(registrar);
        ConfluxNetwork.register(registrar);
        VaultNetwork.register(registrar);
        GregPacksNetwork.register(registrar);
    }

    public void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            VaultRegistry.registerLinkables();
            net.phoenix.core.integration.ae2.CinderAtlasWirelessLink.register();

            MapIngredientTypeManager.registerMapIngredient(Shield.ShieldTypes.class, MapShieldIngredient::from);
            MapIngredientTypeManager.registerMapIngredient(
                    SourceIngredient.class,
                    MapSourceIngredient::convertToMapIngredient);
        });
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {}

    private void addCreative(BuildCreativeModeTabContentsEvent event) {}

    private void registerMaterialIconSets() {
        PhoenixMaterialSet.init();
    }

    private void addMaterials() {
        PhoenixOres.register();
        PhoenixMaterials.register();
        PhoenixProgressionMaterials.register();
        PhoenixPolymerMaterials.register();
        PhoenixBeeMaterials.register();
        PhoenixFissionMaterials.register();
        AstralMaterials.register();
        PhoenixMaterialFlags.init();
    }

    private void modifyMaterials(PostMaterialEvent event) {
        PhoenixMaterials.modifyMaterials();
    }

    private void registerRecipeTypes() {
        PhoenixRecipeTypes.init();
    }

    public void registerSounds() {
        PhoenixSounds.init();
    }

    private void registerWorldGenLayers() {
        net.phoenix.core.integration.conflux.dimension.worldgen.ConfluxWorldGenLayers.init();
    }

    private void registerMachines() {
        PhoenixMachines.init();
        PhoenixBeeMachines.init();
        PhoenixTeslaMachines.init();
        ConfluxProducerMachines.init();
        ConfluxMultiblockRegistry.init();
        GrowthMachines.init();
        AstralMachines.init();
        VaultRegistry.initMachines();
        VaultMachineDefinition.init();
        net.phoenix.core.common.machine.multiblock.cinder.CinderForgeMachines.init();
        net.phoenix.core.common.machine.multiblock.ward.SanctumWardMachines.init();
        net.phoenix.core.integration.continuum.machine.ContinuumMachines.init();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static Fluid plasma(Material material) {
        return material.getFluid(FluidStorageKeys.PLASMA, 1).getFluid();
    }
}
