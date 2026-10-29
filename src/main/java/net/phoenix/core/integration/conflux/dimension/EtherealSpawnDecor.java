package net.phoenix.core.integration.conflux.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.client.renderer.cinema.CinemaClientActions;
import net.phoenix.core.common.block.cinema.CinemaBlocks;
import net.phoenix.core.common.block.cinema.CinemaScreenBlock;
import net.phoenix.core.common.block.cinema.CinemaScreenBlockEntity;

import java.util.List;

/**
 * The cinema screens of the ethereal spawn that explain the choice of discipline to a new player: a welcome, one for each
 * discipline (what it is good at, what it costs), and the choosing screen, which opens the picker when it is clicked.
 * Built when the dimension first loads and left alone after that (break the choosing screen, or run
 * {@code /conflux dimension spawndecor}, to build them again). Right-click a screen to turn its page.
 */
public final class EtherealSpawnDecor {

    private static final int Y = 66;
    private static final int WIDE = 5;
    private static final int HIGH = 3;

    private EtherealSpawnDecor() {}

    private static final BlockPos CHOOSE = new BlockPos(0, Y, 6);
    /** The last screen added: if it is there, so are the rest. */
    private static final BlockPos LAST = new BlockPos(12, Y, 7);

    /** Builds the screens unless the choosing screen is already there. */
    public static void ensure(ServerLevel level) {
        var screen = CinemaBlocks.CINEMA_SCREEN.get();
        if (level.getBlockState(CHOOSE).getBlock() == screen && level.getBlockState(LAST).getBlock() == screen) return;
        build(level);
    }

