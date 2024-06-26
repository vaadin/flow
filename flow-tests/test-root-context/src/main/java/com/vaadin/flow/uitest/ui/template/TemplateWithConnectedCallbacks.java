/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;

@Tag("template-with-connected-callbacks")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/TemplateWithConnectedCallbacks.html")
@JsModule("TemplateWithConnectedCallbacks.js")
public class TemplateWithConnectedCallbacks extends Component {

    public TemplateWithConnectedCallbacks() {
        getElement().synchronizeProperty("connected", "connected-changed");

        getElement().addPropertyChangeListener("connected", evt -> {
            if (evt.isUserOriginated()) {
                setConnected("Connected (checked from server side)");
            }
        });
    }

    public String getConnected() {
        return getElement().getProperty("connected");
    }

    public void setConnected(String connected) {
        getElement().setProperty("connected", connected);
    }
}
