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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.trigger.ClickAction;
import com.vaadin.flow.component.trigger.SetEnabledAction;
import com.vaadin.flow.component.trigger.ShortcutTrigger;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Wires a {@code ShortcutTrigger(Enter)} on the form to two actions on the
 * submit button:
 * {@link ClickAction#ClickAction(com.vaadin.flow.component.Component)
 * ClickAction(button)} first, then
 * {@link SetEnabledAction#SetEnabledAction(com.vaadin.flow.component.Component, boolean)
 * SetEnabledAction(button, false)}. The submit button's server-side click
 * listener appends to a result label, demonstrating that the click reached the
 * server. The local disable then closes the latency window before any second
 * user gesture can re-trigger the submit. Ordering matters: a browser blocks
 * {@code element.click()} on an already-disabled element, so the click action
 * must run while the button is still enabled.
 */
@Route(value = "com.vaadin.flow.uitest.ui.TriggerShortcutDisableView", layout = ViewTestLayout.class)
public class TriggerShortcutDisableView extends AbstractDivView {

    @Override
    protected void onShow() {
        Div form = new Div();
        form.setId("form");
        // Make sure the div receives keydown events: needs a tabindex so the
        // browser treats it as focusable.
        form.getElement().setAttribute("tabindex", "0");

        Input field = new Input();
        field.setId("field");

        NativeButton submit = new NativeButton("Submit");
        submit.setId("submit");

        Span result = new Span("(none)");
        result.setId("result");

        submit.addClickListener(
                e -> result.setText("clicked, enabled=" + submit.isEnabled()));

        form.add(field, submit);
        add(form, result);

        new ShortcutTrigger(form, Key.ENTER).triggers(new ClickAction(submit),
                new SetEnabledAction(submit, false));
    }
}
