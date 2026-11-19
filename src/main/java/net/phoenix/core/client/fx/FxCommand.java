package net.phoenix.core.client.fx;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.phoenix.core.PhoenixCore;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID, value = Dist.CLIENT)
public final class FxCommand {

    private FxCommand() {}

    @SubscribeEvent
    public static void onRegister(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("pftfx")
                .then(Commands.literal("list").executes(FxCommand::list))
                .then(Commands.literal("clear").executes(c -> {
                    int n = FxPreview.clear();
                    return say(c, n == 0 ? "nothing was spawned" :
                            "took away " + n + " spawned machine" + (n == 1 ? "" : "s"));
                }))
                .then(Commands.literal("reset").executes(c -> {
                    FxRegistry.resetAll();
                    return say(c, "every render is back to what it does itself");
                }))
                .then(Commands.argument("render", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(FxRegistry.ids(), b))
                        .then(Commands.literal("status").executes(c -> status(c, render(c))))
                        .then(Commands.literal("reset").executes(c -> {
                            FxRender r = render(c);
                            if (r == null) return say(c, "no such render");
                            r.reset();
                            return say(c, r.id() + " is back to what it does itself");
                        }))
                        .then(Commands.literal("spawn")
                                .executes(c -> spawn(c, 60))
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 600))
                                        .executes(c -> spawn(c, IntegerArgumentType.getInteger(c, "seconds")))))
                        .then(Commands.literal("theme")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests((c, b) -> {
                                            List<String> ids = FxThemes.ids();
                                            ids.add("none");
                                            return SharedSuggestionProvider.suggest(ids, b);
                                        })
                                        .executes(c -> theme(c, StringArgumentType.getString(c, "id")))))
                        .then(Commands.literal("colour")
                                .then(Commands.argument("hex", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("auto"), b))
                                        .executes(c -> colour(c, StringArgumentType.getString(c, "hex")))))
                        .then(Commands.argument("setting", StringArgumentType.word())
                                .suggests((c, b) -> {
                                    FxRender r = render(c);
                                    List<String> names = new ArrayList<>();
                                    if (r != null) for (var p : r.params()) names.add(p.name());
                                    return SharedSuggestionProvider.suggest(names, b);
                                })
                                .then(Commands.argument("value", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("auto"), b))
                                        .executes(c -> setting(c, StringArgumentType.getString(c, "setting"),
                                                StringArgumentType.getString(c, "value")))))));
    }

    private static FxRender render(CommandContext<CommandSourceStack> c) {
        return FxRegistry.get(StringArgumentType.getString(c, "render"));
    }

    private static int list(CommandContext<CommandSourceStack> c) {
        var renders = FxRegistry.all();
        if (renders.isEmpty()) say(c, "no render takes part yet");
        for (FxRender r : renders) {
            StringBuilder names = new StringBuilder();
            for (var p : r.params()) names.append(p.name()).append(' ');
            say(c, r.id() + " (" + r.title() + ")" + (r.machineId() != null ? " [spawn]" : "") + ": " +
                    names.toString().trim());
        }
        say(c, "themes: " + String.join(", ", FxThemes.ids()));
        return 1;
    }

    private static int spawn(CommandContext<CommandSourceStack> c, int seconds) {
        FxRender r = render(c);
        if (r == null) return say(c, "no such render");
        return say(c, FxPreview.spawn(r, seconds));
    }

    private static int status(CommandContext<CommandSourceStack> c, FxRender r) {
        if (r == null) return say(c, "no such render");
        return say(c, r.id() + ": " + r.describe());
    }

    private static int theme(CommandContext<CommandSourceStack> c, String id) {
        FxRender r = render(c);
        if (r == null) return say(c, "no such render");
        if (id.equals("none")) {
            r.setTheme(null);
            return say(c, r.id() + ": no theme");
        }
        FxTheme theme = FxThemes.get(id);
        if (theme == null) return say(c, "no such theme: " + id + " (" + String.join(", ", FxThemes.ids()) + ")");
        r.setTheme(theme);
        return say(c, r.id() + ": theme " + theme.name());
    }

    private static int colour(CommandContext<CommandSourceStack> c, String hex) {
        FxRender r = render(c);
        if (r == null) return say(c, "no such render");
        if (hex.equals("auto")) {
            r.setColour(null);
            return say(c, r.id() + ": colour follows the render");
        }
        float[] rgb = FxTheme.parse(hex);
        if (rgb == null) return say(c, "not a colour like ff44aa: " + hex);
        r.setColour(rgb);
        return say(c, r.id() + ": colour " + hex);
    }

    private static int setting(CommandContext<CommandSourceStack> c, String name, String value) {
        FxRender r = render(c);
        if (r == null) return say(c, "no such render");
        FxRender.Param p = r.param(name);
        if (p == null) return say(c, r.id() + " has no setting " + name);
        if (value.equals("auto")) {
            r.set(name, null);
            return say(c, r.id() + ": " + name + " follows the render");
        }
        float v;
        try {
            v = Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return say(c, "not a number: " + value);
        }
        r.set(name, v);
        return say(c, String.format("%s: %s %.2f (%s to %s)", r.id(), name, r.override(name), p.min(), p.max()));
    }

    private static int say(CommandContext<CommandSourceStack> c, String text) {
        c.getSource().sendSuccess(() -> Component.literal("[pftfx] " + text), false);
        return 1;
    }
}
