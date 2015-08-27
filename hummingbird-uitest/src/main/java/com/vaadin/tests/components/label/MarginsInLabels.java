package com.vaadin.tests.components.label;

import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.tests.components.AbstractTestUI;
import com.vaadin.ui.AbstractComponentContainer;
import com.vaadin.ui.Accordion;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;

public class MarginsInLabels extends AbstractTestUI {

    @Override
    protected void setup(VaadinRequest request) {
        AbstractComponentContainer layout = new VerticalLayout();
        layout.addComponent(
                new Label("<h1>Vertical layout</h1>", ContentMode.HTML));
        layout.addComponent(new Label("Next row"));
        add(layout);

        layout = new GridLayout(1, 2);
        layout.setWidth("100%");
        layout.addComponent(
                new Label("<h1>Grid layout</h1>", ContentMode.HTML));
        layout.addComponent(new Label("Next row"));
        add(layout);

        TabSheet tabSheet = new TabSheet();
        tabSheet.addTab(new Label("<h1>Tabsheet</h1>", ContentMode.HTML),
                "Label");
        add(tabSheet);

        Accordion accordion = new Accordion();
        accordion.addTab(new Label("<h1>Accordion</h1>", ContentMode.HTML),
                "Label");
        add(accordion);
    }

    @Override
    protected String getTestDescription() {
        return "Margins inside labels should not be allowed to collapse out of the label as it causes problems with layotus measuring the label.";
    }

    @Override
    protected Integer getTicketNumber() {
        return Integer.valueOf(8671);
    }

}
