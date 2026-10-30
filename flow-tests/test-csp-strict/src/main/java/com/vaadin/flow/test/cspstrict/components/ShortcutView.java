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
package com.vaadin.flow.test.cspstrict.components;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

/**
 * A keyboard shortcut: the registration builds the key filter at runtime, which
 * the strict mode rejects until the shortcut listener is declared.
 */
@Route("shortcut")
public class ShortcutView extends Div {

    public ShortcutView() {
        Input input = new Input();
        input.setId("input");
        Span fired = new Span();
        fired.setId("fired");
        add(input, fired);
        Shortcuts.addShortcutListener(this, () -> fired.setText("Ctrl+S fired"),
                Key.KEY_S, KeyModifier.CONTROL);
    }
}
