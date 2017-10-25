/*
 * Copyright 2000-2017 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.CompositeView.NameField;
import com.vaadin.router.Route;
import com.vaadin.ui.Composite;
import com.vaadin.ui.html.Div;

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
