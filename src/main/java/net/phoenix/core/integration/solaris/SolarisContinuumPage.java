package net.phoenix.core.integration.solaris;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.continuum.PhoenixContinuum;
import net.phoenix.continuum.client.ContinuumClientState;
import net.phoenix.continuum.client.pdim.PdimClientState;
import net.phoenix.continuum.client.render.SurfaceMap;
import net.phoenix.continuum.common.ContinuumStateSnapshot;
import net.phoenix.continuum.common.Deposits;
import net.phoenix.continuum.data.ContinuumBody;
import net.phoenix.continuum.data.ContinuumData;
import net.phoenix.continuum.data.ContinuumSystem;
import net.phoenix.continuum.data.DiscoveryStage;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenixvine.solaris.client.render.MapPage;
import net.phoenixvine.solaris.client.render.MapPages;
import net.phoenixvine.solaris.client.render.MapProjection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * A Solaris map page for the worlds Continuum has found: a top-down map of each planet or moon, with the deposits that
 * have been mapped, your outpost and your personal dimension on it. It does not depend on the dimension the player is
 * in. Only reached when Solaris is loaded.
 */
@OnlyIn(Dist.CLIENT)
final class SolarisContinuumPage implements MapPage {

    private static final SolarisContinuumPage INSTANCE = new SolarisContinuumPage();
    private static boolean registered = false;

    private int selected = 0;
    private List<ContinuumBody> cachedBodies = List.of();
    private long cachedAt = -1;

    private SolarisContinuumPage() {}

    static void register() {
        if (registered) return;
        registered = true;
        MapPages.register(INSTANCE);
    }

    private List<ContinuumBody> bodies() {
        long now = System.currentTimeMillis();
        if (now - cachedAt < 500) return cachedBodies;
        cachedAt = now;

        List<ContinuumBody> out = new ArrayList<>();
        for (ContinuumBody body : ContinuumData.allBodies()) {
            if (body.type() != ContinuumBody.Type.PLANET && body.type() != ContinuumBody.Type.MOON) continue;
            if (ContinuumClientState.isHidden(body.id())) continue;
            if (!ContinuumClientState.stage(body.id()).atLeast(DiscoveryStage.DETECTED)) continue;
            out.add(body);
        }
        out.sort(Comparator.comparing((ContinuumBody b) -> b.system().toString()).thenComparing(b -> b.orbitAu()));
        cachedBodies = out;
        return out;
    }

    private ContinuumBody current() {
        List<ContinuumBody> list = bodies();
        if (list.isEmpty()) return null;
        selected = Math.floorMod(selected, list.size());
        return list.get(selected);
    }

    @Override
    public ResourceLocation id() {
        return PhoenixContinuum.id("bodies");
    }

    @Override
    public Component title() {
        return Component.literal("Continuum");
    }

    @Override
    public boolean available() {
        return PhoenixConfigs.INSTANCE.confluxSolaris.continuumPage && !bodies().isEmpty();
    }

    @Override
    public double width() {
        return SurfaceMap.WIDTH;
    }

    @Override
    public double height() {
        return SurfaceMap.HEIGHT;
    }

    @Override
    public List<Component> variants() {
        List<Component> names = new ArrayList<>();
        for (ContinuumBody body : bodies()) {
            ContinuumSystem system = ContinuumData.system(body.system());
            String where = system != null ? system.name() + " / " : "";
            names.add(Component.literal(where + body.name()));
        }
        return names;
    }

    @Override
    public int selectedVariant() {
        return selected;
    }

    @Override
    public void selectVariant(int index) {
        selected = index;
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h, MapProjection projection, float partialTick) {
        ContinuumBody body = current();
        if (body == null) return;

        ResourceLocation texture = SurfaceMap.texture(body);
        int sx = (int) projection.screenX(-SurfaceMap.WIDTH / 2.0);
        int sy = (int) projection.screenY(-SurfaceMap.HEIGHT / 2.0);

        var pose = g.pose();
        pose.pushPose();
        pose.translate(sx, sy, 0);
        pose.scale(projection.zoom(), projection.zoom(), 1.0f);
        g.blit(texture, 0, 0, 0, 0, SurfaceMap.WIDTH, SurfaceMap.HEIGHT, SurfaceMap.WIDTH, SurfaceMap.HEIGHT);
        if (!charted(body)) g.fill(0, 0, SurfaceMap.WIDTH, SurfaceMap.HEIGHT, 0xB0000810);
        pose.popPose();

        if (!charted(body)) {
            var font = Minecraft.getInstance().font;
            String text = "Only detected. Survey " + body.name() + " to chart it.";
            g.drawCenteredString(font, text, x + w / 2, y + h / 2, 0xFFFFFFFF);
        }
    }

