package net.phoenix.core.integration.gregvaults.common.multiblock;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;

import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import com.gregtechceu.gtceu.api.multiblock.Predicates;
import com.gregtechceu.gtceu.api.multiblock.pattern.MultiblockPatternBuilder;
import com.gregtechceu.gtceu.client.model.machine.overlays.WorkableOverlays;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.fml.DistExecutor;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.gregvaults.common.multiblock.VaultMachine.VaultTier;
import net.phoenix.core.integration.gregvaults.common.registry.VaultRegistry;

import static com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties.RECIPE_LOGIC_STATUS;
import static com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection.*;

import static com.gregtechceu.gtceu.common.data.models.GTMachineModels.CUBE_ALL_SIDED_OVERLAY_MODEL;
import static com.gregtechceu.gtceu.common.data.models.GTMachineModels.addWorkableOverlays;
import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

@SuppressWarnings("all")
public class VaultMachineDefinition {

    public static MultiblockMachineDefinition BRONZE_VAULT;
    public static MultiblockMachineDefinition STEEL_VAULT;
    public static MultiblockMachineDefinition TITANIUM_VAULT;

    private static final ResourceLocation OVERLAY_DIR = PhoenixCore.id("block/multiblock");

    public static void init() {
        BRONZE_VAULT = registerVault(VaultTier.BRONZE);
        STEEL_VAULT = registerVault(VaultTier.STEEL);
        TITANIUM_VAULT = registerVault(VaultTier.TITANIUM);
    }

    private static Block[] getAllowedCores(VaultTier tier) {
        return switch (tier) {
            case BRONZE -> new Block[] {
                    VaultRegistry.VAULT_CORE_MK1.get()
            };
            case STEEL -> new Block[] {
                    VaultRegistry.VAULT_CORE_MK1.get(),
                    VaultRegistry.VAULT_CORE_MK2.get()
            };
            case TITANIUM -> new Block[] {
                    VaultRegistry.VAULT_CORE_MK1.get(),
                    VaultRegistry.VAULT_CORE_MK2.get(),
                    VaultRegistry.VAULT_CORE_MK3.get()
            };
        };
    }

    private static Component[] buildVaultTooltips(VaultTier tier) {
        int baseSlots = switch (tier) {
            case BRONZE -> PhoenixConfigs.INSTANCE.vaultValues.bronzeVault.bronzeBaseSlots;
            case STEEL -> PhoenixConfigs.INSTANCE.vaultValues.steelVault.steelBaseSlots;
            case TITANIUM -> PhoenixConfigs.INSTANCE.vaultValues.titaniumVault.titaniumBaseSlots;
        };
        int interfaceLimit = switch (tier) {
            case BRONZE -> PhoenixConfigs.INSTANCE.vaultValues.bronzeVault.bronzeInterfaceLimit;
            case STEEL -> PhoenixConfigs.INSTANCE.vaultValues.steelVault.steelInterfaceLimit;
            case TITANIUM -> PhoenixConfigs.INSTANCE.vaultValues.titaniumVault.titaniumInterfaceLimit;
        };
        boolean wireless = switch (tier) {
            case BRONZE -> PhoenixConfigs.INSTANCE.vaultValues.bronzeVault.bronzeWireless;
            case STEEL -> PhoenixConfigs.INSTANCE.vaultValues.steelVault.steelWireless;
            case TITANIUM -> PhoenixConfigs.INSTANCE.vaultValues.titaniumVault.titaniumWireless;
        };

        return new Component[] {
                Component.translatable("tooltip.gregtechvaults.base_slots", baseSlots),
                Component.translatable("tooltip.gregtechvaults.interface_limit", interfaceLimit),
                Component.translatable(wireless ? "tooltip.gregtechvaults.wireless_enabled" :
                        "tooltip.gregtechvaults.wireless_disabled_tooltip"),
        };
    }

