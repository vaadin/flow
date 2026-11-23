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
package com.vaadin.flow.test.cspoff.components;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.router.Route;

/**
 * Forwards the keydown events of an element outside a component to the
 * shortcuts that listen on the component, with a locator given as a string,
 * which the browser compiles. The locator finds the element from the listen-on
 * element, which it gets as {@code this}.
 */
@Route("shortcut-locator")
public class ShortcutLocatorView extends Div {

    public static final String OUTSIDE_INPUT_ID = "outside-input";
    public static final String LOCATED_INPUT_ID = "located-input";
    public static final String RESULT_ID = "result";

    private int count;

    @SuppressWarnings("deprecation")
    public ShortcutLocatorView() {
        Div result = new Div("0");
        result.setId(RESULT_ID);

        Input outsideInput = new Input();
        outsideInput.setId(OUTSIDE_INPUT_ID);

        Div host = new Div();

        // Stands for an overlay that a component renders outside of itself
        Input locatedInput = new Input();
        locatedInput.setId(LOCATED_INPUT_ID);
        Div located = new Div(locatedInput);

        add(outsideInput, host, located, result);

        Shortcuts.setShortcutListenOnElement("this.nextElementSibling", host);
        Shortcuts.addShortcutListener(host,
                () -> result.setText(String.valueOf(++count)), Key.KEY_X)
                .listenOn(host);
    }
}
