package net.phoenix.core;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.event.PostMaterialEvent;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.api.recipe.condition.RecipeConditionType;
import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.MapIngredientTypeManager;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.phoenix.core.api.PhoenixSounds;
import net.phoenix.core.api.capability.PhoenixRecipeCapabilities;
import net.phoenix.core.api.recipe.lookup.MapShieldIngredient;
import net.phoenix.core.client.PhoenixClient;
import net.phoenix.core.client.keybind.PhoenixKeybinds;
import net.phoenix.core.client.particle.PhoenixParticles;
import net.phoenix.core.common.block.PhoenixBlocks;
import net.phoenix.core.common.block.cinder.CinderBlocks;
import net.phoenix.core.common.data.PhoenixCovers;
import net.phoenix.core.common.data.PhoenixRecipeTypes;
import net.phoenix.core.common.data.item.PhoenixItems;
import net.phoenix.core.common.data.materials.*;
import net.phoenix.core.common.data.recipeConditions.FluidInHatchCondition;
import net.phoenix.core.common.data.worldgen.CrystalRoseIndicatorGenerator;
import net.phoenix.core.common.data.worldgen.CrystalRoseVeinHook;
import net.phoenix.core.common.entity.PhoenixEntities;
import net.phoenix.core.common.item.PhoenixItemCaps;
import net.phoenix.core.common.item.cinder.CinderItems;
import net.phoenix.core.common.item.cinder.CinderRecipes;
import net.phoenix.core.common.machine.*;
import net.phoenix.core.common.machine.multiblock.Shield;
import net.phoenix.core.common.machine.multiblock.cinder.CinderForgeMachines;
import net.phoenix.core.common.machine.multiblock.ward.SanctumWardMachines;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.ae2.CinderAtlasWirelessLink;
import net.phoenix.core.integration.ars_nouveau.api.capability.ISourceProviderCapability;
import net.phoenix.core.integration.ars_nouveau.api.recipe.lookup.MapSourceIngredient;
import net.phoenix.core.integration.ars_nouveau.client.gui.SourceHatchMenu;
import net.phoenix.core.integration.ars_nouveau.common.data.recipe.custom.SourceIngredient;
import net.phoenix.core.integration.ars_nouveau.common.data.recipeConditons.SoulCondition;
import net.phoenix.core.integration.ars_nouveau.common.event.SourceHatchJarTransferTick;
import net.phoenix.core.integration.astral.AstralBlocks;
import net.phoenix.core.integration.astral.AstralItems;
import net.phoenix.core.integration.astral.AstralMachines;
import net.phoenix.core.integration.drone.DroneItems;
import net.phoenix.core.integration.gregpacks.client.OmniPackCurioRenderer;
import net.phoenix.core.integration.gregpacks.common.fluid.GregPacksCapabilityHandler;
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
import net.phoenix.core.integration.growth.GrowthItems;
import net.phoenix.core.integration.growth.GrowthMachines;
import net.phoenix.core.integration.recipe_helper.RecipeBuilderMenu;
import net.phoenix.core.network.PhoenixNetwork;

import com.tterrag.registrate.util.entry.RegistryEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

@SuppressWarnings("all")
@Mod(PhoenixCore.MOD_ID)
public class PhoenixCore {

    public static final String MOD_ID = "phoenixcore";
    public static final Logger LOGGER = LogManager.getLogger();
    public static GTRegistrate PHOENIX_REGISTRATE = GTRegistrate.create(MOD_ID);

    public static RegistryEntry<CreativeModeTab, CreativeModeTab> PHOENIX_CREATIVE_TAB = REGISTRATE
            .defaultCreativeTab(PhoenixCore.MOD_ID,
                    builder -> builder
                            .title(REGISTRATE.addLang("itemGroup", PhoenixCore.id("creative_tab"),
                                    "PhoenixCore (CoreMod)"))
                            .icon(() -> PhoenixMachines.HIGH_YIELD_PHOTON_EMISSION_REGULATOR.asStack())
                            .build())
            .register();

    public PhoenixCore(IEventBus modEventBus, ModContainer container) {
        PhoenixConfigs.init();
        CrystalRoseIndicatorGenerator.register(modEventBus);
        net.phoenix.core.api.pattern.PhoenixPatternErrors.register(modEventBus);
        VaultDatagen.init();
        GregPacksDatagen.init();
        GregPacksUpgrades.init();

        PHOENIX_REGISTRATE.registerEventListeners(modEventBus);
        net.phoenix.core.integration.continuum.ContinuumBridge.init();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerPayloads);