    private static MultiblockMachineDefinition registerVault(VaultTier tier) {
        var casingBlock = switch (tier) {
            case BRONZE -> GTBlocks.CASING_BRONZE_BRICKS;
            case STEEL -> GTBlocks.CASING_STEEL_SOLID;
            case TITANIUM -> GTBlocks.CASING_TITANIUM_STABLE;
        };

        String name = switch (tier) {
            case BRONZE -> "large_bronze_vault";
            case STEEL -> "large_steel_vault";
            case TITANIUM -> "large_titanium_vault";
        };

        ResourceLocation casingTexture = switch (tier) {
            case BRONZE -> ResourceLocation.fromNamespaceAndPath("gtceu", "block/casings/solid/machine_casing_bronze_plated_bricks");
            case STEEL -> ResourceLocation.fromNamespaceAndPath("gtceu", "block/casings/solid/machine_casing_solid_steel");
            case TITANIUM -> ResourceLocation.fromNamespaceAndPath("gtceu", "block/casings/solid/machine_casing_stable_titanium");
        };

        return REGISTRATE.multiblock(name, holder -> new VaultMachine(holder, tier))
                .tooltips(buildVaultTooltips(tier))
                .rotationState(RotationState.ALL)
                .appearanceBlock(casingBlock)
                .modelProperty(RECIPE_LOGIC_STATUS, RecipeLogic.Status.IDLE)
                .model((ctx, prov, builder) -> {
                    WorkableOverlays overlays = WorkableOverlays.get(
                            OVERLAY_DIR,
                            prov.getExistingFileHelper());

                    builder.forAllStatesModels(state -> {
                        RecipeLogic.Status status = state.getValue(RECIPE_LOGIC_STATUS);

                        BlockModelBuilder casingModel = prov.models()
                                .nested()
                                .parent(prov.models().getExistingFile(CUBE_ALL_SIDED_OVERLAY_MODEL))
                                .texture("all", casingTexture);

                        return addWorkableOverlays(overlays, status, casingModel);
                    });

                    builder.addTextureOverride("all", casingTexture);
                    builder.addTextureOverride("side", casingTexture);
                    builder.addTextureOverride("top", casingTexture);
                    builder.addTextureOverride("bottom", casingTexture);

                    DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                            () -> () -> net.phoenix.core.integration.gregvaults.client.VaultClientModelHooks
                                    .addVaultOverlayRenderer(builder));
                })
                .pattern(definition -> MultiblockPatternBuilder.start(BACK, UP, RIGHT)
                        .slice("WWWWW", "WWWWW", "WWCWW", "WWWWW", "WWWWW")
                        .slice("WWWWW", "WVVVW", "WVVVW", "WVVVW", "WWWWW")
                        .slice("WWWWW", "WVVVW", "WVVVW", "WVVVW", "WWWWW")
                        .slice("WWWWW", "WVVVW", "WVVVW", "WVVVW", "WWWWW")
                        .slice("WWWWW", "WWWWW", "WWWWW", "WWWWW", "WWWWW")
                        .where('C', Predicates.controller(Predicates.blocks(definition.getBlock())))
                        .where('W', Predicates.blocks(casingBlock.get()).or(
                                Predicates.abilities(VaultRegistry.VAULT_INTERFACE_ABILITY)
                                        .setMaxGlobalLimited(switch (tier) {
                                            case BRONZE -> PhoenixConfigs.INSTANCE.vaultValues.bronzeVault.bronzeInterfaceLimit;
                                            case STEEL -> PhoenixConfigs.INSTANCE.vaultValues.steelVault.steelInterfaceLimit;
                                            case TITANIUM -> PhoenixConfigs.INSTANCE.vaultValues.titaniumVault.titaniumInterfaceLimit;
                                        })
                                        .setPreviewCount(1)))
                        .where('V', Predicates.blocks(getAllowedCores(tier)).or(Predicates.air()))
                        .build())
                .register();
    }
}
