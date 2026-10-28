package net.phoenix.core.integration.chronicles;

import net.neoforged.fml.loading.FMLPaths;
import net.phoenix.core.PhoenixCore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ChroniclesScaffold {

    private static final String ROOT = "/phoenixcore_questlines/";

    private ChroniclesScaffold() {}

    public static void install() {
        if (!ChroniclesLink.present()) return;
        Path quests = FMLPaths.CONFIGDIR.get().resolve("phoenix_chronicles").resolve("quests");
        Map<String, List<String>> chapters = readIndex();
        int installed = 0;
        for (Map.Entry<String, List<String>> chapter : chapters.entrySet()) {
            Path folder = quests.resolve(chapter.getKey());
            if (Files.exists(folder)) continue;
            try {
                Files.createDirectories(folder);
                for (String file : chapter.getValue()) copy(chapter.getKey() + "/" + file, folder.resolve(file));
                installed++;
            } catch (IOException e) {
                PhoenixCore.LOGGER.warn("[Chronicles] Could not install the starter chapter {}: {}", chapter.getKey(),
                        e.getMessage());
            }
        }
        if (installed > 0) PhoenixCore.LOGGER.info("[Chronicles] Installed {} starter chapter(s)", installed);
    }

    private static void copy(String resource, Path target) throws IOException {
        try (InputStream in = ChroniclesScaffold.class.getResourceAsStream(ROOT + resource)) {
            if (in == null) throw new IOException("missing " + resource);
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Map<String, List<String>> readIndex() {
        Map<String, List<String>> out = new LinkedHashMap<>();
        try (InputStream in = ChroniclesScaffold.class.getResourceAsStream(ROOT + "index.txt")) {
            if (in == null) return out;
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                int slash = line.indexOf('/');
                if (slash <= 0) continue;
                out.computeIfAbsent(line.substring(0, slash), k -> new java.util.ArrayList<>())
                        .add(line.substring(slash + 1));
            }
        } catch (IOException e) {
            PhoenixCore.LOGGER.warn("[Chronicles] Could not read the starter chapter index: {}", e.getMessage());
        }
        return out;
    }
}
