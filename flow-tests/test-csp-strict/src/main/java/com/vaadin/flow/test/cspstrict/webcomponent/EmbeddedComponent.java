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
package com.vaadin.flow.test.cspstrict.webcomponent;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.function.SerializableConsumer;

/**
 * The server-side component that {@link EmbeddedExporter} exports as a web
 * component: a greeting that follows a property set by the host page, and a
 * button whose clicks are counted on the server and reported to the host page.
 */
public class EmbeddedComponent extends Div {

    private final Span greeting = new Span();

    private int clicks;

    private SerializableConsumer<Integer> clickHandler = count -> {
    };

    public EmbeddedComponent() {
        greeting.setId("greeting");
        Span clicked = new Span("0");
        clicked.setId("clicks");
        NativeButton count = new NativeButton("Count", click -> {
            clicks++;
            clicked.setText(String.valueOf(clicks));
            clickHandler.accept(clicks);
        });
        count.setId("count");
        add(greeting, count, clicked);
    }

    /**
     * Sets the name to greet.
     *
     * @param name
     *            the name
     */
    public void setName(String name) {
        greeting.setText("Hello, " + name);
    }

    /**
     * Sets the handler called with the number of clicks after every click.
     *
     * @param clickHandler
     *            the handler, not {@code null}
     */
    public void setClickHandler(SerializableConsumer<Integer> clickHandler) {
        this.clickHandler = clickHandler;
    }
}