    /** Builds (or rebuilds) every screen. */
    public static void build(ServerLevel level) {
        // west: the way in
        screen(level, new BlockPos(-12, Y, 0), Direction.EAST, WIDE, HIGH, 0xFFFFFFFF,
                CinemaScreenBlockEntity.Background.VOID_GALAXY, "", List.of(
                        "Welcome to the Conflux.",
                        "This is a threshold. What you become next is a choice, and it is yours to make once.",
                        "Three disciplines are open to you. Each is a way of working, and each shapes the machines, "
                                + "materials and research you will have.",
                        "Walk round and read them. Right-click a screen to turn its page.",
                        "When you are ready, go to the choosing screen in front of you and click it."));
        // north: Phoenix
        screen(level, new BlockPos(0, Y, -12), Direction.SOUTH, WIDE, HIGH, 0xFFFFC66B,
                CinemaScreenBlockEntity.Background.SUNFLARE, "", List.of(
                        "THE PHOENIX",
                        "Rebirth through consumption. Every ending is an ignition.",
                        "You work with fire, heat and raw energy: forges, reactors, and reactions that burn hot.",
                        "Its research opens through Ember Awakening and leads to The Pyre, where you commit.",
                        "Choose it if you want power and speed, and do not mind handling a lot of heat.",
                        "Changing your mind later costs energetic and material data."));
        // east: Sculk
        screen(level, new BlockPos(12, Y, 0), Direction.WEST, WIDE, HIGH, 0xFF6FE0D0,
                CinemaScreenBlockEntity.Background.SCULK_ABYSS, "", List.of(
                        "THE SCULK",
                        "A network that remembers every death that fed it. Not a parasite: a library.",
                        "You work with living systems: growth, sensing, signals, and information that spreads.",
                        "Its research opens through First Contact and leads to The Joining, where you commit.",
                        "Choose it if you like patient, sprawling builds and machines that sense and respond.",
                        "Changing your mind later costs biological and material data."));
        // south: Void
        screen(level, new BlockPos(0, Y, 12), Direction.NORTH, WIDE, HIGH, 0xFFB983FF,
                CinemaScreenBlockEntity.Background.VOID_GALAXY, "", List.of(
                        "THE VOID",
                        "Before the first thing there was the space between things. It was patient.",
                        "You work with space and computation: storage, distance, odd geometry and the arcane.",
                        "Its research opens through First Silence and leads to The Inversion, where you commit.",
                        "Choose it if you like logistics, dimensions and precise control.",
                        "Changing your mind later costs computational and arcane data."));
        // beside the welcome: what the Conflux is, and what research and data are
        screen(level, new BlockPos(-12, Y, -7), Direction.EAST, WIDE, HIGH, 0xFFFFFFFF,
                CinemaScreenBlockEntity.Background.NEBULA, "", List.of(
                        "THE CONFLUX",
                        "The Conflux is a body of knowledge that grows as you work, and a place that answers to it.",
                        "Your team has one Conflux. Whatever one of you researches, all of you have.",
                        "Each discipline is a tree of research. Finishing a node opens new recipes, machines or "
                                + "abilities for your whole team.",
                        "Some research is gated: a recipe or a multiblock stays locked until the Conflux teaches it."));
        screen(level, new BlockPos(-12, Y, 7), Direction.EAST, WIDE, HIGH, 0xFF6FA8FF,
                CinemaScreenBlockEntity.Background.NEBULA, "", List.of(
                        "RESEARCH AND DATA",
                        "Research is paid for in data. There are five kinds: Material, Biological, Energetic, "
                                + "Computational and Arcane.",
                        "Arcane data only exists when Ars Nouveau is installed.",
                        "Data-producer machines make it. Spend it at a research terminal, on the nodes of your path.",
                        "Each discipline leans on different kinds, so what you build first depends on what you choose."));
        // beside the Sculk: committing, and what comes after
        screen(level, new BlockPos(12, Y, -7), Direction.WEST, WIDE, HIGH, 0xFFFF9A6B,
                CinemaScreenBlockEntity.Background.SUNFLARE, "", List.of(
                        "COMMITTING",
                        "Every path has a commitment node: The Pyre, The Joining, The Inversion.",
                        "Researching it commits you to the path, and the research that follows it is built on that.",
                        "You can still change path afterwards, but it costs ten times the data of changing before "
                                + "you commit, and you lose the research of the path you leave.",
                        "Be sure before you commit, and read the paths again if you are not."));
        screen(level, LAST, Direction.WEST, WIDE, HIGH, 0xFF5CFF8F, CinemaScreenBlockEntity.Background.NEBULA, "",
                List.of(
                        "AFTER YOU CHOOSE",
                        "When you choose, you are taken to the realm of your discipline.",
                        "Your questbook has a chapter for your path, and the first quests tell you where to begin.",
                        "Return here any time with /conflux dimension return.",
                        "Good luck."));
        // in front of where a new player arrives: the choice
        screen(level, CHOOSE, Direction.NORTH, 3, HIGH, 0xFFFFFFFF, CinemaScreenBlockEntity.Background.NEBULA,
                CinemaClientActions.OPEN_DISCIPLINE_PICKER, List.of(
                        "THE CHOICE",
                        "Your first choice is free.",
                        "You can change your path later by abandoning it at a research terminal, for a cost in research "
                                + "data. Once you have committed to it in its research, the cost is ten times as much.",
                        "Click here to choose."));
        PhoenixCore.LOGGER.info("[Conflux] Built the ethereal spawn's screens");
    }

    /** A screen of cols x rows blocks around {@code center}, facing {@code facing}, the text on its centre block. */
    private static void screen(ServerLevel level, BlockPos center, Direction facing, int cols, int rows, int color,
                               CinemaScreenBlockEntity.Background background, String action, List<String> pages) {
        BlockState state = CinemaBlocks.CINEMA_SCREEN.get().defaultBlockState().setValue(CinemaScreenBlock.FACING,
                facing);
        Direction right = facing.getCounterClockWise();
        int halfCols = (cols - 1) / 2;
        int halfRows = (rows - 1) / 2;
        for (int col = -halfCols; col <= halfCols; col++) {
            for (int row = -halfRows; row <= halfRows; row++) {
                BlockPos pos = center.relative(right, col).relative(Direction.UP, row);
                level.setBlock(pos, state, 3);
                if (!(level.getBlockEntity(pos) instanceof CinemaScreenBlockEntity be)) continue;
                be.applyConfig(pages.stream().map(page -> (Component) Component.literal(page)).toList(), color,
                        0.015f, CinemaScreenBlockEntity.TextAlign.CENTER.ordinal(), background.ordinal());
                be.setAction(action);
            }
        }
    }
}