        PhoenixParticles.init(modEventBus);
        PhoenixEntities.init(modEventBus);
        if (FMLEnvironment.dist.isClient()) {
            modEventBus.addListener(PhoenixKeybinds::register);
            PhoenixClient.init(modEventBus);
            modEventBus.addListener(this::clientSetup);
            modEventBus.addListener(this::registerMenuScreens);
            modEventBus.addListener(OmniPackKeybind::register);
            NeoForge.EVENT_BUS.register(OmniPackKeybind.class);
        }

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::modifyMaterials);

        PhoenixElements.init();
        PhoenixRecipeCapabilities.init();
        PhoenixCovers.init();
        registerMaterialIconSets();
        addMaterials();
        MaterialLang.register();
        registerSounds();
        registerConditions();
        registerRecipeTypes();
        onRegisterBlocksAndItems();
        registerMachines();

        VaultRegistry.registerEventBus(modEventBus);
        VaultRecipes.SERIALIZERS.register(modEventBus);
        CinderRecipes.SERIALIZERS.register(modEventBus);

        GregPacksItems.init();
        GregPacksBlocks.init();
        GregPacksMenus.MENUS.register(modEventBus);
        GregPacksBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        modEventBus.register(GregPacksDataGenerators.class);

        MENUS.register(modEventBus);

        net.phoenix.core.integration.ars_nouveau.logistics.SourceLogistics.register(modEventBus);
        modEventBus
                .addListener(net.phoenix.core.integration.ars_nouveau.logistics.SourceLogistics::registerCapabilities);
        modEventBus
                .addListener(GregPacksCapabilityHandler::register);
        modEventBus.addListener(
                ISourceProviderCapability::register);
        AstralBlocks.registerDeferred(modEventBus);
        PhoenixItemCaps.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(SourceHatchJarTransferTick.class);
        NeoForge.EVENT_BUS.addListener(CrystalRoseVeinHook::onAddReloadListeners);

        GregPacksBlockEntities.init();
    }

    private void onRegisterBlocksAndItems() {
        PhoenixBlocks.init();
        net.phoenix.core.common.block.PrismariteBlocks.init();
        GrowthBlocks.init();
        GrowthItems.init();
        AstralBlocks.init();
        AstralItems.init();
        PhoenixItems.init();
        net.phoenix.core.common.data.item.PftItems.init();
        net.phoenix.core.common.data.item.PftCircuits.init();
        DroneItems.init();
        CinderItems.init();
        CinderBlocks.init();
        CinderRecipes.init();
    }

    private void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(GregPacksMenus.OMNIPACK_MENU.get(), OmniPackScreen::new);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        registerCurioRenderers();
        event.enqueueWork(net.phoenix.core.integration.continuum.ContinuumBridge::initClient);
    }

    private void registerCurioRenderers() {
        if (!ModList.get().isLoaded("curios")) return;
        CuriosRendererRegistry.register(
                GregPacksBlocks.BASIC_OMNIPACK_BLOCK.asItem(), () -> OmniPackCurioRenderer.INSTANCE);
        CuriosRendererRegistry.register(
                GregPacksBlocks.ADVANCED_OMNIPACK_BLOCK.asItem(), () -> OmniPackCurioRenderer.INSTANCE);
        CuriosRendererRegistry.register(
                GregPacksBlocks.ELITE_OMNIPACK_BLOCK.asItem(), () -> OmniPackCurioRenderer.INSTANCE);
    }

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BuiltInRegistries.MENU,
            MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<SourceHatchMenu>> SOURCE_HATCH_MENU = MENUS.register(
            "source_hatch",
            () -> IMenuTypeExtension.create((IContainerFactory<SourceHatchMenu>) SourceHatchMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<RecipeBuilderMenu>> RECIPE_BUILDER_MENU = MENUS.register(
            "recipe_builder",
            () -> IMenuTypeExtension.create(
                    (windowId, inv, data) -> new RecipeBuilderMenu(windowId, inv)));

    public void registerConditions() {
        FluidInHatchCondition.TYPE = REGISTRATE.simple("plasma_temp_condition",
                GTRegistries.Keys.RECIPE_CONDITION,
                () -> new RecipeConditionType<>(FluidInHatchCondition::new, FluidInHatchCondition.CODEC));

        net.phoenix.core.common.data.recipeConditions.DisciplineCondition.TYPE = REGISTRATE.simple("discipline",
                GTRegistries.Keys.RECIPE_CONDITION,
                () -> new RecipeConditionType<>(net.phoenix.core.common.data.recipeConditions.DisciplineCondition::new,
                        net.phoenix.core.common.data.recipeConditions.DisciplineCondition.CODEC));

        SoulCondition.TYPE = REGISTRATE.simple("soul_resonance",
                GTRegistries.Keys.RECIPE_CONDITION,
                () -> new RecipeConditionType<>(SoulCondition::new, SoulCondition.CODEC));
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        PhoenixNetwork.register(registrar);
        net.phoenix.core.rebirth.RebirthRegistry.registerPackets(registrar);
        net.phoenix.core.common.machine.multiblock.electric.plasma.PlasmaArcFurnaceMachines.registerPackets(registrar);
        net.phoenix.core.common.machine.multiblock.electric.honey.HoneyNetwork.register(registrar);
        net.phoenix.core.common.machine.multiblock.nullwell.NullWellNetwork.register(registrar);
        net.phoenix.core.common.machine.multiblock.cortex.S2CCortexOpenPacket.register(registrar);
        net.phoenix.core.common.machine.multiblock.infuser.InfuserPackets.register(registrar);
        net.phoenix.core.common.machine.multiblock.collider.ColliderPackets.register(registrar);
        VaultNetwork.register(registrar);
        GregPacksNetwork.register(registrar);
    }

    public void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(net.phoenix.core.common.GtHooks::registerModifiers);
        event.enqueueWork(() -> {
            PhoenixRecipeTypes.postInit();
            net.phoenix.core.integration.conflux.ConfluxBridge.init();
            VaultRegistry.registerLinkables();
            CinderAtlasWirelessLink.register();
            net.phoenix.core.integration.chronicles.ChroniclesLink.setup();

            MapIngredientTypeManager.registerMapIngredient(Shield.ShieldTypes.class, MapShieldIngredient::from);
            MapIngredientTypeManager.registerMapIngredient(
                    SourceIngredient.class,
                    MapSourceIngredient::convertToMapIngredient);
        });
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {}

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(PHOENIX_CREATIVE_TAB.getKey())) {
            for (net.phoenix.core.integration.ars_nouveau.logistics.SourcePipeTier tier : net.phoenix.core.integration.ars_nouveau.logistics.SourcePipeTier
                    .values()) {
                event.accept(net.phoenix.core.integration.ars_nouveau.logistics.SourceLogistics.PIPES.get(tier).get());
            }
        }
    }

    private void registerMaterialIconSets() {
        PhoenixMaterialSet.init();
    }

    private void addMaterials() {
        PhoenixProgressionMaterials.register();
        PhoenixOres.register();
        PhoenixPolymerMaterials.register();
        PhoenixBeeMaterials.register();
        PhoenixFissionMaterials.register();
        AstralMaterials.register();
        PhoenixMaterialFlags.init();
        net.phoenix.core.common.data.materials.PftMaterials.register();
    }

    private void modifyMaterials(PostMaterialEvent event) {
        PhoenixMaterials.register(event);
        net.phoenix.core.common.data.materials.PftMaterials.modify();
        PhoenixMaterials.modifyMaterials();
        PhoenixMaterials.postInit();
    }

    private void registerRecipeTypes() {
        PhoenixRecipeTypes.init();
        net.phoenix.core.common.data.PftRecipeTypes.init();
    }

    public void registerSounds() {
        PhoenixSounds.init();
    }

    private void registerMachines() {
        PhoenixMachines.init();
        net.phoenix.core.common.machine.PftMachines.init();
        net.phoenix.core.common.machine.PftChargers.init();
        PhoenixBeeMachines.init();
        net.phoenix.core.integration.ars_nouveau.conflux.ConfluxArcaneMachines.init();
        net.phoenix.core.integration.ars_nouveau.conflux.ConfluxProducerMachines.init();
        GrowthMachines.init();
        AstralMachines.init();
        net.phoenix.core.integration.ars_nouveau.logistics.SourceLogistics.init();
        VaultRegistry.initMachines();
        VaultMachineDefinition.init();
        CinderForgeMachines.init();
        net.phoenix.core.common.machine.multiblock.SubstructureTestMachines.init();
        net.phoenix.core.common.machine.multiblock.stellar.StellarForgeMachines.init();
        net.phoenix.core.common.machine.multiblock.nullwell.NullWellMachines.init();
        net.phoenix.core.common.machine.multiblock.cortex.SculkCortexMachines.init();
        net.phoenix.core.common.machine.multiblock.infuser.PhoenixInfuserMachines.init();
        net.phoenix.core.common.machine.multiblock.collider.AntimatterColliderMachines.init();
        net.phoenix.core.common.machine.multiblock.forge.PhoenixForgeMachines.init();
        net.phoenix.core.common.machine.multiblock.electric.AetherCrucibleMachines.init();
        net.phoenix.core.common.machine.PhoenixAEMachines.init();
        net.phoenix.core.common.machine.multiblock.electric.plasma.PlasmaArcFurnaceMachines.init();
        net.phoenix.core.rebirth.RebirthRegistry.init();
        SanctumWardMachines.init();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static Fluid plasma(Material material) {
        return material.getFluid(FluidStorageKeys.PLASMA, 1).getFluid();
    }
}
