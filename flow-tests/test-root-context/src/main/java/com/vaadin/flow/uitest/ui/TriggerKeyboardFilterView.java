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
import com.vaadin.flow.component.trigger.internal.CallbackAction;
import com.vaadin.flow.component.trigger.internal.KeyboardEventTrigger;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Wires a {@link KeyboardEventTrigger} on an {@link Input}'s {@code keydown}
 * event with a {@link KeyboardEventTrigger#forKeys(Key...) forKeys(Enter,
 * Escape)} filter to a {@link CallbackAction} that appends each accepted
 * {@code event.key} to a status {@link Div}. The IT presses a mix of filtered
 * and non-filtered keys and asserts the final concatenation contains only the
 * filtered ones in order — proving the guard runs client-side and unmatched
 * keys never reach the server.
 */
@Route(value = "com.vaadin.flow.uitest.ui.TriggerKeyboardFilterView", layout = ViewTestLayout.class)
public class TriggerKeyboardFilterView extends AbstractDivView {

    @Override
    protected void onShow() {
        Input field = new Input();
        field.setId("source");
        Div status = new Div();
        status.setId("status");
        add(field, status);

        new KeyboardEventTrigger(field).forKeys(Key.ENTER, Key.ESCAPE)
                .triggers(new CallbackAction<>(String.class,
                        key -> status.setText(status.getText() + key),
                        KeyboardEventTrigger.EventData.key));
    }
}
