/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.CompositeView.NameField;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.CompositeNestedView", layout = ViewTestLayout.class)
public class CompositeNestedView extends Composite<Div> {

    public static final String NAME_ID = "name";
    public static final String NAME_FIELD_ID = "nameField";

    private NameField nameField;

    @Override
    protected Div initContent() {
        Div div = new Div();
        nameField = new NameField();
        nameField.setId(NAME_FIELD_ID);
        Div name = new Div();
        name.setText("Name on server: " + nameField.getName());
        name.setId(NAME_ID);
        nameField.addNameChangeListener(e -> {
            name.setText("Name on server: " + nameField.getName());
        });
        div.add(name, nameField);
        return div;
    }
}
