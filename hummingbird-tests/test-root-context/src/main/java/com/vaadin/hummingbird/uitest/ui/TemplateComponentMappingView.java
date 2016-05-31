/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.uitest.ui;

import com.vaadin.annotations.Id;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.html.Button;
import com.vaadin.hummingbird.html.Div;
import com.vaadin.hummingbird.html.Span;
import com.vaadin.ui.Template;

public class TemplateComponentMappingView extends Template {

    static final String LOG_ID = "log";

    static final String BUTTON_ID = "button";

    static final String SPAN_ID = "span";

    static final String INPUT_ID = "input";

    @Id(LOG_ID)
    private Div log;
    @Id(SPAN_ID)
    private Span span;
    @Id(BUTTON_ID)
    private Button button;

    public TemplateComponentMappingView() {
        span.getElement().addEventListener("click",
                e -> logClick(e.getSource()));
        button.addClickListener(e -> {
            logClick(e.getSource().getElement());
        });
    }

    private void logClick(Element source) {
        log(source.getAttribute("id") + " was clicked");
    }

    private void log(String message) {
        getUI().get().getPage().executeJavaScript("log($0);",
                "server: " + message);
    }

}
