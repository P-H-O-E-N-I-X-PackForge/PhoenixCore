package net.phoenix.core.integration.jade.provider;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.machine.multiblock.stellar.StellarForgeMachine;
import net.phoenix.core.common.machine.multiblock.stellar.StellarForgeState;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Locale;

/** What a Stellar Forge is making, and how steady it is, for the tooltip. */
public class StellarForgeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    public static final ResourceLocation UID = PhoenixCore.id("stellar_forge_info");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof StellarForgeMachine)) return;
        CompoundTag data = accessor.getServerData();
        if (!data.contains("formed")) return;
        if (!data.getBoolean("formed")) {
            tooltip.add(Component.literal("Not formed").withStyle(ChatFormatting.RED));
            return;
        }

        StellarForgeState.Stage stage = StellarForgeState.Stage.of(data.getInt("stage"));
        tooltip.add(Component.literal("Making: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(stage.label).withStyle(stageColor(stage))));
        if (stage == StellarForgeState.Stage.IDLE) {
            tooltip.add(Component.literal("Feed it hydrogen, helium or neutronium").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        tooltip.add(Component.literal(String.format(Locale.ROOT, "Mass %,.1f M   Heat %.0f%%", data.getDouble("mass"),
                data.getFloat("heat"))).withStyle(ChatFormatting.GRAY));
        float stability = data.getFloat("stability");
        tooltip.add(Component.literal(String.format(Locale.ROOT, "Stability %.0f%%", stability)).withStyle(
                stability >= 70.0f ? ChatFormatting.GREEN : stability >= 40.0f ? ChatFormatting.YELLOW :
                        ChatFormatting.RED));
        tooltip.add(Component.literal(String.format(Locale.ROOT, "Feed %d%%   Field %d%%   Spin %d%%",
                data.getInt("feed"), data.getInt("field"), data.getInt("spin"))).withStyle(ChatFormatting.GRAY));

        boolean powered = data.getBoolean("powered");
        tooltip.add(Component.literal(String.format(Locale.ROOT, "%,d EU/t", data.getInt("energy")))
                .withStyle(powered ? ChatFormatting.GRAY : ChatFormatting.RED)
                .append(Component.literal(powered ? "" : "  (short of power)").withStyle(ChatFormatting.RED)));
        float plasma = data.getFloat("plasma");
        if (plasma > 0.0f) {
            tooltip.add(Component.literal(String.format(Locale.ROOT, "Plasma %.1f mB/t", plasma))
                    .withStyle(ChatFormatting.AQUA));
        }
        if (data.getBoolean("armed")) {
            tooltip.add(Component.literal("Collapse armed").withStyle(ChatFormatting.GOLD));
        }
    }

    private static ChatFormatting stageColor(StellarForgeState.Stage stage) {
        return switch (stage) {
            case IDLE -> ChatFormatting.DARK_GRAY;
            case GAS_CLOUD -> ChatFormatting.AQUA;
            case PROTOSTAR -> ChatFormatting.GOLD;
            case STAR -> ChatFormatting.YELLOW;
            case BLACK_HOLE -> ChatFormatting.LIGHT_PURPLE;
        };
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof StellarForgeMachine machine)) return;
        tag.putBoolean("formed", machine.isFormed());
        if (!machine.isFormed()) return;
        tag.putInt("stage", machine.stage().ordinal());
        tag.putDouble("mass", machine.mass());
        tag.putFloat("heat", machine.heat());
        tag.putFloat("stability", machine.stability());
        tag.putInt("feed", machine.feedSetting());
        tag.putInt("field", machine.fieldSetting());
        tag.putInt("spin", machine.spinSetting());
        tag.putBoolean("powered", machine.powerOk());
        tag.putInt("energy", machine.energyUse());
        tag.putFloat("plasma", machine.plasmaPerTick());
        tag.putBoolean("armed", machine.armed());
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
