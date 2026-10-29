package net.phoenix.core.client.renderer.cinema;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.phoenix.core.common.block.cinema.CinemaPage;
import net.phoenix.core.common.block.cinema.CinemaScreenBlock;
import net.phoenix.core.common.block.cinema.CinemaScreenBlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What each client knows about the cinema screens it is looking at: the page it has turned to on a screen whose pages
 * are its own (so one player turning the page does not turn it for everyone), and where on a screen a look or a click
 * lands.
 */
public final class CinemaClientState {

    private static final Map<BlockPos, Integer> PAGES = new HashMap<>();

    private CinemaClientState() {}

    /** The screen a block belongs to: the block that holds its text, the layout and the geometry. */
    public record Target(CinemaScreenBlockEntity centre, BlockPos key, Direction right, float width, float height) {

        public BlockPos pos() {
            return centre.getBlockPos();
        }
    }

    public static Target target(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof CinemaScreenBlockEntity clicked)) return null;
        var layout = CinemaGroupUtil.getLayout(level, pos);
        Direction facing = clicked.getBlockState().getValue(CinemaScreenBlock.FACING);
        Direction right = facing.getCounterClockWise();
        boolean solo = layout.width() == 1 && layout.height() == 1;
        CinemaScreenBlockEntity centre = clicked;
        BlockPos key = pos;
        if (!solo && layout.anchor() != null) {
            key = layout.anchor();
            BlockPos centrePos = layout.anchor().relative(right, (layout.width() - 1) / 2)
                    .relative(Direction.DOWN, (layout.height() - 1) / 2);
            if (level.getBlockEntity(centrePos) instanceof CinemaScreenBlockEntity found) centre = found;
        }
        float unit = solo ? 0.8f : 1.0f;
        return new Target(centre, key, right, layout.width() * unit, layout.height() * unit);
    }

    /** The lines of the screen the player sees as pages: all of them, or on a screen with its own pages those whose {@code @if} holds. */
    private static List<Integer> visibleLines(Target t) {
        var lines = t.centre().getLines();
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            if (!t.centre().isLocalPages() ||
                    CinemaConditions.test(CinemaPage.parse(lines.get(i).getString()).condition())) {
                out.add(i);
            }
        }
        if (out.isEmpty()) out.add(0);
        return out;
    }

    /**
     * Everything worked out about the screen's current page (its pages, the page, its layout): every block of a screen asks
     * for it every frame, and parsing and wrapping the text is far too much work to repeat that often, so it is kept for a
     * moment.
     */
    private record Cached(long at, CinemaScreenBlockEntity centre, int count, int index, CinemaPage page,
                          CinemaLayout layout) {}

    private static final Map<BlockPos, Cached> CACHE = new HashMap<>();
    private static final long KEEP_MS = 250L;

    private static Cached cached(Target t) {
        long now = net.minecraft.Util.getMillis();
        Cached c = CACHE.get(t.key());
        if (c != null && c.centre() == t.centre() && now - c.at() < KEEP_MS) return c;
        int count = visibleLines(t).size();
        int index = rawIndex(t, count);
        CinemaPage page = rawPage(t, index);
        CinemaLayout layout = CinemaLayout.of(Minecraft.getInstance().font, page, t.centre().getTextScale(), t.width(),
                t.height(), index, count, true);
        c = new Cached(now, t.centre(), count, index, page, layout);
        CACHE.put(t.key(), c);
        return c;
    }

    public static int pageCount(Target t) {
        return cached(t).count();
    }

    /** Which of the visible pages is shown (counting from 0). */
    public static int pageIndex(Target t) {
        return cached(t).index();
    }

    private static int rawIndex(Target t, int count) {
        int index = t.centre().isLocalPages() ? PAGES.getOrDefault(t.key(), 0) : t.centre().getCurrentLineIndex();
        return Math.floorMod(index, count);
    }

    public static void setPage(Target t, int index) {
        PAGES.put(t.key(), Math.floorMod(index, pageCount(t)));
        CACHE.remove(t.key());
    }

    public static CinemaPage pageOf(Target t) {
        return cached(t).page();
    }

    private static CinemaPage rawPage(Target t, int index) {
        var lines = t.centre().getLines();
        int line = visibleLines(t).get(index);
        String raw = lines.isEmpty() || line >= lines.size() ? "" : lines.get(line).getString();
        return CinemaPage.parse(CinemaStatTokens.resolve(raw, Minecraft.getInstance().player));
    }

    /** The layout of the screen's current page. */
    public static CinemaLayout layout(Target t) {
        return cached(t).layout();
    }

    /** A point in the world on the screen, as blocks from its middle: x to the viewer's right, y up. */
    public static float[] local(Target t, Vec3 world) {
        Vec3 middle = Vec3.atCenterOf(t.pos());
        Vec3 d = world.subtract(middle);
        float x = (float) (d.x * t.right().getStepX() + d.z * t.right().getStepZ());
        return new float[] { x, (float) d.y };
    }

    public static void forget() {
        PAGES.clear();
        CACHE.clear();
    }
}
