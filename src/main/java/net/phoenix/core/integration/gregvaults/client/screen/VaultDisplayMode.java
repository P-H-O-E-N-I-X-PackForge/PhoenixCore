package net.phoenix.core.integration.gregvaults.client.screen;

public enum VaultDisplayMode {

    SLOTS,
    STACKED;

    public VaultDisplayMode next() {
        return this == SLOTS ? STACKED : SLOTS;
    }
}
