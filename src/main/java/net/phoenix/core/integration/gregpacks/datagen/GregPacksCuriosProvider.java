package net.phoenix.core.integration.gregpacks.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.phoenix.core.PhoenixCore;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class GregPacksCuriosProvider implements DataProvider {

    private final PackOutput output;

    public GregPacksCuriosProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput cache) {
        var basePath = output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve(PhoenixCore.MOD_ID);
        var curiosPath = output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("curios");

        JsonObject back = new JsonObject();
        back.addProperty("replace", false);
        back.addProperty("size", 1);
        back.addProperty("operation", "SET");
        back.addProperty("order", 300);
        back.addProperty("icon", "curios:slot/empty_back_slot");
        back.addProperty("add_cosmetic", false);
        back.addProperty("use_native_gui", true);
        back.addProperty("render_toggle", false);
        JsonArray validators = new JsonArray();
        validators.add("curios:tag");
        back.add("validators", validators);

        JsonObject backEntity = new JsonObject();
        JsonArray entities = new JsonArray();
        entities.add("player");
        JsonArray slots = new JsonArray();
        slots.add("back");
        backEntity.add("entities", entities);
        backEntity.add("slots", slots);
        JsonObject backTag = new JsonObject();
        backTag.addProperty("replace", false);
        JsonArray values = new JsonArray();
        values.add("phoenixcore:basic_omnipack");
        values.add("phoenixcore:advanced_omnipack");
        values.add("phoenixcore:elite_omnipack");
        backTag.add("values", values);

        return CompletableFuture.allOf(
                DataProvider.saveStable(cache, back, basePath.resolve("curios/slots/back.json")),
                DataProvider.saveStable(cache, backEntity, basePath.resolve("curios/entities/back.json")),
                DataProvider.saveStable(cache, backTag, curiosPath.resolve("tags/items/back.json")));
    }

    @Override
    public @NotNull String getName() {
        return "GregPacks Curios Slots";
    }
}
