/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.DefaultErrorHandler;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ComponentErrorView", layout = ViewTestLayout.class)
public class ComponentErrorView extends Div {
    protected NativeButton throwException = new NativeButton("Throw");

    public ComponentErrorView() {
        throwException.addClickListener(event -> {
            throw new IllegalArgumentException("No clicking");
        });
        throwException.setId("throw");
        add(throwException);

        UI.getCurrent().getSession().setErrorHandler(error -> {
            Span componentPresent = new Span(
                    "" + error.getComponent().isPresent());
            componentPresent.setId("present");
            add(componentPresent);
            error.getComponent().ifPresent(component -> {
                Span componentName = new Span(component.getClass().getName());
                componentName.setId("name");
                add(componentName);
            });
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        UI.getCurrent().getSession().setErrorHandler(new DefaultErrorHandler());
    }
}
