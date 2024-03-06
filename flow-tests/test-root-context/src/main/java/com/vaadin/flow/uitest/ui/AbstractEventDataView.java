/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;

public class AbstractEventDataView extends AbstractDivView {

    public static final String EMPTY_VALUE = "EMPTY";
    public static final String TARGET_ID = "target";
    public static final String VIEW_CONTAINER = "View-container";
    public static final String HEADER = "Header";

    public AbstractEventDataView() {
        add(new Text(VIEW_CONTAINER), new H3(HEADER));
        setId(VIEW_CONTAINER);
    }

    protected void createComponents() {
        for (int i = 0; i < 10; i++) {
            final Div container = createContainer("Child-" + i);
            for (int j = 0; j < 10; j++) {
                final Div child = createContainer("Grandchild-" + i + j);
                child.getStyle().set("display", "inline-block");
                container.add(child);
            }
            add(container);
        }
    }

    private Div createContainer(String identifier) {
        final Div div = new Div();
        div.add(new Text(identifier));
        div.setId(identifier);
        div.getStyle().set("border", "1px solid orange").set("padding", "5px");
        return div;
    }
}
