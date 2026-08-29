package net.phoenix.core.shop.reward;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.phoenix.core.shop.RewardSpec;

public class CommandShopReward implements ShopReward {

    public static final String TYPE = "command";

    private final String command;

    public CommandShopReward(String command) {
        this.command = command;
    }

    @Override
    public void grant(ServerPlayer player) {
        String resolved = command.replace("%player%", player.getGameProfile().getName());
        player.getServer().getCommands().performPrefixedCommand(
                player.getServer().createCommandSourceStack().withPermission(4).withSuppressedOutput(),
                resolved);
    }

    @Override
    public Component describe() {
        return Component.literal("Runs: " + command);
    }

    @Override
    public String typeId() {
        return TYPE;
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("command", command);
        return tag;
    }

    public static CommandShopReward load(CompoundTag tag) {
        return new CommandShopReward(tag.getString("command"));
    }

    @Override
    public RewardSpec toSpec() {
        return new RewardSpec(TYPE, command);
    }
}
