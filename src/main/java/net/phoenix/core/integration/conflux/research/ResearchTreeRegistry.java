package net.phoenix.core.integration.conflux.research;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ResearchTreeRegistry extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final ResearchTreeRegistry INSTANCE = new ResearchTreeRegistry();

    private final Map<ResourceLocation, ResearchTree> trees = new LinkedHashMap<>();

    private final Set<ResourceLocation> gatedMultiblocks = new LinkedHashSet<>();

    private final Set<String> gatedRecipeTags = new LinkedHashSet<>();

    /** The source JSON of every loaded tree, kept so it can be sent to clients that never read the datapack. */
    private final Map<ResourceLocation, String> raw = new LinkedHashMap<>();

    private ResearchTreeRegistry() {
        super(GSON, "conflux/research");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager mgr, ProfilerFiller profiler) {
        trees.clear();
        raw.clear();

        data.forEach((id, element) -> {
            JsonObject json = element.getAsJsonObject();
            try {
                trees.put(id, ResearchTree.fromJson(id, json));
                raw.put(id, json.toString());
            } catch (Exception e) {
                throw new RuntimeException("Failed to load Axiom research tree: " + id, e);
            }
        });
        rebuildDerived();
    }

    private void rebuildDerived() {
        gatedMultiblocks.clear();
        gatedRecipeTags.clear();
        for (ResearchTree tree : trees.values()) {
            for (ResearchNode node : tree.getNodes()) {
                for (ResearchUnlock unlock : node.unlocks) {
                    switch (unlock.type()) {
                        case "multiblock" -> {
                            ResourceLocation id = ResourceLocation.tryParse(unlock.value());
                            if (id != null) gatedMultiblocks.add(id);
                        }
                        case "recipe_tag" -> gatedRecipeTags.add(unlock.value());
                    }
                }
            }
        }
    }

    public Map<ResourceLocation, String> rawJson() {
        return Collections.unmodifiableMap(raw);
    }

    /** Client side of the definition sync: a remote client rebuilds the registry from what the server sent. */
    public void clientReset() {
        trees.clear();
        raw.clear();
        rebuildDerived();
    }

    public void clientAdd(ResourceLocation id, String json) {
        try {
            trees.put(id, ResearchTree.fromJson(id, com.google.gson.JsonParser.parseString(json).getAsJsonObject()));
            raw.put(id, json);
            rebuildDerived();
        } catch (RuntimeException e) {
            net.phoenix.core.PhoenixCore.LOGGER.error("[Conflux] Could not read synced research tree {}", id, e);
        }
    }

    public Optional<ResearchTree> getTree(ResourceLocation id) {
        return Optional.ofNullable(trees.get(id));
    }

    public Collection<ResearchTree> getAllTrees() {
        return Collections.unmodifiableCollection(trees.values());
    }

    public Optional<ResearchNode> getNode(ResourceLocation nodeId) {
        for (ResearchTree tree : trees.values()) {
            Optional<ResearchNode> n = tree.getNode(nodeId);
            if (n.isPresent()) return n;
        }
        return Optional.empty();
    }

    public Collection<ResearchNode> getAllNodes() {
        return trees.values().stream()
                .flatMap(t -> t.getNodes().stream())
                .toList();
    }

    public Set<ResourceLocation> getGatedMultiblocks() {
        return Collections.unmodifiableSet(gatedMultiblocks);
    }

    public Set<String> getGatedRecipeTags() {
        return Collections.unmodifiableSet(gatedRecipeTags);
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }
}
