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
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

/**
 * Reproduces issue #24974 using the same shortcut wiring as the real
 * {@code Dialog} component: the shortcut listens on a server-side component
 * ({@code parentDialog}) while a browser-only overlay element relays keydown
 * events to it via {@link Shortcuts#setShortcutListenOnElement} (the
 * {@code ELEMENT_LOCATOR_JS} delegate).
 * <p>
 * In Vaadin 25 the nested overlay is a native popover and a real DOM descendant
 * of the parent overlay (in Vaadin 24 overlays were teleported to be siblings).
 * A keydown inside the nested overlay therefore bubbles up to the parent
 * overlay's relay listener, which clones and re-dispatches it to the parent
 * dialog, firing the parent shortcut even though the nested overlay has focus.
 */
@Route(value = "com.vaadin.flow.uitest.ui.NestedPopoverShortcutView")
public class NestedPopoverShortcutView extends Div {

    public static final String EVENT_LOG_ID = "event-log";
    public static final String OPEN_PARENT_BUTTON = "open-parent";
    public static final String OPEN_NESTED_BUTTON = "open-nested";
    public static final String PARENT_OVERLAY_ID = "parent-overlay";
    public static final String NESTED_OVERLAY_ID = "nested-overlay";
    public static final String PARENT_INPUT_ID = "parent-input";
    public static final String NESTED_INPUT_ID = "nested-input";

    // Shortcut fired by the parent dialog, logged with this source label.
    public static final String PARENT_SHORTCUT = "parent-shortcut";

    public static final Key SHORTCUT_KEY = Key.KEY_S;
    public static final KeyModifier SHORTCUT_MODIFIER = KeyModifier.ALT;

    private final Div eventLog;
    private final AtomicInteger eventCounter = new AtomicInteger();

    // Server-side component the shortcut listens on (like <vaadin-dialog>).
    private final Div parentDialog = new Div();
    // Top-layer overlay elements (like <vaadin-dialog-overlay>).
    private final Div parentOverlay;
    private final Div nestedOverlay;

    public NestedPopoverShortcutView() {
        eventLog = new Div(new Text("Shortcut events:"));
        eventLog.setId(EVENT_LOG_ID);

        final Input parentInput = new Input();
        parentInput.setId(PARENT_INPUT_ID);
        parentOverlay = new Div(new Text("Parent overlay"), parentInput);
        parentOverlay.setId(PARENT_OVERLAY_ID);
        parentOverlay.getElement().setAttribute("popover", "manual");

        final Input nestedInput = new Input();
        nestedInput.setId(NESTED_INPUT_ID);
        nestedOverlay = new Div(new Text("Nested overlay"), nestedInput);
        nestedOverlay.setId(NESTED_OVERLAY_ID);
        nestedOverlay.getElement().setAttribute("popover", "manual");
        // Native popover nesting: the nested overlay is a DOM child of the
        // parent overlay, exactly like a nested dialog in Vaadin 25.
        parentOverlay.add(nestedOverlay);

        // Relay keydown events from the parent overlay element to parentDialog,
        // matching how Dialog wires shortcuts to its overlay.
        Shortcuts.setShortcutListenOnElement(
                "document.getElementById('" + PARENT_OVERLAY_ID + "')",
                parentDialog);
        Shortcuts.addShortcutListener(parentDialog, () -> log(PARENT_SHORTCUT),
                SHORTCUT_KEY, SHORTCUT_MODIFIER).listenOn(parentDialog);

        final NativeButton openParent = new NativeButton("Open parent overlay",
                e -> parentOverlay.getElement()
                        .executeJs("this.showPopover();"));
        openParent.setId(OPEN_PARENT_BUTTON);
        final NativeButton openNested = new NativeButton("Open nested overlay",
                e -> nestedOverlay.getElement()
                        .executeJs("this.showPopover();"));
        openNested.setId(OPEN_NESTED_BUTTON);

        add(openParent, openNested, parentDialog, parentOverlay, eventLog);
        setId("main-div");
    }

    private void log(String source) {
        eventLog.addComponentAsFirst(new Div(
                new Text(eventCounter.getAndIncrement() + "-" + source)));
    }
}
