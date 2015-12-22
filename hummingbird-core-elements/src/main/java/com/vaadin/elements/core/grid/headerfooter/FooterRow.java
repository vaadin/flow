package com.vaadin.elements.core.grid.headerfooter;

/**
 * Represents a footer row in Grid.
 */
public class FooterRow extends StaticRow<FooterCell> {

    protected FooterRow(StaticSection<?> section) {
        super(section);
    }

    @Override
    protected FooterCell createCell() {
        return new FooterCell(this);
    }

    @Override
    protected String getCellTagName() {
        return "td";
    }

}