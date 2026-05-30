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
import com.vaadin.flow.component.trigger.internal.LiteralInput;
import com.vaadin.flow.component.trigger.internal.SequenceTrigger;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * A {@link SequenceTrigger} that fires when the letters {@code "hi"} are typed
 * in order on the input field. Each fire appends {@code "!"} to a status
 * {@link Div}, so the IT can assert that partial / interrupted sequences do not
 * fire and that subsequent valid sequences continue to work.
 */
@Route(value = "com.vaadin.flow.uitest.ui.TriggerSequenceView", layout = ViewTestLayout.class)
public class TriggerSequenceView extends AbstractDivView {

    @Override
    protected void onShow() {
        Input field = new Input();
        field.setId("source");
        Div status = new Div();
        status.setId("status");
        add(field, status);

        // SequenceTrigger is not a KeyboardEventTrigger, so KeyboardEvent
        // EventData.* sources are off-limits. A LiteralInput is enough here
        // because the action only needs to know "the sequence completed",
        // not which key triggered it.
        new SequenceTrigger(field, Key.KEY_H, Key.KEY_I)
                .triggers(new CallbackAction<>(String.class,
                        ignored -> status.setText(status.getText() + "!"),
                        new LiteralInput<>("!")));
    }
}
