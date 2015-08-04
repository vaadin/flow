package com.vaadin.tests.integration;

import com.vaadin.event.SelectionEvent;
import com.vaadin.event.SelectionEvent.SelectionListener;
import com.vaadin.server.ClassResource;
import com.vaadin.server.Resource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

public class IntegrationTestApplication extends UI {

    @Override
    public void init(VaadinRequest r) {
        VerticalLayout vl = new VerticalLayout();
        final Grid table = new Grid();
        table.addColumn("icon", Resource.class);
        // table.setItemIconPropertyId("icon");
        table.addColumn("country", String.class);
        // table.setVisibleColumns(new Object[] { "country" });
        vl.addComponent(table);

        Object id = table.addRow(new ClassResource("fi.gif"), "Finland");
        id = table.addRow(new FlagSeResource(), "Sweden");

        final Label selectedLabel = new Label();
        table.addSelectionListener(new SelectionListener() {

            @Override
            public void select(SelectionEvent event) {
                selectedLabel.setValue(String.valueOf(table.getSelectedRow()));
            }
        });
        vl.addComponent(selectedLabel);
        setContent(vl);
    }
}
