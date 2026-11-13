package net.phoenix.core.common.machine.multiblock.nullwell;

public interface NullWellControls {

    int FEED = 0;
    int DRAW = 1;
    int VENT = 2;

    void set(int which, int value);

    void press(int action);
}
