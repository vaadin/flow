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
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.dom.ShadowRoot;
import com.vaadin.flow.router.Route;

/**
 * Regression view for issue #25213: the keydown originates on the element that
 * <em>hosts</em> the popover in its shadow root, while the shortcut owner is
 * slotted into that popover.
 * <p>
 * This mirrors the structure of {@code Dialog}: the overlay is a popover inside
 * the {@code <vaadin-dialog>} shadow root, the dialog content is slotted into
 * it, and the focus trap focuses the host element itself right after opening.
 * The host is not in the popover's flattened ancestor chain, so the event scope
 * resolves to {@code null} while the owner scope resolves to the popover - the
 * event comes from a shallower scope than the owner, not from a nested one.
 */
@Route(value = "com.vaadin.flow.uitest.ui.PopoverHostFocusShortcutView")
public class PopoverHostFocusShortcutView extends Div {

    public static final String EVENT_LOG_ID = "event-log";
    public static final String OPEN_BUTTON = "open-dialog";
    public static final String HOST_ID = "dialog-host";
    public static final String FIELD_ID = "field";
    public static final String CONFIRMED = "confirmed";

    private final Div eventLog;
    private final AtomicInteger counter = new AtomicInteger();

    public PopoverHostFocusShortcutView() {
        eventLog = new Div(new Text("Shortcut events:"));
        eventLog.setId(EVENT_LOG_ID);

        // Host of the overlay, focusable like <vaadin-dialog>.
        final Div host = new Div();
        host.setId(HOST_ID);
        host.getElement().setAttribute("tabindex", "0");

        final ShadowRoot shadowRoot = host.getElement().attachShadow();
        final Element overlay = ElementFactory.createDiv();
        overlay.setAttribute("popover", "manual");
        overlay.appendChild(new Element("slot"));
        shadowRoot.appendChild(overlay);

        // Light DOM content, slotted into the overlay in the flattened tree.
        final Input field = new Input();
        field.setId(FIELD_ID);

        final NativeButton ok = new NativeButton("Ok", e -> log(CONFIRMED));
        // Owner = Ok button (slotted into the overlay); listenOn = UI by
        // default.
        ok.addClickShortcut(Key.ENTER);

        host.add(field, ok);

        final NativeButton open = new NativeButton("Open dialog",
                e -> overlay.executeJs(
                        "this.showPopover(); this.getRootNode().host.focus();"));
        open.setId(OPEN_BUTTON);

        add(open, host, eventLog);
        setId("main-div");
    }

    private void log(String source) {
        eventLog.addComponentAsFirst(
                new Div(new Text(counter.getAndIncrement() + "-" + source)));
    }
}
