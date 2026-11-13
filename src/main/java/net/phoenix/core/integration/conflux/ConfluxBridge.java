package net.phoenix.core.integration.conflux;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;

import net.phoenix.conflux.api.ConfluxHooks;
import net.phoenix.core.common.block.PhoenixBlocks;
import net.phoenix.core.common.data.materials.PhoenixMaterialFlags;
import net.phoenix.core.integration.chronicles.ChroniclesLink;
import net.phoenix.core.integration.solaris.TeamSiteData;
import net.phoenix.core.rebirth.RebirthNetwork;

public final class ConfluxBridge {

    private ConfluxBridge() {}

    public static void init() {
        if (ChroniclesLink.present()) {
            ConfluxHooks.sink = ChroniclesLink::fire;
        }
        ConfluxHooks.giftSupplier = () -> new net.minecraft.world.item.ItemStack(
                net.phoenix.core.rebirth.RebirthRegistry.ASHEN_REEL.get());
        ConfluxHooks.terminalListener = TeamSiteData::registerTerminal;
        ConfluxHooks.panopticDenialText = RebirthNetwork::denial;
        ConfluxHooks.roseResolver = material -> {
            var rose = ChemicalHelper.getBlock(PhoenixMaterialFlags.crystal_rose, material);
            if (rose == null || rose.defaultBlockState().isAir()) rose = PhoenixBlocks.CRYSTAL_ROSE.get();
            return rose;
        };
    }
}
