package net.phoenix.core.integration.gregvaults.datagen.model;

import com.gregtechceu.gtceu.api.registry.registrate.provider.GTBlockstateProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public class VaultModelProvider {

    public static void init(GTBlockstateProvider provider) {
        generateVaultInterfaceModel(provider);
    }

    private static void generateVaultInterfaceModel(GTBlockstateProvider provider) {
        provider.models()
                .withExistingParent(
                        "block/machine/part/vault_interface",
                        ResourceLocation.fromNamespaceAndPath("minecraft", "block/block"))
                .texture("particle", "#side")
                .element()
                .from(0, 0, 0)
                .to(16, 16, 16)

                .face(Direction.DOWN)
                .texture("#bottom")
                .cullface(Direction.DOWN)
                .tintindex(1)
                .end()

                .face(Direction.UP)
                .texture("#top")
                .cullface(Direction.UP)
                .tintindex(1)
                .end()

                .face(Direction.NORTH)
                .texture("#side")
                .cullface(Direction.NORTH)
                .tintindex(1)
                .end()

                .face(Direction.SOUTH)
                .texture("#side")
                .cullface(Direction.SOUTH)
                .tintindex(1)
                .end()

                .face(Direction.WEST)
                .texture("#side")
                .cullface(Direction.WEST)
                .tintindex(1)
                .end()

                .face(Direction.EAST)
                .texture("#side")
                .cullface(Direction.EAST)
                .tintindex(1)
                .end()

                .end();
    }
}