    private static boolean charted(ContinuumBody body) {
        return ContinuumClientState.stage(body.id()).atLeast(DiscoveryStage.SURVEYED);
    }

    @Override
    public List<Marker> markers() {
        ContinuumBody body = current();
        if (body == null || !charted(body)) return List.of();

        List<Marker> markers = new ArrayList<>();
        List<ContinuumBody.Yield> yields = body.yields();
        for (int i = 0; i < yields.size(); i++) {
            float richness = ContinuumClientState.richness(body.id(), i);
            if (richness <= 0.0f) continue;
            ContinuumBody.Yield yield = yields.get(i);
            Item item = BuiltInRegistries.ITEM.get(yield.item());
            ItemStack stack = new ItemStack(item);
            double[] at = spot(body, yield.item().toString());
            markers.add(new Marker(at[0], at[1], Component.literal("Deposit: ").append(stack.getHoverName()), 0x66DDFF,
                    stack, List.of(
                            Component.literal(Deposits.label(richness) + " (" + Deposits.percent(richness) + ")")
                                    .withStyle(ChatFormatting.GRAY),
                            Component.literal(yield.min() + "-" + yield.max() + " per cycle")
                                    .withStyle(ChatFormatting.DARK_GRAY))));
        }

        ContinuumStateSnapshot.OutpostView outpost = ContinuumClientState.outpost(body.id());
        if (outpost != null) {
            double[] at = spot(body, "outpost");
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal(outpost.probes() + " probes, " + outpost.readyAt(ContinuumClientState.now()) +
                    " cycles ready").withStyle(ChatFormatting.GRAY));
            if (outpost.damaged()) lines.add(Component.literal("Damaged: fly a repair run").withStyle(ChatFormatting.RED));
            else if (!outpost.powered()) lines.add(Component.literal("Unpowered").withStyle(ChatFormatting.RED));
            else if (outpost.upkeepPerCycle() > 0) {
                lines.add(Component.literal(String.format(Locale.ROOT, "%,d EU per cycle", outpost.upkeepPerCycle()))
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            markers.add(new Marker(at[0], at[1], Component.literal("Your outpost"),
                    outpost.damaged() || !outpost.powered() ? 0xFF5555 : 0xFFB040, ItemStack.EMPTY, lines));
        }

        Float gravity = PdimClientState.dimension(body.id());
        if (gravity != null) {
            double[] at = spot(body, "pdim");
            markers.add(new Marker(at[0], at[1], Component.literal("Your personal dimension"), 0xC890FF,
                    ItemStack.EMPTY, List.of(Component.literal(String.format(Locale.ROOT, "gravity x%.2f", gravity))
                            .withStyle(ChatFormatting.GRAY))));
        }
        return markers;
    }

    /** A stable spot on the map for a named thing, away from the poles. */
    private static double[] spot(ContinuumBody body, String what) {
        Random random = new Random(body.id().toString().hashCode() * 31L + what.hashCode());
        double x = (random.nextDouble() - 0.5) * SurfaceMap.WIDTH * 0.86;
        double y = (random.nextDouble() - 0.5) * SurfaceMap.HEIGHT * 0.62;
        return new double[] { x, y };
    }

    @Override
    public Component footer() {
        ContinuumBody body = current();
        if (body == null) return null;
        DiscoveryStage stage = ContinuumClientState.stage(body.id());
        String mapped = stage == DiscoveryStage.SURVEYED ?
                "   deposits mapped: " + ContinuumClientState.mappedDeposits(body.id()) + "/" +
                        body.yields().size() :
                "";
        return Component.literal(body.name() + "  (" + stage.label() + ")" + mapped +
                "   Q / E: switch body   Esc: back");
    }
}
