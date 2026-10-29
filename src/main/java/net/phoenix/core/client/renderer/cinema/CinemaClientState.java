package net.phoenix.core.client.renderer.cinema;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.phoenix.core.common.block.cinema.CinemaPage;
import net.phoenix.core.common.block.cinema.CinemaScreenBlock;
import net.phoenix.core.common.block.cinema.CinemaScreenBlockEntity;

import java.util.HashMap;
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

    public static int pageCount(Target t) {
        return Math.max(1, t.centre().getLines().size());
    }

    public static int pageIndex(Target t) {
        int count = pageCount(t);
        int index = t.centre().isLocalPages() ? PAGES.getOrDefault(t.key(), 0) : t.centre().getCurrentLineIndex();
        return Math.floorMod(index, count);
    }

    public static void setPage(Target t, int index) {
        PAGES.put(t.key(), Math.floorMod(index, pageCount(t)));
    }

    public static CinemaPage pageOf(Target t) {
        var lines = t.centre().getLines();
        String raw = lines.isEmpty() ? "" : lines.get(pageIndex(t)).getString();
        return CinemaPage.parse(CinemaStatTokens.resolve(raw, Minecraft.getInstance().player));
    }

    /** The layout of the screen's current page. */
    public static CinemaLayout layout(Target t) {
        return CinemaLayout.of(Minecraft.getInstance().font, pageOf(t), t.centre().getTextScale(), t.width(), t.height(),
                pageIndex(t), pageCount(t), true);
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
    }
}
