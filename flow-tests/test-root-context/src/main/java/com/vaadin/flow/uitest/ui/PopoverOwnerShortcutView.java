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

import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

/**
 * Regression view for issue #24974: a shortcut whose lifecycle owner lives
 * <em>inside</em> a popover must still fire for keydowns originating in that
 * same popover, even though the shortcut listens on the UI (body) by default.
 * <p>
 * Mirrors the reported {@code Popover} case: a Save button inside the popover
 * with {@code addClickShortcut(ENTER)}. The origin guard must recognise that
 * the event and the shortcut owner share the same popover scope and let it
 * fire, rather than treating the popover as a boundary to suppress.
 */
@Route(value = "com.vaadin.flow.uitest.ui.PopoverOwnerShortcutView")
public class PopoverOwnerShortcutView extends Div {

    public static final String EVENT_LOG_ID = "event-log";
    public static final String OPEN_BUTTON = "open-popover";
    public static final String OUTSIDE_FIELD_ID = "outside-field";
    public static final String POPOVER_ID = "popover";
    public static final String FIELD_ID = "field";
    public static final String SAVED = "saved";

    private final Div eventLog;
    private final AtomicInteger counter = new AtomicInteger();

    public PopoverOwnerShortcutView() {
        eventLog = new Div(new Text("Shortcut events:"));
        eventLog.setId(EVENT_LOG_ID);

        final Input field = new Input();
        field.setId(FIELD_ID);

        final NativeButton save = new NativeButton("Save", e -> log(SAVED));
        // Owner = save button (inside the popover); listenOn = UI by default.
        save.addClickShortcut(Key.ENTER);

        final Div popover = new Div(new Text("Popover"), field, save);
        popover.setId(POPOVER_ID);
        popover.getElement().setAttribute("popover", "manual");

        final NativeButton open = new NativeButton("Open popover",
                e -> popover.getElement().executeJs("this.showPopover();"));
        open.setId(OPEN_BUTTON);

        // Focusable element outside the popover. Reproduces #25213: with focus
        // here (a scope shallower than the owner's popover scope, mirroring a
        // modal Dialog host whose overlay lives in shadow DOM) the shortcut must
        // still fire, because the event scope is an ancestor of the owner scope.
        final Input outsideField = new Input();
        outsideField.setId(OUTSIDE_FIELD_ID);

        add(open, outsideField, popover, eventLog);
        setId("main-div");
    }

    private void log(String source) {
        eventLog.addComponentAsFirst(
                new Div(new Text(counter.getAndIncrement() + "-" + source)));
    }
}
