package net.phoenix.core.common.machine.multiblock.stellar;

public interface StellarControls {

    int FEED = 0;
    int FIELD = 1;
    int SPIN = 2;
    int ARM = 3;
    int DISPERSE = 4;

    int PLASMA = 5;

    void set(int which, int value);

    void press(int action);
}
