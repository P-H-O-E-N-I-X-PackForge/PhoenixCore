package net.phoenix.core.client.renderer.cinema.cutscene.background;

/**
 * One layer of a {@link CutsceneBackgroundBuilder}: returns the ARGB colour at a point on screen.
 * Built-in effects live in {@link BackgroundEffects}; a lambda works too for code-only backgrounds.
 */
@FunctionalInterface
public interface BackgroundEffect {

    int colorAt(Point point, float time);

    /** An effect that can be read from JSON. {@link #type()} is its key in {@link BackgroundEffects}. */
    interface Data extends BackgroundEffect {

        String type();
    }

    /**
     * A sample position, reused between calls.
     * <ul>
     * <li>{@code u, v}: 0..1 across / down the screen</li>
     * <li>{@code nx, ny}: centred and aspect-corrected; ny is -1..1 (down is positive), nx is -aspect..aspect</li>
     * <li>{@code dist, angle}: polar form of (nx, ny), so dist 1 touches the top and bottom edges</li>
     * </ul>
     */
    final class Point {

        public float u, v, nx, ny, dist, angle;
    }
}
