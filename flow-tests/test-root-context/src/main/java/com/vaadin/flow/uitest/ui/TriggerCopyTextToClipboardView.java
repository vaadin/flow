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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.ClickTrigger;
import com.vaadin.flow.component.trigger.internal.CopyTextToClipboardAction;
import com.vaadin.flow.component.trigger.internal.LiteralInput;
import com.vaadin.flow.component.trigger.internal.PropertyInput;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Two buttons exercising {@link CopyTextToClipboardAction}: one copies the
 * current value of an {@link Input} (via {@link PropertyInput}), the other
 * copies a fixed string (via {@link LiteralInput}) that includes a quote and a
 * newline to verify the JSON escaping round-trips through the browser. Each
 * action's success/error consumers write the outcome into the status
 * {@link Div}. The IT replaces {@code navigator.clipboard.writeText} with a
 * recording shim so the assertions don't depend on browser clipboard
 * permissions.
 */
@Route(value = "com.vaadin.flow.uitest.ui.TriggerCopyTextToClipboardView", layout = ViewTestLayout.class)
public class TriggerCopyTextToClipboardView extends AbstractDivView {

    /**
     * Literal copied by the {@code #copy-static} button — see the IT for the
     * matching expected value.
     */
    static final String STATIC_TEXT = "hello \"world\"\n";

    @Override
    protected void onShow() {
        Input field = new Input();
        field.setId("source");
        NativeButton copyButton = new NativeButton("Copy input value");
        copyButton.setId("copy");
        NativeButton copyStaticButton = new NativeButton("Copy static text");
        copyStaticButton.setId("copy-static");
        Div status = new Div();
        status.setId("status");

        add(field, copyButton, copyStaticButton, status);

        Action.Input<String> value = new PropertyInput<>(field, "value",
                String.class);
        CopyTextToClipboardAction copy = new CopyTextToClipboardAction(value,
                () -> status.setText("ok"),
                err -> status.setText("err:" + err));
        new ClickTrigger(copyButton).triggers(copy);

        CopyTextToClipboardAction copyStatic = new CopyTextToClipboardAction(
                new LiteralInput<>(STATIC_TEXT), () -> status.setText("ok"),
                err -> status.setText("err:" + err));
        new ClickTrigger(copyStaticButton).triggers(copyStatic);
    }
}
