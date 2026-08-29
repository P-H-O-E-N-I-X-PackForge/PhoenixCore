package net.phoenix.core.integration.gregvaults.client.screen;

public enum VaultSortMode {

    NAME,
    COUNT;

    public VaultSortMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public VaultSortMode prev() {
        return values()[(ordinal() - 1 + values().length) % values().length];
    }

    public String label() {
        return switch (this) {
            case NAME -> "Name";
            case COUNT -> "Amount";
        };
    }
}
