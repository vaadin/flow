package com.vaadin.elements.core.grid.headerfooter;

import com.vaadin.elements.core.grid.Grid;
import com.vaadin.shared.ui.grid.GridStaticSectionState;

/**
 * Represents the footer section of a Grid. By default Footer is not visible.
 */
public class Footer extends StaticSection<FooterRow> {

    private final GridStaticSectionState footerState = new GridStaticSectionState();

    public Footer(Grid grid) {
        this.grid = grid;
        grid.getState(true).footer = footerState;
    }

    @Override
    protected GridStaticSectionState getSectionState() {
        return footerState;
    }

    @Override
    protected FooterRow createRow() {
        return new FooterRow(this);
    }

    @Override
    public void sanityCheck() throws IllegalStateException {
        super.sanityCheck();
    }
}