/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */

package com.vaadin.flow.uitest.ui.littemplate;

import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.littemplate.SetInitialTextLitView", layout = ViewTestLayout.class)
@Tag("set-initial-text-lit")
@JsModule("lit/SetInitialText.js")
public class SetInitialTextLitView extends LitTemplate
        implements HasComponents {

    @Id("child")
    private Div child;

    public SetInitialTextLitView() {
        // this is no-op since the text is an empty string by default but it
        // removes all children
        child.setText("");
        setId("set-initial-text");

        NativeButton button = new NativeButton("Add a new child",
                event -> addChild());
        button.setId("add-child");
        add(button);
    }

    private void addChild() {
        Div div = new Div();

        div.setId("new-child");
        div.setText("New child");
        child.add(div);
    }

}