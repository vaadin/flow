/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ShortcutsWithValueChangeModeView", layout = ViewTestLayout.class)
public class ShortcutsWithValueChangeModeView extends Div
        implements AfterNavigationObserver {

    private final Input input;

    public ShortcutsWithValueChangeModeView() {

        input = new Input();
        input.setId("input");

        NativeButton button = new NativeButton("Report value");
        button.setId("button");

        Paragraph value = new Paragraph();
        value.setId("value");
        value.setText("");

        input.setValueChangeMode(ValueChangeMode.LAZY);
        // make this really big to make testing easier
        input.setValueChangeTimeout(3000);

        // clickShortcutWorks
        button.setText(
                "Button triggered by CTRL + ALT + S and CTRL + ENTER (with reset focus)");
        button.addClickShortcut(Key.KEY_S, KeyModifier.CONTROL,
                KeyModifier.ALT);
        button.addClickShortcut(Key.ENTER, KeyModifier.CONTROL)
                .setResetFocusOnActiveElement(true);
        button.addClickListener(e -> value.setText(input.getValue()));

        add(input, button, value);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        String valueChangeMode = event.getLocation().getQueryParameters()
                .getQueryString();
        if (valueChangeMode != null && !valueChangeMode.isBlank()) {
            input.setValueChangeMode(ValueChangeMode.valueOf(valueChangeMode));
        }
    }
}
