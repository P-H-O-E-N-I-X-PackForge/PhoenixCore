package net.phoenix.core.integration.gregvaults.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.gregvaults.client.screen.AbstractVaultMenu;
import net.phoenix.core.integration.gregvaults.common.multiblock.VaultMachine;

public class CPacketFillCraftingGrid implements CustomPacketPayload {

    public static final Type<CPacketFillCraftingGrid> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c_packet_fill_crafting_grid"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CPacketFillCraftingGrid> STREAM_CODEC = StreamCodec
            .of((buf, msg) -> CPacketFillCraftingGrid.encode(msg, buf), CPacketFillCraftingGrid::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final ResourceLocation recipeId;
    private final ItemStack[] template;

    public CPacketFillCraftingGrid(ResourceLocation recipeId, ItemStack[] template) {
        this.recipeId = recipeId;
        this.template = template;
    }

    public static void encode(CPacketFillCraftingGrid packet, RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(packet.recipeId != null);
        if (packet.recipeId != null) buf.writeResourceLocation(packet.recipeId);
        for (int i = 0; i < 9; i++) ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, packet.template[i]);
    }

    public static CPacketFillCraftingGrid decode(RegistryFriendlyByteBuf buf) {
        ResourceLocation id = buf.readBoolean() ? buf.readResourceLocation() : null;
        ItemStack[] template = new ItemStack[9];
        for (int i = 0; i < 9; i++) template[i] = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        return new CPacketFillCraftingGrid(id, template);
    }

    public static void handle(CPacketFillCraftingGrid packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;
            if (!(player.containerMenu instanceof AbstractVaultMenu menu)) return;
            if (!(menu.vaultHandler instanceof ItemStackHandler vaultHandler)) return;

            Ingredient[] grid = resolveGrid(packet, player);
            fillCraftingGrid(menu, vaultHandler, grid, player);
        });
    }

    private static Ingredient[] resolveGrid(CPacketFillCraftingGrid packet, ServerPlayer player) {
        Ingredient[] grid = new Ingredient[9];
        for (int i = 0; i < 9; i++) grid[i] = Ingredient.EMPTY;

        if (packet.recipeId != null) {
            var recipeOpt = player.level().getRecipeManager().byKey(packet.recipeId);
            if (recipeOpt.isPresent()) {
                Recipe<?> recipe = recipeOpt.get().value();
                var ingredients = recipe.getIngredients();

                if (recipe instanceof ShapedRecipe shaped) {
                    int w = shaped.getWidth();
                    for (int row = 0; row < shaped.getHeight(); row++) {
                        for (int col = 0; col < w; col++) {
                            int src = row * w + col;
                            int dst = row * 3 + col;
                            if (src < ingredients.size()) grid[dst] = ingredients.get(src);
                        }
                    }
                } else {
                    for (int i = 0; i < Math.min(ingredients.size(), 9); i++) {
                        grid[i] = ingredients.get(i);
                    }
                }
                return grid;
            }
        }

        for (int i = 0; i < 9; i++) {
            if (!packet.template[i].isEmpty()) {
                grid[i] = Ingredient.of(packet.template[i]);
            }
        }
        return grid;
    }

    private static void fillCraftingGrid(AbstractVaultMenu menu, ItemStackHandler vaultHandler,
                                         Ingredient[] grid, ServerPlayer player) {
        if (menu.machine != null)
            menu.machine.beginBatch(VaultMachine.BatchSyncMode.DELTA_ONLY);
        menu.beginCraftingGridBulkUpdate();
        try {
            for (int g = 0; g < menu.craftingGrid.getContainerSize(); g++) {
                ItemStack existing = menu.craftingGrid.getItem(g);
                if (existing.isEmpty()) continue;
                ItemStack remaining = tryInsertVault(vaultHandler, existing.copy());
                if (!remaining.isEmpty()) {
                    if (!player.getInventory().add(remaining)) {
                        player.drop(remaining, false);
                    }
                }
                menu.craftingGrid.setItem(g, ItemStack.EMPTY);
            }

            for (int g = 0; g < 9; g++) {
                Ingredient ing = grid[g];
                if (ing == null || ing.isEmpty()) continue;

                ItemStack pulled = tryExtractVault(vaultHandler, ing);
                if (pulled.isEmpty()) pulled = tryExtractPlayerInv(player, ing);
                if (!pulled.isEmpty()) {
                    menu.craftingGrid.setItem(g, pulled);
                }
            }
        } finally {
            menu.endCraftingGridBulkUpdate();
            if (menu.machine != null) menu.machine.endBatch();
        }
    }

    private static ItemStack tryExtractVault(ItemStackHandler handler, Ingredient ingredient) {
        for (int v = 0; v < handler.getSlots(); v++) {
            ItemStack s = handler.getStackInSlot(v);
            if (s.isEmpty() || !ingredient.test(s)) continue;
            ItemStack extracted = handler.extractItem(v, 1, false);
            if (!extracted.isEmpty()) return extracted;
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack tryInsertVault(ItemStackHandler handler, ItemStack stack) {
        for (int v = 0; v < handler.getSlots() && !stack.isEmpty(); v++) {
            stack = handler.insertItem(v, stack, false);
        }
        return stack;
    }

    private static ItemStack tryExtractPlayerInv(ServerPlayer player, Ingredient ingredient) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty() || !ingredient.test(s)) continue;
            ItemStack extracted = s.copyWithCount(1);
            s.shrink(1);
            if (s.isEmpty()) inv.setItem(i, ItemStack.EMPTY);
            return extracted;
        }
        return ItemStack.EMPTY;
    }
}
