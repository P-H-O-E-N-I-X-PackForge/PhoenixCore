package net.phoenix.core.common.data.materials;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlag;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.registry.registrate.entry.TagPrefixEntry;

import net.phoenix.core.common.block.CrystalRoseBlock;

import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.GENERATE_DENSE;
import static net.phoenix.core.PhoenixCore.PHOENIX_REGISTRATE;

public class PhoenixMaterialFlags {

    public static final MaterialFlag GENERATE_NANITES = new MaterialFlag.Builder("generate_nanites")
            .requireFlags(GENERATE_DENSE)
            .requireProps(PropertyKey.DUST)
            .build();

    public static final MaterialFlag GENERATE_CRYSTAL_ROSE = new MaterialFlag.Builder("generate_crystal_rose")
            .requireProps(PropertyKey.DUST)
            .build();

    public static final MaterialFlag GENERATE_TIER_ONE_BEE = new MaterialFlag.Builder("generate_tier_one_bee")
            .requireProps(PropertyKey.DUST)
            .build();
    public static final MaterialFlag GENERATE_TIER_TWO_BEE = new MaterialFlag.Builder("generate_tier_two_bee")
            .requireProps(PropertyKey.DUST)
            .build();
    public static final MaterialFlag GENERATE_TIER_THREE_BEE = new MaterialFlag.Builder("generate_tier_three_bee")
            .requireProps(PropertyKey.DUST)
            .build();
    public static final MaterialFlag GENERATE_BEE_COMB = new MaterialFlag.Builder("generate_bee_comb")
            .requireProps(PropertyKey.DUST)
            .build();
    public static final MaterialFlag GENERATE_BEE_HONEYCOMB_BLOCK = new MaterialFlag.Builder(
            "generate_bee_honeycomb_block")
            .requireProps(PropertyKey.DUST)
            .build();

    public static final TagPrefixEntry nanites = PHOENIX_REGISTRATE.tagPrefix("nanites")
            .idPattern("%s_nanites")
            .defaultTagPath("nanites/%s")
            .unformattedTagPath("nanites")
            .lang("%s Nanites")
            .materialAmount(GTValues.M / 4)
            .unificationEnabled(true)
            .generateItem(true)
            .materialIconType(PhoenixMaterialSet.NANITES)
            .generationCondition(mat -> mat.hasFlag(PhoenixMaterialFlags.GENERATE_NANITES))
            .register();

    public static final TagPrefixEntry honeycomb_block = PHOENIX_REGISTRATE.tagPrefix("honeycomb_block")
            .idPattern("%s_honeycomb_block")
            .defaultTagPath("honeycomb_block/%s")
            .unformattedTagPath("honeycomb_block")
            .lang("%s HoneyComb Block")
            .materialAmount(GTValues.M / 4)
            .unificationEnabled(true)
            .generateBlock(true)
            .materialIconType(PhoenixMaterialSet.HONEYCOMB_BLOCK)
            .generationCondition(mat -> mat.hasFlag(PhoenixMaterialFlags.GENERATE_BEE_HONEYCOMB_BLOCK))
            .register();

    public static final TagPrefixEntry crystal_rose = PHOENIX_REGISTRATE.tagPrefix("crystal_rose")
            .idPattern("%s_crystal_rose")
            .defaultTagPath("crystal_roses/%s")
            .unformattedTagPath("crystal_rose")
            .lang("%s Crystal Rose")
            .materialAmount(GTValues.M / 4)
            .unificationEnabled(true)
            .generateBlock(true)
            .blockConstructor(CrystalRoseBlock::new)
            .blockProperties(new TagPrefix.BlockProperties(
                    () -> net.minecraft.client.renderer.RenderType::cutoutMipped,
                    p -> p.noCollission().noOcclusion().instabreak()))
            .materialIconType(PhoenixMaterialSet.CRYSTAL_ROSE)
            .generationCondition(mat -> mat.hasFlag(PhoenixMaterialFlags.GENERATE_CRYSTAL_ROSE))
            .register();

    public static final TagPrefixEntry tier_one_bee = PHOENIX_REGISTRATE.tagPrefix("tier_one_bee")
            .idPattern("%s_tier_one_bee")
            .defaultTagPath("tier_one_bee/%s")
            .unformattedTagPath("tier_one_bee")
            .lang("%s Lively Bee")
            .materialAmount(GTValues.M / 4)
            .unificationEnabled(true)
            .generateItem(true)
            .materialIconType(PhoenixMaterialSet.TIER_ONE_BEE)
            .generationCondition(mat -> mat.hasFlag(PhoenixMaterialFlags.GENERATE_TIER_ONE_BEE))
            .register();

    public static final TagPrefixEntry tier_two_bee = PHOENIX_REGISTRATE.tagPrefix("tier_two_bee")
            .idPattern("%s_tier_two_bee")
            .defaultTagPath("tier_two_bee/%s")
            .unformattedTagPath("tier_two_bee")
            .lang("%s Energetic Bee")
            .materialAmount(GTValues.M / 4)
            .unificationEnabled(true)
            .generateItem(true)
            .materialIconType(PhoenixMaterialSet.TIER_TWO_BEE)
            .generationCondition(mat -> mat.hasFlag(PhoenixMaterialFlags.GENERATE_TIER_TWO_BEE))
            .register();

    public static final TagPrefixEntry tier_three_bee = PHOENIX_REGISTRATE.tagPrefix("tier_three_bee")
            .idPattern("%s_tier_three_bee")
            .defaultTagPath("tier_three_bee/%s")
            .unformattedTagPath("tier_three_bee")
            .lang("%s Stronk Bee")
            .materialAmount(GTValues.M / 4)
            .unificationEnabled(true)
            .generateItem(true)
            .materialIconType(PhoenixMaterialSet.TIER_THREE_BEE)
            .generationCondition(mat -> mat.hasFlag(PhoenixMaterialFlags.GENERATE_TIER_THREE_BEE))
            .register();

    public static final TagPrefixEntry honeycomb = PHOENIX_REGISTRATE.tagPrefix("honeycomb")
            .idPattern("%s_honeycomb")
            .defaultTagPath("honeycombs/%s")
            .unformattedTagPath("bee_comb")
            .lang("%s Honeycomb")
            .materialAmount(GTValues.M / 4)
            .unificationEnabled(true)
            .generateItem(true)
            .materialIconType(PhoenixMaterialSet.BEE_COMB)
            .generationCondition(mat -> mat.hasFlag(GENERATE_BEE_COMB))
            .register();

    public static void init() {}
}
