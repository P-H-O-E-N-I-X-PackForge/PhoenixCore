package net.phoenix.core.shop;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.shop.network.C2SAddShopEntryPacket;
import net.phoenix.core.shop.network.S2CShopSyncPacket;
import net.phoenix.core.shop.reward.ItemShopReward;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber
public class PhoenixShopCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("phoenixshop")
                        .executes(ctx -> run(ctx.getSource()))
                        .then(Commands.literal("seed")
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> seed(ctx.getSource()))));
    }

    private static int run(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();

        WorldShopData data = WorldShopData.get(level);
        PhoenixNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new S2CShopSyncPacket(data.getEntries()));
        return 1;
    }

    private static final Object[][] EXAMPLE_ENTRIES = {
            { "Iron Ingot Bundle", Items.IRON_INGOT, 5, "Materials" },
            { "Diamond", Items.DIAMOND, 40, "Materials" },
            { "Golden Apple", Items.GOLDEN_APPLE, 25, "Consumables" },
            { "Ender Pearl", Items.ENDER_PEARL, 12, "Materials" },
            { "Netherite Scrap", Items.NETHERITE_SCRAP, 90, "Materials" },
            { "Emerald", Items.EMERALD, 15, "Materials" },
            { "Blaze Rod", Items.BLAZE_ROD, 8, "Materials" },
            { "Totem of Undying", Items.TOTEM_OF_UNDYING, 150, "Consumables" },
            { "Enchanted Book", Items.ENCHANTED_BOOK, 60, "Gear" },
            { "Elytra", Items.ELYTRA, 300, "Gear" },
            { "TNT", Items.TNT, 3, "Misc" },
            { "Nether Star", Items.NETHER_STAR, 500, "Materials" },
    };

    private static int seed(CommandSourceStack source) throws CommandSyntaxException {
        ServerLevel level = source.getLevel();
        WorldShopData data = WorldShopData.get(level);

        for (Object[] example : EXAMPLE_ENTRIES) {
            String name = (String) example[0];
            ItemStack icon = new ItemStack((net.minecraft.world.item.Item) example[1]);
            int cost = (int) example[2];
            String category = (String) example[3];
            List<net.phoenix.core.shop.reward.ShopReward> rewards = new ArrayList<>();
            rewards.add(new ItemShopReward(icon.copy()));
            data.addEntry(ShopEntry.create(name, icon, cost, rewards, category));
        }

        C2SAddShopEntryPacket.broadcastSync(data);
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                "Added " + EXAMPLE_ENTRIES.length + " example shop entries."), true);
        return 1;
    }
}
