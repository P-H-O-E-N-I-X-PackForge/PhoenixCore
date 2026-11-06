package net.phoenix.core.integration.conflux.research;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.network.PacketDistributor;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.conflux.network.S2CResearchDataPacket;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class ResearchTreeEditStore {

    private ResearchTreeEditStore() {}

    private static Path root(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("conflux_edits");
    }

    public static String apply(MinecraftServer server, ResourceLocation id, String json) {
        JsonObject parsed;
        try {
            parsed = JsonParser.parseString(json).getAsJsonObject();
            ResearchTree.fromJson(id, parsed);
        } catch (RuntimeException e) {
            return "Not a valid research tree: " + e.getMessage();
        }
        try {
            Path file = root(server).resolve(id.getNamespace()).resolve(id.getPath() + ".json");
            Files.createDirectories(file.getParent());
            Files.writeString(file, json);
            Files.deleteIfExists(root(server).resolve(id.getNamespace()).resolve(id.getPath() + ".deleted"));

            Path parent = server.getServerDirectory().toAbsolutePath().getParent();
            if (parent != null) {
                Path project = parent.resolve("src/main/resources/data").resolve(id.getNamespace())
                        .resolve("conflux/research");
                if (Files.isDirectory(project)) {
                    Path target = project.resolve(id.getPath() + ".json");
                    Files.createDirectories(target.getParent());
                    Files.writeString(target, json);
                }
            }
        } catch (IOException e) {
            PhoenixCore.LOGGER.error("[Conflux] Could not save the edited tree {}", id, e);
            return "Could not save: " + e.getMessage();
        }

        ResearchTreeRegistry.INSTANCE.serverPut(id, json);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(player, new S2CResearchDataPacket(S2CResearchDataPacket.TREE, id, json));
        }
        return null;
    }

    public static String delete(MinecraftServer server, ResourceLocation id) {
        try {
            Path dir = root(server).resolve(id.getNamespace());
            Files.deleteIfExists(dir.resolve(id.getPath() + ".json"));
            Path marker = dir.resolve(id.getPath() + ".deleted");
            Files.createDirectories(marker.getParent());
            Files.writeString(marker, "");
            Path parent = server.getServerDirectory().toAbsolutePath().getParent();
            if (parent != null) {
                Path project = parent.resolve("src/main/resources/data").resolve(id.getNamespace())
                        .resolve("conflux/research").resolve(id.getPath() + ".json");
                Files.deleteIfExists(project);
            }
        } catch (IOException e) {
            PhoenixCore.LOGGER.error("[Conflux] Could not delete the tree {}", id, e);
            return "Could not delete: " + e.getMessage();
        }
        ResearchTreeRegistry.INSTANCE.serverRemove(id);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(player, new S2CResearchDataPacket(S2CResearchDataPacket.REMOVE, id, ""));
        }
        return null;
    }

    public static void overlay(MinecraftServer server, ResearchTreeRegistry registry) {
        Path root = root(server);
        if (!Files.isDirectory(root)) return;
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(p -> p.toString().endsWith(".json") || p.toString().endsWith(".deleted")).forEach(file -> {
                try {
                    Path relative = root.relativize(file);
                    String namespace = relative.getName(0).toString();
                    String path = relative.subpath(1, relative.getNameCount()).toString().replace('\\', '/');
                    boolean deleted = path.endsWith(".deleted");
                    path = path.substring(0, path.length() - (deleted ? ".deleted" : ".json").length());
                    ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, path);
                    if (deleted) registry.serverRemove(id);
                    else registry.serverPut(id, Files.readString(file));
                } catch (Exception e) {
                    PhoenixCore.LOGGER.error("[Conflux] Could not read the edited tree {}", file, e);
                }
            });
        } catch (IOException e) {
            PhoenixCore.LOGGER.error("[Conflux] Could not list the edited trees", e);
        }
    }
}
