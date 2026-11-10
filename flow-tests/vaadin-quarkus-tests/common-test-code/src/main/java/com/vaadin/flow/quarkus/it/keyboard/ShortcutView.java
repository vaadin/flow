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

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

/**
 * Listens to a keyboard shortcut. Flow reads the client code of the shortcut
 * from a classpath resource at runtime.
 */
@Route("shortcut")
public class ShortcutView extends Div {

    public static final String STATUS_ID = "status";
    public static final String TRIGGERED = "Shortcut triggered";

    public ShortcutView() {
        Span status = new Span();
        status.setId(STATUS_ID);
        Shortcuts.addShortcutListener(this, () -> status.setText(TRIGGERED),
                Key.KEY_K);
        add(status);
    }
}
