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
 * Covers the origin guard of a shortcut whose lifecycle owner is the
 * {@code UI}. The UI element is {@code <body>}, which can never be inside an
 * open popover, so such a shortcut must fire for keydowns on the top layer and
 * stay silent for keydowns originating inside an open popover (#24974).
 * <p>
 * Since #25624 that case is guarded without a per-registration owner token, so
 * this view also exercises the token-free client helper.
 */
@Route(value = "com.vaadin.flow.uitest.ui.UiOwnerShortcutPopoverView")
public class UiOwnerShortcutPopoverView extends Div {

    public static final String EVENT_LOG_ID = "event-log";
    public static final String OPEN_BUTTON = "open-popover";
    public static final String POPOVER_ID = "popover";
    public static final String OUTSIDE_INPUT_ID = "outside-input";
    public static final String POPOVER_INPUT_ID = "popover-input";
    public static final String SYNC_BUTTON = "sync";

    public static final String UI_SHORTCUT = "ui-shortcut";
    // Logged by the sync button; used by tests as an ordered round-trip
    // barrier.
    public static final String SYNC = "sync-done";

    public static final Key SHORTCUT_KEY = Key.KEY_S;
    public static final KeyModifier SHORTCUT_MODIFIER = KeyModifier.ALT;

    private final Div eventLog;
    private final AtomicInteger eventCounter = new AtomicInteger();

    public UiOwnerShortcutPopoverView() {
        eventLog = new Div(new Text("Shortcut events:"));
        eventLog.setId(EVENT_LOG_ID);

        final Input outsideInput = new Input();
        outsideInput.setId(OUTSIDE_INPUT_ID);

        final Input popoverInput = new Input();
        popoverInput.setId(POPOVER_INPUT_ID);
        final Div popover = new Div(new Text("Popover"), popoverInput);
        popover.setId(POPOVER_ID);
        popover.getElement().setAttribute("popover", "manual");

        final NativeButton open = new NativeButton("Open popover",
                e -> popover.getElement().executeJs("this.showPopover();"));
        open.setId(OPEN_BUTTON);

        // Plain server round-trip used by tests as an ordered barrier: Flow
        // serializes requests, so once this logs, any earlier shortcut RPC has
        // already been applied.
        final NativeButton sync = new NativeButton("Sync", e -> log(SYNC));
        sync.setId(SYNC_BUTTON);

        add(open, sync, outsideInput, popover, eventLog);
        setId("main-div");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        // Owner = the UI, listenOn = the UI: the plain application-wide
        // keyboard shortcut case.
        attachEvent.getUI().addShortcutListener(() -> log(UI_SHORTCUT),
                SHORTCUT_KEY, SHORTCUT_MODIFIER);
    }

    private void log(String source) {
        eventLog.addComponentAsFirst(new Div(
                new Text(eventCounter.getAndIncrement() + "-" + source)));
    }
}
