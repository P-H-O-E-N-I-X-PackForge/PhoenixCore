package net.phoenix.core.integration.gregvaults.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@OnlyIn(Dist.CLIENT)
public class VaultScreenState {

    public static class State {

        public VaultDisplayMode displayMode = VaultDisplayMode.SLOTS;
        public VaultSortMode sortMode = VaultSortMode.NAME;
        public boolean sortReversed = false;
        public String searchQuery = "";
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "gregtechvaults-client.json";

    private static State cached = null;

    public static State get() {
        if (cached == null) load();
        return cached;
    }

    public static void save(VaultDisplayMode displayMode, VaultSortMode sortMode,
                            boolean sortReversed, String searchQuery) {
        State state = get();
        state.displayMode = displayMode;
        state.sortMode = sortMode;
        state.sortReversed = sortReversed;
        state.searchQuery = searchQuery != null ? searchQuery : "";
        persist();
    }

    private static void load() {
        cached = new State();
        Path file = configPath();
        if (!Files.exists(file)) return;
        try {
            String json = Files.readString(file);
            State loaded = GSON.fromJson(json, State.class);
            if (loaded != null) cached = loaded;
        } catch (IOException ignored) {}
    }

    private static void persist() {
        try {
            Path file = configPath();
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(cached));
        } catch (IOException ignored) {}
    }

    private static Path configPath() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve(FILE_NAME);
    }
}
