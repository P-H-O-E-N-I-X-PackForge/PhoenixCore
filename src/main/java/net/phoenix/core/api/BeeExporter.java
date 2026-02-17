package net.phoenix.core.api;

import net.phoenix.core.common.data.bees.BeeRecipeData;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class BeeExporter {

    public static void main(String[] args) {
        try {
            exportBees();
            System.out.println("Bee export complete!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void exportBees() throws IOException {

        Path path = Paths.get("debug_exports", "bee_export.csv");
        Files.createDirectories(path.getParent());

        try (FileWriter writer = new FileWriter(path.toFile())) {

            writer.write("Bee ID,Display Name,Tier\n");

            BeeRecipeData.ALL_BEE_NAMES.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        String beeId = entry.getKey();
                        String displayName = entry.getValue();
                        int tier = BeeRecipeData.tierFor(beeId);


                        try {
                            writer.write(csv(beeId) + "," +
                                    csv(displayName) + "," +
                                    tier + "\n");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }
    private static String csv(String value) {
        if (value.contains(",")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }


}
