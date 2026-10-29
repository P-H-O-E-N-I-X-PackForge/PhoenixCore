package net.phoenix.core.integration.conflux.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.block.cinema.CinemaBlocks;
import net.phoenix.core.common.block.cinema.CinemaScreenBlock;
import net.phoenix.core.common.block.cinema.CinemaScreenBlockEntity;

import java.util.List;

/**
 * The cinema screens of the ethereal spawn that explain the choice of discipline to a new player: a welcome, what the
 * Conflux is, research and data, one for each discipline (what it is good at, what it costs), committing, what comes
 * after, and the choosing screen, whose button opens the picker.
 * <p>
 * Each page is a title, a body and, if it has them, buttons: see {@link net.phoenix.core.common.block.cinema.CinemaPage}.
 * The pages are each player's own (one player turning a page does not turn it for anyone else), and a click on a button
 * does what it says; a click anywhere else, or the arrows, turns the page. Built when the dimension first loads and left
 * alone after that (break the choosing screen, or run {@code /conflux dimension spawndecor}, to build them again).
 */
public final class EtherealSpawnDecor {

    private static final int Y = 66;
    private static final int WIDE = 5;
    private static final int HIGH = 3;

    private static final BlockPos CHOOSE = new BlockPos(0, Y, 6);
    /** The last screen added: if it is there, so are the rest. */
    private static final BlockPos LAST = new BlockPos(12, Y, 7);

    private static final String PICK = "@button Choose your path | open_discipline_picker";

    private EtherealSpawnDecor() {}

    /** Builds the screens unless the choosing screen and the last screen are already there. */
    public static void ensure(ServerLevel level) {
        var screen = CinemaBlocks.CINEMA_SCREEN.get();
        // the choosing screen's pages being the player's own marks the current version of the screens
        boolean current = level.getBlockState(CHOOSE).getBlock() == screen && level.getBlockState(LAST).getBlock() == screen &&
                level.getBlockEntity(CHOOSE) instanceof CinemaScreenBlockEntity be && be.isLocalPages();
        if (!current) build(level);
    }

