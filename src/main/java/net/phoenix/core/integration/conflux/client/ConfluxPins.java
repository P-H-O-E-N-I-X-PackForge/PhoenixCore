package net.phoenix.core.integration.conflux.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.PhoenixCore;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/** Research nodes the player has starred. Purely a client convenience, stored next to the other client configs. */
@OnlyIn(Dist.CLIENT)
public final class ConfluxPins {

    private ConfluxPins() {}

    private static final Set<ResourceLocation> PINS = new LinkedHashSet<>();
    private static boolean loaded;

    private static Path file() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("phoenixcore_conflux_pins.json");
    }

    private static void load() {
        if (loaded) return;
        loaded = true;
        Path f = file();
        if (!Files.exists(f)) return;
        try {
            JsonArray arr = new Gson().fromJson(Files.readString(f, StandardCharsets.UTF_8), JsonArray.class);
            if (arr == null) return;
            for (JsonElement e : arr) {
                ResourceLocation id = ResourceLocation.tryParse(e.getAsString());
                if (id != null) PINS.add(id);
            }
        } catch (IOException | RuntimeException e) {
            PhoenixCore.LOGGER.warn("[Conflux] Could not read pinned research: {}", e.toString());
        }
    }

    private static void save() {
        JsonArray arr = new JsonArray();
        for (ResourceLocation id : PINS) arr.add(id.toString());
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), new Gson().toJson(arr), StandardCharsets.UTF_8);
        } catch (IOException e) {
            PhoenixCore.LOGGER.warn("[Conflux] Could not save pinned research: {}", e.toString());
        }
    }

    public static boolean isPinned(ResourceLocation id) {
        load();
        return PINS.contains(id);
    }

    public static void toggle(ResourceLocation id) {
        load();
        if (!PINS.remove(id)) PINS.add(id);
        save();
    }

    public static Set<ResourceLocation> all() {
        load();
        return PINS;
    }
}
