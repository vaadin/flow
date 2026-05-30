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
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.trigger.internal.CallbackAction;
import com.vaadin.flow.component.trigger.internal.KeyboardEventTrigger;
import com.vaadin.flow.component.trigger.internal.ShortcutTrigger;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Two {@link ShortcutTrigger}s on the same host — {@code Ctrl+S} and
 * {@code Ctrl+Shift+S} — appending {@code "S"} / {@code "SS"} respectively to a
 * status {@link Div}. The IT verifies that the modifier match is exact:
 * {@code Ctrl+Shift+S} must not trigger the {@code Ctrl+S} shortcut.
 */
@Route(value = "com.vaadin.flow.uitest.ui.TriggerShortcutView", layout = ViewTestLayout.class)
public class TriggerShortcutView extends AbstractDivView {

    @Override
    protected void onShow() {
        Input field = new Input();
        field.setId("source");
        Div status = new Div();
        status.setId("status");
        add(field, status);

        new ShortcutTrigger(field, Key.KEY_S, KeyModifier.CONTROL)
                .triggers(new CallbackAction<>(String.class,
                        key -> status.setText(status.getText() + "S"),
                        KeyboardEventTrigger.EventData.key));

        new ShortcutTrigger(field, Key.KEY_S, KeyModifier.CONTROL,
                KeyModifier.SHIFT)
                .triggers(new CallbackAction<>(String.class,
                        key -> status.setText(status.getText() + "SS"),
                        KeyboardEventTrigger.EventData.key));
    }
}
