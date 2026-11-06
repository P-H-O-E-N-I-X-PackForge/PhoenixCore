package net.phoenix.core.integration.gregvaults.mui;

public enum VaultDisplayMode {

    SLOTS,
    STACKED;

    public VaultDisplayMode next() {
        return this == SLOTS ? STACKED : SLOTS;
    }
}
