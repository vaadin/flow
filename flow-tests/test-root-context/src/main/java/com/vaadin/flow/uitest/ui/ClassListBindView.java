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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.signals.ValueSignal;

/**
 * Test view for end-to-end verification of ClassList.bind. Binds a highlight
 * class to a boolean signal and provides a toggle button to flip the signal.
 */
@Route(value = "com.vaadin.flow.uitest.ui.ClassListBindView", layout = ViewTestLayout.class)
public class ClassListBindView extends Div {

    private final ValueSignal<Boolean> highlight = new ValueSignal<>(false);

    public ClassListBindView() {
        setId("classlist-bind-view");

        // Add a <style> so we can verify visual styling via computed CSS
        Element style = new Element("style");
        style.setText(".highlight{color: rgb(255, 0, 0);}");
        getElement().appendChild(style);

        Div target = new Div();
        target.setId("target");
        target.setText("Highlight me");
        // Bind the class presence to the signal
        target.getElement().getClassList().bind("highlight", highlight);

        NativeButton toggle = new NativeButton("Toggle highlight", event -> {
            Boolean current = highlight.value();
            highlight.value(current == null ? Boolean.TRUE : !current);
        });
        toggle.setId("toggle");

        add(target, toggle);
    }
}
