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
import com.vaadin.router.Route;
import com.vaadin.ui.polymertemplate.Id;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.dom.Element;
import com.vaadin.ui.AngularTemplate;

@Route(value = "com.vaadin.flow.uitest.ui.TemplateComponentMappingView", layout = ViewTestLayout.class)
public class TemplateComponentMappingView extends AngularTemplate {

    static final String LOG_ID = "log";

    static final String BUTTON_ID = "button";

    static final String SPAN_ID = "span";

    static final String INPUT_ID = "input";

    @Id(LOG_ID)
    private Div log;
    @Id(SPAN_ID)
    private Span span;
    @Id(BUTTON_ID)
    private NativeButton button;
    @Id(INPUT_ID)
    private Input input;

    public TemplateComponentMappingView() {
        span.getElement().addEventListener("click",
                e -> logClick(e.getSource()));
        button.addClickListener(e -> {
            logClick(e.getSource().getElement());
        });
        input.addChangeListener(e -> {
            log(e.getSource().getId().get() + " value changed to "
                    + input.getValue());
        });
        input.getElement().setProperty("value", "baz");
    }

    private void logClick(Element source) {
        log(source.getAttribute("id") + " was clicked");
    }

    private void log(String message) {
        getUI().get().getPage().executeJavaScript("log($0);",
                "server: " + message);
    }

}
