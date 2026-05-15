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
 * {@link SetEnabledAction#SetEnabledAction(com.vaadin.flow.component.Component, boolean)
 * SetEnabledAction(button, false)} first, then
 * {@link ClickAction#ClickAction(com.vaadin.flow.component.Component)
 * ClickAction(button)}. The submit button's server-side click listener appends
 * to a result label with the button's enabled state observed at the time the
 * listener runs — exercising the "mirror before listener" ordering target of
 * slice 2.
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

        new ShortcutTrigger(form, Key.ENTER).triggers(
                new SetEnabledAction(submit, false), new ClickAction(submit));
    }
}
