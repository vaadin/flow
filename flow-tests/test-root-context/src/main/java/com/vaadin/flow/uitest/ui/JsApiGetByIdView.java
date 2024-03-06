/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.JsApiGetByIdView", layout = ViewTestLayout.class)
@JavaScript("./js-get-by-id.js")
public class JsApiGetByIdView extends AbstractDivView {

    public JsApiGetByIdView() {
        Label label = new Label("Original label");
        label.setId("source");
        add(label);

        Label target = new Label();
        target.setId("target");
        add(target);

        getElement().getNode()
                .runWhenAttached(ui -> ui.getPage().executeJs(
                        "window.jsApiConnector.jsFunction($0, this.$appId, $1)",
                        target, label.getElement().getNode().getId()));

        add(createButton("Update target", "update",
                event -> target.getElement().callJsFunction("operation")));
    }
}
