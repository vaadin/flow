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
import com.vaadin.flow.component.trigger.internal.LiteralInput;
import com.vaadin.flow.component.trigger.internal.PropertyInput;
import com.vaadin.flow.component.trigger.internal.WriteToClipboardAction;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Three buttons exercising {@link WriteToClipboardAction}: one copies the
 * current value of an {@link Input} as {@code text/plain} (via
 * {@link PropertyInput}), one copies a fixed string with embedded escape
 * characters to verify JSON encoding round-trips, and one packs both
 * {@code text/plain} and {@code text/html} into a single
 * {@link com.vaadin.flow.component.html.Div ClipboardItem} to verify the
 * multi-format path resolves with the text value. Each action's success/error
 * consumers write the outcome into the status {@link Div}. The IT replaces
 * {@code navigator.clipboard.write} and {@code ClipboardItem} with recording
 * shims so the assertions don't depend on browser clipboard permissions.
 */
@Route(value = "com.vaadin.flow.uitest.ui.TriggerWriteToClipboardView", layout = ViewTestLayout.class)
public class TriggerWriteToClipboardView extends AbstractDivView {

    /** Literal copied by the {@code #copy-static} button. */
    static final String STATIC_TEXT = "hello \"world\"\n";

    /** Text slot copied by the {@code #copy-multi} button. */
    static final String MULTI_TEXT = "plain";

    /** HTML slot copied by the {@code #copy-multi} button. */
    static final String MULTI_HTML = "<b>html</b>";

    @Override
    protected void onShow() {
        Input field = new Input();
        field.setId("source");
        NativeButton copyButton = new NativeButton("Copy input value");
        copyButton.setId("copy");
        NativeButton copyStaticButton = new NativeButton("Copy static text");
        copyStaticButton.setId("copy-static");
        NativeButton copyMultiButton = new NativeButton("Copy text + html");
        copyMultiButton.setId("copy-multi");
        Div status = new Div();
        status.setId("status");

        add(field, copyButton, copyStaticButton, copyMultiButton, status);

        Action.Input<String> value = new PropertyInput<>(field, "value",
                String.class);
        new ClickTrigger(copyButton).triggers(new WriteToClipboardAction(value,
                null, copied -> status.setText("ok:" + copied), err -> status
                        .setText("err:" + err.name() + ":" + err.message())));

        new ClickTrigger(copyStaticButton).triggers(new WriteToClipboardAction(
                new LiteralInput<>(STATIC_TEXT), null,
                copied -> status.setText("ok:" + copied), err -> status
                        .setText("err:" + err.name() + ":" + err.message())));

        new ClickTrigger(copyMultiButton).triggers(new WriteToClipboardAction(
                new LiteralInput<>(MULTI_TEXT), new LiteralInput<>(MULTI_HTML),
                copied -> status.setText("ok:" + copied), err -> status
                        .setText("err:" + err.name() + ":" + err.message())));
    }
}
