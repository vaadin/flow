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
package com.vaadin.flow.quarkus.it.keyboard;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

/**
 * Focuses an input through {@code Focusable.focus()}, which runs a JavaScript
 * definition of Flow itself.
 */
@Route("focus")
public class FocusView extends Div {

    public static final String INPUT_ID = "input";
    public static final String FOCUS_ID = "focus";

    public FocusView() {
        Input input = new Input();
        input.setId(INPUT_ID);
        NativeButton focus = new NativeButton("Focus the input",
                event -> input.focus());
        focus.setId(FOCUS_ID);
        add(input, focus);
    }
}
