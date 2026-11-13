package net.phoenix.core.client.fx;

public final class FxClock {

    private double seconds;
    private long last = -1L;
    private float speed = 1f;

    public synchronized void speed(float speed) {
        this.speed = speed;
    }

    public synchronized float now() {
        return now(System.nanoTime());
    }

    synchronized float now(long nanos) {
        if (last >= 0) {
            double dt = Math.min(0.1, (nanos - last) / 1.0e9);
            if (dt > 0.0005) {
                seconds += dt * speed;
                last = nanos;
            }
        } else {
            last = nanos;
        }
        return (float) seconds;
    }
}
