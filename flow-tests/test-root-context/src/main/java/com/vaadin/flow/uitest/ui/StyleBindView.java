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
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.signals.BindingActiveException;
import com.vaadin.signals.ValueSignal;

/**
 * Test view for end-to-end verification of Style.bind. Binds the
 * background-color style to a String signal and offers controls to toggle the
 * value, attempt manual set/remove, and detach/attach the element.
 */
@Route(value = "com.vaadin.flow.uitest.ui.StyleBindView", layout = ViewTestLayout.class)
public class StyleBindView extends Div {

    private final ValueSignal<String> color = new ValueSignal<>(
            "rgba(255, 0, 0, 1)"); // red in rgba

    public StyleBindView() {
        setId("style-bind-view");

        Div target = new Div();
        target.setId("target");
        target.setText("Color me");

        // Bind background-color to the signal
        Style style = target.getElement().getStyle();
        style.bind("background-color", color);

        // Control buttons
        NativeButton setRed = new NativeButton("Set Red",
                e -> color.value("rgba(255, 0, 0, 1)"));
        setRed.setId("set-red");

        NativeButton setGreen = new NativeButton("Set Green",
                e -> color.value("rgba(0, 128, 0, 1)"));
        setGreen.setId("set-green");

        NativeButton setNull = new NativeButton("Set Null",
                e -> color.value(null));
        setNull.setId("set-null");

        NativeButton removeBinding = new NativeButton("Remove Binding",
                e -> style.bind("background-color", null));
        removeBinding.setId("remove-binding");

        // Manual operations that should fail while bound/active
        Span status = new Span();
        status.setId("status");

        NativeButton manualSet = new NativeButton("Manual Set", e -> {
            try {
                target.getElement().getStyle().set("background-color",
                        "rgba(0, 0, 255, 1)");
                status.setText("manual-set-ok");
            } catch (BindingActiveException ex) {
                status.setText("BindingActiveException");
            }
        });
        manualSet.setId("manual-set");

        NativeButton manualRemove = new NativeButton("Manual Remove", e -> {
            try {
                target.getElement().getStyle().remove("background-color");
                status.setText("manual-remove-ok");
            } catch (BindingActiveException ex) {
                status.setText("BindingActiveException");
            }
        });
        manualRemove.setId("manual-remove");

        // Detach/attach controls
        NativeButton detach = new NativeButton("Detach", e -> {
            if (target.getParent().isPresent()) {
                remove(target);
            }
        });
        detach.setId("detach");

        NativeButton attach = new NativeButton("Attach", e -> {
            if (target.getParent().isEmpty()) {
                addComponentAtIndex(0, target);
            }
        });
        attach.setId("attach");

        add(target, setRed, setGreen, setNull, removeBinding, manualSet,
                manualRemove, detach, attach, status);
    }
}
