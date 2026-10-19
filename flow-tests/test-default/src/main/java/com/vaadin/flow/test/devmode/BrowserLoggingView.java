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
package com.vaadin.flow.test.devmode;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.LoadMode;

@Route("browser-logging")
@JavaScript(value = "./consoleLoggingProxy.js", loadMode = LoadMode.INLINE)
public class BrowserLoggingView extends Div {

    public BrowserLoggingView() {
        NativeLabel label = new NativeLabel("Just a label");
        label.setId("elementId");
        add(label);

        NativeButton causeException = new NativeButton(
                "Cause client side exception", e -> {
                    getUI().get().getPage().executeJs("null.foo");
                });
        causeException.setId("exception");
        add(causeException);
    }
}
