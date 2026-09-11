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
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

/**
 * Reproducer from PR #25044 review for the element-locator delegate leak: the
 * delegate re-dispatches a clone to the {@code listenOn} element, and that
 * clone must not leak the shortcut to owners outside the overlay it came from.
 * <p>
 * Open the overlay, focus the field, press Alt+S. The owner inside the overlay
 * ({@code save}, reached through the delegate) must fire; the owner outside any
 * overlay (listening on the UI) must not, even though it would otherwise catch
 * the delegate's bubbling clone.
 */
@Route(value = "com.vaadin.flow.uitest.ui.DelegateLeakView")
public class DelegateLeakView extends Div {

    public static final String EVENT_LOG_ID = "event-log";
    public static final String FIELD_ID = "field";
    public static final String OPEN_ID = "open";
    public static final String INSIDE_FIRED = "inside owner fired";
    public static final String OUTSIDE_FIRED = "outside owner fired";

    public DelegateLeakView() {
        final Div log = new Div();
        log.setId(EVENT_LOG_ID);

        final Input field = new Input();
        field.setId(FIELD_ID);
        final NativeButton save = new NativeButton("Save");

        final Div overlay = new Div(field, save);
        overlay.setId("overlay");
        overlay.getElement().setAttribute("popover", "manual");

        // The component the delegate relays keydowns to; outside the overlay.
        final Div host = new Div(overlay);
        Shortcuts.setShortcutListenOnElement(
                "document.getElementById('overlay')", host);

        // (1) owner INSIDE the overlay, reached through the delegate.
        // Must fire for a keydown in that overlay.
        Shortcuts.addShortcutListener(save,
                () -> log.add(new Div(new Text(INSIDE_FIRED))), Key.KEY_S,
                KeyModifier.ALT).listenOn(host);

        // (2) owner OUTSIDE any overlay, listening on the UI (default).
        // Must NOT fire for that keydown, off the delegate's clone.
        Shortcuts.addShortcutListener(this,
                () -> log.add(new Div(new Text(OUTSIDE_FIRED))), Key.KEY_S,
                KeyModifier.ALT);

        final NativeButton open = new NativeButton("Open overlay",
                e -> overlay.getElement().executeJs("this.showPopover();"));
        open.setId(OPEN_ID);

        add(open, host, log);
        setId("main-div");
    }
}
