package net.phoenix.core.integration.vocal_resonance.puzzle;

public enum AttuneDifficulty {

    EASY("Easy", 1.70f, 1900, 90, 180, 0.50, 0xFF7AFFB0),
    NORMAL("Normal", 1.35f, 1500, 75, 150, 0.60, 0xFFFFE066),
    HARD("Hard", 1.05f, 1200, 60, 120, 0.65, 0xFFFF9A5C),
    EXPERT("Expert", 0.85f, 1000, 45, 95, 0.72, 0xFFFF5577);

    public static final int LEAD_MS = 3000;

    private final String label;
    private final float timeScale;
    private final int fallMs;
    private final int perfectMs;
    private final int goodMs;
    private final double pass;
    private final int color;

    AttuneDifficulty(String label, float timeScale, int fallMs, int perfectMs, int goodMs, double pass, int color) {
        this.label = label;
        this.timeScale = timeScale;
        this.fallMs = fallMs;
        this.perfectMs = perfectMs;
        this.goodMs = goodMs;
        this.pass = pass;
        this.color = color;
    }

    public String label() {
        return label;
    }

    public int level() {
        return ordinal() + 1;
    }

    public int fallMs() {
        return fallMs;
    }

    public int perfectMs() {
        return perfectMs;
    }

    public int goodMs() {
        return goodMs;
    }

    public double pass() {
        return pass;
    }

    public int color() {
        return color;
    }

    public int beatMs(int tick) {
        return Math.round(tick * AcousticChart.MS_PER_TICK * timeScale);
    }

    public int forgiven() {
        return ordinal();
    }

    public String boost() {
        return switch (this) {
            case EASY -> "Wakes the lock.";
            case NORMAL -> "+1 hint note. The puzzle forgives 1 wrong note.";
            case HARD -> "+2 hint notes. The puzzle forgives 2 wrong notes.";
            case EXPERT -> "+3 hint notes. The puzzle forgives 3 wrong notes.";
        };
    }

    public static AttuneDifficulty fromLevel(int level) {
        AttuneDifficulty[] all = values();
        return all[Math.max(0, Math.min(level - 1, all.length - 1))];
    }
}
