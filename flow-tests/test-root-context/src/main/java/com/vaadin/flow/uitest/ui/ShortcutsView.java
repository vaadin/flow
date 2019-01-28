/*
 * Copyright 2000-2019 Vaadin Ltd.
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
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ShortcutsView",
        layout = ViewTestLayout.class)
public class ShortcutsView extends Div {

    private Paragraph invisibleP = new Paragraph("invisible");
    private boolean attached = false;

    private ShortcutRegistration flipFloppingRegistration;

    public ShortcutsView() {
        Paragraph expected = new Paragraph();
        expected.setId("expected");
        expected.setText("testing...");

        // clickShortcutWorks
        NativeButton button = new NativeButton();
        button.setId("button");
        button.addClickListener(e -> expected.setText("button"));
        button.addClickShortcut(Key.KEY_B, KeyModifier.ALT);

        // focusShortcutWorks
        Input input = new Input();
        input.setId("input");
        input.addFocusShortcut(Key.KEY_F, KeyModifier.ALT);

        // shortcutsOnlyWorkWhenComponentIsVisible
        UI.getCurrent().addShortcutListener(() -> {
            invisibleP.setVisible(!invisibleP.isVisible());
            expected.setText("toggled!");
        }, Key.KEY_I, KeyModifier.ALT);

        Shortcuts.addShortcutListener(invisibleP, () -> expected
                .setText("invisibleP"), Key.KEY_V).withAlt();

        add(expected, button, input, invisibleP);

        // listenOnScopesTheShortcut
        Div subview = new Div();
        subview.setId("subview");

        Input focusTarget = new Input();
        focusTarget.setId("focusTarget");

        subview.add(focusTarget);

        Shortcuts.addShortcutListener(subview,
                () -> expected.setText("subview"), Key.KEY_S, KeyModifier.ALT)
                .listenOn(subview);

        add(subview);

        // shortcutsOnlyWorkWhenComponentIsAttached
        Paragraph attachable = new Paragraph("attachable");
        attachable.setId("attachable");

        Shortcuts.addShortcutListener(attachable, () -> expected
                .setText("attachable"), Key.KEY_A).withAlt();

        UI.getCurrent().addShortcutListener(() -> {
            attached = !attached;
            if (attached) {
                add(attachable);
            }
            else {
                remove(attachable);
            }
            expected.setText("toggled!");
        }, Key.KEY_Y, KeyModifier.ALT);

        // modifyingShortcutShouldChangeShortcutEvent
        flipFloppingRegistration =
                UI.getCurrent().addShortcutListener(event -> {
                    if (event.getKeyModifiers().contains(KeyModifier.ALT)) {
                        expected.setText("Alt");
                        flipFloppingRegistration.withModifiers(
                                KeyModifier.SHIFT);
                    } else if (event.getKeyModifiers().contains(
                            KeyModifier.SHIFT)) {
                        expected.setText("Shift");
                        flipFloppingRegistration.withModifiers(KeyModifier.ALT);
                    }
                    else {
                        expected.setText("Failed");
                    }

                }, Key.KEY_G, KeyModifier.ALT);
    }
}
