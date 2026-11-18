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
package com.vaadin.flow.quarkus.it.webcomponent;

import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.webcomponent.WebComponent;

/**
 * Exports a component as a web component, which Flow creates through
 * reflection. The page META-INF/resources/embedded.html embeds it.
 */
public class GreetingExporter extends WebComponentExporter<Span> {

    public static final String TAG = "quarkus-greeting";
    public static final String TEXT_ID = "text";
    public static final String TEXT = "Hello from a web component";

    public GreetingExporter() {
        super(TAG);
    }

    @Override
    protected void configureInstance(WebComponent<Span> webComponent,
            Span component) {
        component.setId(TEXT_ID);
        component.setText(TEXT);
    }
}
