package net.phoenix.core.client.tooltips;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.phoenix.core.PhoenixCore;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID, value = Dist.CLIENT)
public final class PftScriptTooltips {

    private static Map<ResourceLocation, List<String>> lookup;

    private PftScriptTooltips() {}

    private static synchronized Map<ResourceLocation, List<String>> lookup() {
        if (lookup != null) return lookup;
        Map<ResourceLocation, List<String>> map = new HashMap<>();
        try (Reader reader = new InputStreamReader(
                PftScriptTooltips.class.getResourceAsStream("/assets/phoenixcore/pft_tooltips.json"),
                StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject materials = root.getAsJsonObject("__materials");
            for (JsonElement form : root.getAsJsonArray("__forms")) {
                for (Map.Entry<String, JsonElement> material : materials.entrySet()) {
                    String path = form.getAsString().replace("${materialName}", material.getKey());
                    List<String> text = List.of(material.getValue().getAsString());
                    map.put(ResourceLocation.fromNamespaceAndPath("gtceu", path), text);
                    map.put(ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, path), text);
                }
            }
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                if (entry.getKey().startsWith("__")) continue;
                List<String> lines = entry.getValue().getAsJsonArray().asList().stream().map(JsonElement::getAsString)
                        .toList();
                map.put(ResourceLocation.parse(entry.getKey()), lines);
            }
        } catch (Exception e) {
            PhoenixCore.LOGGER.error("[PFT] could not read pft_tooltips.json", e);
        }
        lookup = map;
        return map;
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        List<String> lines = lookup().get(id);
        if (lines == null) return;
        for (String line : lines) {
            event.getToolTip().add(Component.literal(line));
        }
    }
}
