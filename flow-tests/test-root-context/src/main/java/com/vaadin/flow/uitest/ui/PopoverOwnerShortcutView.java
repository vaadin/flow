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

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
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
 * <p>
 * The view also carries the opposite case on the same page: an Alt+S shortcut
 * owned by the {@code UI}. The UI element is {@code <body>}, which can never be
 * inside an open popover, so that shortcut must stay silent for a keydown
 * originating in the popover and fire on the top layer. Since #25624 that case
 * is guarded without a per-registration owner token, so it also exercises the
 * token-free client helper.
 */
@Route(value = "com.vaadin.flow.uitest.ui.PopoverOwnerShortcutView")
public class PopoverOwnerShortcutView extends Div {

    public static final String EVENT_LOG_ID = "event-log";
    public static final String OPEN_BUTTON = "open-popover";
    public static final String POPOVER_ID = "popover";
    public static final String FIELD_ID = "field";
    public static final String OUTSIDE_FIELD_ID = "outside-field";
    public static final String SYNC_BUTTON = "sync";
    public static final String SAVED = "saved";
    public static final String UI_SHORTCUT = "ui-shortcut";
    // Logged by the sync button; used by tests as an ordered round-trip
    // barrier.
    public static final String SYNC = "sync-done";

    public static final Key UI_SHORTCUT_KEY = Key.KEY_S;
    public static final KeyModifier UI_SHORTCUT_MODIFIER = KeyModifier.ALT;

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

        final Input outsideField = new Input();
        outsideField.setId(OUTSIDE_FIELD_ID);

        // Plain server round-trip used by tests as an ordered barrier: Flow
        // serializes requests, so once this logs, any earlier shortcut RPC has
        // already been applied.
        final NativeButton sync = new NativeButton("Sync", e -> log(SYNC));
        sync.setId(SYNC_BUTTON);

        add(open, sync, outsideField, popover, eventLog);
        setId("main-div");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        // Owner = the UI, listenOn = the UI: the plain application-wide
        // keyboard shortcut case.
        attachEvent.getUI().addShortcutListener(() -> log(UI_SHORTCUT),
                UI_SHORTCUT_KEY, UI_SHORTCUT_MODIFIER);
    }

    private void log(String source) {
        eventLog.addComponentAsFirst(
                new Div(new Text(counter.getAndIncrement() + "-" + source)));
    }
}
