package com.vaadin.tests.integration;

import com.vaadin.server.ClassResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

public class ServletIntegrationUI extends UI {

    @Override
    protected void init(VaadinRequest request) {
        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        setContent(layout);

        // final Grid grid = new Grid();
        // grid.addColumn("short", String.class);
        // // Disabled until ImageRenderer supports ClassResource
        // // grid.addColumn("icon", Resource.class);
        // grid.addColumn("country", String.class);
        // // grid.getColumn("icon").setHeaderCaption("")
        // // .setRenderer(new ImageRenderer());
        //
        // layout.addComponent(grid);
        //
        // // grid.addRow("FI", new ClassResource("fi.gif"), "Finland");
        // // grid.addRow("SE", new FlagSeResource(), "Sweden");
        // grid.addRow("FI", "Finland");
        // grid.addRow("SE", "Sweden");
        //
        // grid.setColumns("icon", "country");

        final Label selectedLabel = new Label(); // FIXME Read from template

        // grid.addSelectionListener(new SelectionListener() {
        //
        // @Override
        // public void select(SelectionEvent event) {
        // Object selectedItemId = grid.getSelectedRow();
        // String sel = "(null)";
        // if (selectedItemId != null) {
        // sel = (String)
        // grid.getContainerDataSource().getItem(selectedItemId).getItemProperty("short").getValue();
        // }
        // selectedLabel.setValue("Selected: " + sel);
        // }
        // });
        layout.addComponent(selectedLabel);
        Label fi = new Label("The Finnish flag");
        fi.setIcon(new ClassResource("fi.gif"));
        layout.addComponent(fi);
        Label se = new Label("The Swedish flag");
        se.setIcon(new FlagSeResource());
        layout.addComponent(se);
    }

}
