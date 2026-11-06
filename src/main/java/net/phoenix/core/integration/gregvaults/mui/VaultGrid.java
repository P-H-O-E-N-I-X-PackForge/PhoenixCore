package net.phoenix.core.integration.gregvaults.mui;

import brachy.modularui.api.widget.Interactable;
import brachy.modularui.widget.ParentWidget;

public final class VaultGrid extends ParentWidget<VaultGrid> implements Interactable {

    private final VaultSession session;

    public VaultGrid(VaultSession session) {
        this.session = session;
    }

    @Override
    public boolean onMouseScrolled(double scrollX, double scrollY) {
        if (session.sync == null || scrollY == 0) return false;
        session.sync.requestScroll(session.scrollRow + (scrollY < 0 ? 1 : -1));
        return true;
    }
}
