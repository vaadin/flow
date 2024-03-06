/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend.scanner.samples;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.server.UIInitEvent;
import com.vaadin.flow.server.UIInitListener;

public class MyUIInitListener implements UIInitListener {

    @JsModule("baz.js")
    @JavaScript("foobar.js")
    public static class MyComponent extends Component {

    }

    @Override
    public void uiInit(UIInitEvent event) {
        event.getSource().addUIInitListener(uiEvent -> {
            final UI ui = uiEvent.getUI();
            ui.add(new MyComponent());
        });
    }

}
