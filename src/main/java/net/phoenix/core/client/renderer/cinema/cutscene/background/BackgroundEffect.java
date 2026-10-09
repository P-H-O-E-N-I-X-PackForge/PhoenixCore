package net.phoenix.core.client.renderer.cinema.cutscene.background;

@FunctionalInterface
public interface BackgroundEffect {

    int colorAt(Point point, float time);

    interface Data extends BackgroundEffect {

        String type();
    }

    final class Point {

        public float u, v, nx, ny, dist, angle;
    }
}
