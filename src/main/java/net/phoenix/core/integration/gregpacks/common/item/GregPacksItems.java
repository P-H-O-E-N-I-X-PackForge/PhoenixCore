package net.phoenix.core.integration.gregpacks.common.item;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import com.tterrag.registrate.util.entry.ItemEntry;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

@SuppressWarnings("all")
public class GregPacksItems {

    public static final ItemEntry<GPItem> MODULE_EXTRUDER_MOLD = REGISTRATE
            .item("module_extruder_mold", GPItem::new)
            .lang("Module Base Extruder Mold")
            .register();
    public static final ItemEntry<GPItem> MODULE_CASTING_MOLD = REGISTRATE
            .item("module_casting_mold", GPItem::new)
            .lang("Module Base Casting Mold")
            .register();

    public static final ItemEntry<GPItem> LV_MODULE_BASE = REGISTRATE
            .item("lv_module_base", GPItem::new)
            .lang("LV Module Base")
            .register();
    public static final ItemEntry<GPItem> MV_MODULE_BASE = REGISTRATE
            .item("mv_module_base", GPItem::new)
            .lang("MV Module Base")
            .register();
    public static final ItemEntry<GPItem> HV_MODULE_BASE = REGISTRATE
            .item("hv_module_base", GPItem::new)
            .lang("HV Module Base")
            .register();
    public static final ItemEntry<GPItem> EV_MODULE_BASE = REGISTRATE
            .item("ev_module_base", GPItem::new)
            .lang("EV Module Base")
            .register();
    public static final ItemEntry<GPItem> IV_MODULE_BASE = REGISTRATE
            .item("iv_module_base", GPItem::new)
            .lang("IV Module Base")
            .register();
    public static final ItemEntry<GPItem> LUV_MODULE_BASE = REGISTRATE
            .item("luv_module_base", GPItem::new)
            .lang("LuV Module Base")
            .register();

    public static class GPItem extends Item {

        public GPItem(Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
            super.appendHoverText(stack, level, tooltip, flag);
            String key = stack.getItem().getDescriptionId() + ".tooltip";
            if (I18n.exists(key)) {
                tooltip.add(Component.translatable(key));
            }
        }
    }

    public static void init() {}
}
