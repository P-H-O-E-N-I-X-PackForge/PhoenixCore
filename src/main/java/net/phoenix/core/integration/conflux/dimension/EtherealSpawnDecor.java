package net.phoenix.core.integration.conflux.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.block.cinema.CinemaBlocks;
import net.phoenix.core.common.block.cinema.CinemaScreenBlock;
import net.phoenix.core.common.block.cinema.CinemaScreenBlockEntity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The cinema screens of the ethereal spawn that explain the choice of discipline to a new player. What they say and where
 * they stand is in {@code data/phoenixcore/ethereal_spawn/screens.json} (a data pack can replace the file); this builds
 * them from it. They are built when the dimension first loads and left alone after that: change the file, then run
 * {@code /conflux dimension spawndecor} to clear the old screens and build the new ones. See docs/cinema-screens.md for
 * the page text (titles, buttons, conditions).
 */
public final class EtherealSpawnDecor {

    public static final ResourceLocation FILE = ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID,
            "ethereal_spawn/screens.json");

    /** Where {@link #clear} looks for screens to remove: around the platform. */
    private static final int CLEAR_RADIUS = 24;
    private static final int CLEAR_MIN_Y = 62;
    private static final int CLEAR_MAX_Y = 80;

    private EtherealSpawnDecor() {}

    private record Screen(BlockPos pos, Direction facing, int cols, int rows, int color,
                          CinemaScreenBlockEntity.Background background, List<String> pages) {}

    /** Builds the screens unless every one of them is already there with its pages. */
    public static void ensure(ServerLevel level) {
        List<Screen> screens = load(level);
        if (screens.isEmpty()) return;
        var block = CinemaBlocks.CINEMA_SCREEN.get();
        for (Screen s : screens) {
            if (level.getBlockState(s.pos()).getBlock() != block ||
                    !(level.getBlockEntity(s.pos()) instanceof CinemaScreenBlockEntity be) || !be.isLocalPages() ||
                    be.getLines().size() != s.pages().size()) {
                build(level);
                return;
            }
        }
    }

    /** Removes the old screens and builds the ones the file describes. */
    public static void build(ServerLevel level) {
        List<Screen> screens = load(level);
        if (screens.isEmpty()) {
            PhoenixCore.LOGGER.warn("[Conflux] No screens in {}: nothing built", FILE);
            return;
        }
        clear(level);
        for (Screen s : screens) place(level, s);
        PhoenixCore.LOGGER.info("[Conflux] Built {} screen(s) in the ethereal spawn", screens.size());
    }

    /** Removes every cinema screen on and around the platform. */
    public static int clear(ServerLevel level) {
        var block = CinemaBlocks.CINEMA_SCREEN.get();
        int removed = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -CLEAR_RADIUS; x <= CLEAR_RADIUS; x++) {
            for (int z = -CLEAR_RADIUS; z <= CLEAR_RADIUS; z++) {
                for (int y = CLEAR_MIN_Y; y <= CLEAR_MAX_Y; y++) {
                    pos.set(x, y, z);
                    if (level.getBlockState(pos).getBlock() == block) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        removed++;
                    }
                }
            }
        }
        return removed;
    }

    private static List<Screen> load(ServerLevel level) {
        List<Screen> out = new ArrayList<>();
        try (Reader reader = level.getServer().getResourceManager().openAsReader(FILE)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray array = root.getAsJsonArray("screens");
            for (JsonElement element : array) {
                try {
                    out.add(parse(element.getAsJsonObject()));
                } catch (RuntimeException e) {
                    PhoenixCore.LOGGER.warn("[Conflux] A screen in {} is not valid: {}", FILE, e.getMessage());
                }
            }
        } catch (IOException | RuntimeException e) {
            PhoenixCore.LOGGER.warn("[Conflux] Could not read {}: {}", FILE, e.getMessage());
        }
        return out;
    }

    private static Screen parse(JsonObject json) {
        JsonArray p = json.getAsJsonArray("pos");
        BlockPos pos = new BlockPos(p.get(0).getAsInt(), p.get(1).getAsInt(), p.get(2).getAsInt());
        Direction facing = Direction.byName(json.get("facing").getAsString().toLowerCase(Locale.ROOT));
        if (facing == null || !facing.getAxis().isHorizontal()) {
            throw new IllegalArgumentException("facing is north, south, east or west");
        }
        int cols = json.has("cols") ? json.get("cols").getAsInt() : 5;
        int rows = json.has("rows") ? json.get("rows").getAsInt() : 3;
        String hex = json.has("color") ? json.get("color").getAsString() : "#FFFFFF";
        int color = 0xFF000000 | (int) Long.parseLong(hex.startsWith("#") ? hex.substring(1) : hex, 16);
        CinemaScreenBlockEntity.Background background = json.has("background") ? CinemaScreenBlockEntity.Background
                .valueOf(json.get("background").getAsString().toUpperCase(Locale.ROOT)) :
                CinemaScreenBlockEntity.Background.NEBULA;
        List<String> pages = new ArrayList<>();
        for (JsonElement page : json.getAsJsonArray("pages")) pages.add(page.getAsString());
        return new Screen(pos, facing, Math.max(1, cols), Math.max(1, rows), color, background, pages);
    }

    /** A screen of cols x rows blocks around its position, the text on its middle block. */
    private static void place(ServerLevel level, Screen s) {
        BlockState state = CinemaBlocks.CINEMA_SCREEN.get().defaultBlockState().setValue(CinemaScreenBlock.FACING,
                s.facing());
        Direction right = s.facing().getCounterClockWise();
        int halfCols = (s.cols() - 1) / 2;
        int halfRows = (s.rows() - 1) / 2;
        List<Component> pages = s.pages().stream().map(page -> (Component) Component.literal(page)).toList();
        for (int col = -halfCols; col <= halfCols; col++) {
            for (int row = -halfRows; row <= halfRows; row++) {
                BlockPos pos = s.pos().relative(right, col).relative(Direction.UP, row);
                level.setBlock(pos, state, 3);
                if (!(level.getBlockEntity(pos) instanceof CinemaScreenBlockEntity be)) continue;
                be.applyConfig(pages, s.color(), 0.015f, CinemaScreenBlockEntity.TextAlign.CENTER.ordinal(),
                        s.background().ordinal());
                be.setAction("");
                be.setLocalPages(true);
            }
        }
    }
}