    /** Builds (or rebuilds) every screen. */
    public static void build(ServerLevel level) {
        // west: the way in
        screen(level, new BlockPos(-12, Y, 0), Direction.EAST, 0xFFFFFFFF, CinemaScreenBlockEntity.Background.VOID_GALAXY,
                List.of(
                        "WELCOME TO THE CONFLUX\nThis is a threshold. What you become next is a choice, and you make it once.\n"
                                + "@button Begin | next",
                        "THREE PATHS\nThree disciplines are open to you. Each is a way of working, and each shapes the machines, "
                                + "materials and research you will have.",
                        "HOW THESE SCREENS WORK\nClick a button to use it, or the arrows to turn the page. Right-click "
                                + "anywhere else on a screen to go to the next page.",
                        "WHERE TO GO\nRead the screens on each side: what the Conflux is, what research costs, and the three "
                                + "paths. When you are ready, use the choosing screen in front of you."));
        screen(level, new BlockPos(-12, Y, -7), Direction.EAST, 0xFFFFFFFF, CinemaScreenBlockEntity.Background.NEBULA,
                List.of(
                        "THE CONFLUX\nA body of knowledge that grows as you work, and a place that answers to it.",
                        "ONE FOR YOUR TEAM\nYour team has one Conflux. Whatever one of you researches, all of you have.",
                        "TREES OF RESEARCH\nEach discipline is a tree. Finishing a node opens new recipes, machines or abilities "
                                + "for your whole team.",
                        "GATES\nSome things stay locked until the Conflux teaches them: a recipe, or a multiblock you cannot "
                                + "form yet."));
        screen(level, new BlockPos(-12, Y, 7), Direction.EAST, 0xFF6FA8FF, CinemaScreenBlockEntity.Background.NEBULA,
                List.of(
                        "RESEARCH IS PAID IN DATA\nThere are five kinds: Material, Biological, Energetic, Computational and "
                                + "Arcane.",
                        "ARCANE DATA\nArcane data only exists when Ars Nouveau is installed.",
                        "MAKING DATA\nData-producer machines make it. Spend it at a research terminal, on the nodes of your "
                                + "path.",
                        "WHAT YOU NEED\nEach discipline leans on different kinds, so what you build first depends on what you "
                                + "choose."));
        // north: Phoenix
        screen(level, new BlockPos(0, Y, -12), Direction.SOUTH, 0xFFFFC66B, CinemaScreenBlockEntity.Background.SUNFLARE,
                List.of(
                        "THE PHOENIX\nRebirth through consumption. Every ending is an ignition.\n@button Next | next",
                        "WHAT YOU DO\nYou work with fire, heat and raw energy: forges, reactors, and reactions that burn hot.",
                        "ITS RESEARCH\nIt opens through Ember Awakening and leads to The Pyre, where you commit.",
                        "CHOOSE IT IF\nYou want power and speed, and do not mind handling a lot of heat.",
                        "CHANGING YOUR MIND\nCosts energetic and material data."));
        // east: Sculk
        screen(level, new BlockPos(12, Y, 0), Direction.WEST, 0xFF6FE0D0, CinemaScreenBlockEntity.Background.SCULK_ABYSS,
                List.of(
                        "THE SCULK\nA network that remembers every death that fed it. Not a parasite: a library.\n"
                                + "@button Next | next",
                        "WHAT YOU DO\nYou work with living systems: growth, sensing, signals, and information that spreads.",
                        "ITS RESEARCH\nIt opens through First Contact and leads to The Joining, where you commit.",
                        "CHOOSE IT IF\nYou like patient, sprawling builds and machines that sense and respond.",
                        "CHANGING YOUR MIND\nCosts biological and material data."));
        // south: Void
        screen(level, new BlockPos(0, Y, 12), Direction.NORTH, 0xFFB983FF, CinemaScreenBlockEntity.Background.VOID_GALAXY,
                List.of(
                        "THE VOID\nBefore the first thing there was the space between things. It was patient.\n"
                                + "@button Next | next",
                        "WHAT YOU DO\nYou work with space and computation: storage, distance, odd geometry and the arcane.",
                        "ITS RESEARCH\nIt opens through First Silence and leads to The Inversion, where you commit.",
                        "CHOOSE IT IF\nYou like logistics, dimensions and precise control.",
                        "CHANGING YOUR MIND\nCosts computational and arcane data."));
        // beside the Sculk: committing, and what comes after
        screen(level, new BlockPos(12, Y, -7), Direction.WEST, 0xFFFF9A6B, CinemaScreenBlockEntity.Background.SUNFLARE,
                List.of(
                        "COMMITTING\nEvery path has a commitment node: The Pyre, The Joining, The Inversion.",
                        "WHAT IT DOES\nResearching it commits you to the path, and the research that follows it is built on that.",
                        "CHANGING AFTERWARDS\nYou can still change path, but it costs ten times the data of changing before you "
                                + "commit, and you lose the research of the path you leave.",
                        "BE SURE\nRead the paths again if you are not."));
        screen(level, LAST, Direction.WEST, 0xFF5CFF8F, CinemaScreenBlockEntity.Background.NEBULA, List.of(
                "AFTER YOU CHOOSE\nYou are taken to the realm of your discipline.",
                "YOUR QUESTBOOK\nIt has a chapter for your path, and the first quests tell you where to begin.",
                "COMING BACK\nReturn here any time with /conflux dimension return.",
                "GOOD LUCK\nThe Conflux is listening."));
        // in front of where a new player arrives: the choice (3 wide)
        screen(level, CHOOSE, Direction.NORTH, 0xFFFFFFFF, CinemaScreenBlockEntity.Background.NEBULA, List.of(
                "THE CHOICE\nYour first choice is free.\n" + PICK,
                "CHANGING IT LATER\nYou can change your path by abandoning it at a research terminal, for a cost in research "
                        + "data. Once you have committed it is ten times as much.\n" + PICK,
                "READY?\nRead the screens around you first if you have not.\n" + PICK));
        PhoenixCore.LOGGER.info("[Conflux] Built the ethereal spawn's screens");
    }

    /** A screen of 5 x 3 blocks (the choosing one is 3 wide), the text on its centre block. */
    private static void screen(ServerLevel level, BlockPos center, Direction facing, int color,
                               CinemaScreenBlockEntity.Background background, List<String> pages) {
        int cols = center.equals(CHOOSE) ? 3 : WIDE;
        BlockState state = CinemaBlocks.CINEMA_SCREEN.get().defaultBlockState().setValue(CinemaScreenBlock.FACING,
                facing);
        Direction right = facing.getCounterClockWise();
        int halfCols = (cols - 1) / 2;
        int halfRows = (HIGH - 1) / 2;
        for (int col = -halfCols; col <= halfCols; col++) {
            for (int row = -halfRows; row <= halfRows; row++) {
                BlockPos pos = center.relative(right, col).relative(Direction.UP, row);
                level.setBlock(pos, state, 3);
                if (!(level.getBlockEntity(pos) instanceof CinemaScreenBlockEntity be)) continue;
                be.applyConfig(pages.stream().map(page -> (Component) Component.literal(page)).toList(), color,
                        0.015f, CinemaScreenBlockEntity.TextAlign.CENTER.ordinal(), background.ordinal());
                be.setAction("");
                be.setLocalPages(true);
            }
        }
    }
}
