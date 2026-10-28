package net.phoenix.core.common.machine.multiblock.stellar;

/** What the Stellar Forge screen can ask of the machine; the machine sends it to the server. */
public interface StellarControls {

    int FEED = 0;
    int FIELD = 1;
    int SPIN = 2;
    int ARM = 3;
    int DISPERSE = 4;

    /** Pick the next exotic plasma to draw (and none, the usual oxygen plasma, after the last). */
    int PLASMA = 5;

    /** Set feed, field or spin to a percentage. */
    void set(int which, int value);

    /** Arm the collapse, or disperse everything. */
    void press(int action);
}
