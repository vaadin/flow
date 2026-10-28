/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.test.jpms;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

/**
 * Shows which modules the application and Flow are loaded in.
 */
@Route("")
public class ModuleInfoView extends Div {

    public ModuleInfoView() {
        Span appModule = new Span(describe(ModuleInfoView.class.getModule()));
        appModule.setId("app-module");
        Span flowModule = new Span(describe(Route.class.getModule()));
        flowModule.setId("flow-module");
        Span componentsModule = new Span(describe(Span.class.getModule()));
        componentsModule.setId("components-module");

        Div clicked = new Div();
        clicked.setId("clicked");
        NativeButton button = new NativeButton("Click",
                event -> clicked.setText("Clicked"));
        button.setId("button");

        Div pushed = new Div();
        pushed.setId("pushed");
        NativeButton pushButton = new NativeButton("Push", event -> {
            UI ui = event.getSource().getUI().orElseThrow();
            Thread.ofVirtual().start(
                    () -> ui.access(() -> pushed.setText("Pushed")));
        });
        pushButton.setId("push-button");

        add(appModule, flowModule, componentsModule, button, clicked,
                pushButton, pushed);
    }

    private static String describe(Module module) {
        if (!module.isNamed()) {
            return "unnamed";
        }
        return module.getName() + " "
                + (module.getDescriptor().isAutomatic() ? "automatic"
                        : "explicit");
    }
}
