package com.vaadin.elements.core.grid.headerfooter;

/**
 * Represents a header row in Grid.
 */
public class HeaderRow extends StaticRow<HeaderCell> {

    protected HeaderRow(StaticSection<?> section) {
        super(section);
    }

    void setDefaultRow(boolean value) {
        getRowState().defaultRow = value;
    }

    private boolean isDefaultRow() {
        return getRowState().defaultRow;
    }

    @Override
    protected HeaderCell createCell() {
        return new HeaderCell(this);
    }

    @Override
    protected String getCellTagName() {
        return "th";
    }

}