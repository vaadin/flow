/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import org.apache.commons.lang3.SerializationUtils;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.SerializeShortcutView", layout = ViewTestLayout.class)
public class SerializeShortcutView extends AbstractDivView {
    public SerializeShortcutView() {
        Div label = new Div();
        label.setId("message");

        Input input = new Input();
        Shortcuts.addShortcutListener(input, event -> input.focus(), Key.KEY_F,
                KeyModifier.META);

        NativeButton button1 = createButton("Add Shortcut Owner",
                "add-serialize", event -> {
                    UI ui = UI.getCurrent();
                    add(input);
                    serializedAndDeserialize(label, ui);
                });
        NativeButton button2 = createButton("Add Remove Shortcut Owner",
                "add-remove-serialize", event -> {
                    UI ui = UI.getCurrent();
                    add(input);
                    remove(input);
                    serializedAndDeserialize(label, ui);
                });
        add(label, button1, button2);
    }

    private static void serializedAndDeserialize(Div label, UI ui) {
        try {
            UI newUI = SerializationUtils
                    .deserialize(SerializationUtils.serialize(ui));
            String result = newUI != null ? "Successfully serialized ui"
                    : "Serialization failed";
            label.setText(result);
        } catch (Exception se) {
            label.setText(se.getMessage());
        }
    }
}
