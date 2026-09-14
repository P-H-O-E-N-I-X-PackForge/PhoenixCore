package net.phoenix.core.integration.conflux.dimension;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = "phoenixcore")
public class DisciplineThemeRegistry {

    private static final Map<String, DisciplineTheme> THEMES = new HashMap<>();
    private static final Gson GSON = new Gson();

    static {
        loadDefaultThemes();
    }

    public static @Nullable DisciplineTheme getTheme(String disciplineId) {
        return THEMES.get(disciplineId);
    }

    public static DisciplineTheme getThemeOrThrow(String disciplineId) {
        DisciplineTheme theme = THEMES.get(disciplineId);
        if (theme == null) {
            throw new IllegalArgumentException("Unknown discipline theme: " + disciplineId);
        }
        return theme;
    }

    public static Collection<DisciplineTheme> getAllThemes() {
        return THEMES.values();
    }

    public static void registerTheme(DisciplineTheme theme) {
        THEMES.put(theme.disciplineId, theme);
    }

    private static void loadDefaultThemes() {}

    @SubscribeEvent
    public static void onResourceReload(AddReloadListenerEvent event) {
        event.addListener((stage, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor,
                           gameExecutor) -> CompletableFuture
                                   .runAsync(() -> loadThemesFromResources(resourceManager), backgroundExecutor)
                                   .thenCompose(stage::wait));
    }

    private static void loadThemesFromResources(ResourceManager resourceManager) {
        THEMES.clear();

        String[] disciplineIds = { "phoenix", "sculk", "void", "sealed_a", "sealed_b" };

        for (String disciplineId : disciplineIds) {
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath("phoenixcore",
                    "conflux/themes/" + disciplineId + ".json");

            var resources = resourceManager.getResourceStack(location);
            for (var resource : resources) {
                try (BufferedReader reader = new BufferedReader(
                        new java.io.InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    DisciplineTheme theme = DisciplineTheme.fromJson(json, disciplineId);
                    registerTheme(theme);
                } catch (Exception e) {
                    System.err.println("Failed to load theme for discipline: " + disciplineId);
                    e.printStackTrace();
                }
            }
        }
    }
}
