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
package com.example.application;

import org.vaadin.addon.greeting.GreetingBox;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

/**
 * Runs a definition of an add-on, whose package the application does not scan.
 */
@Route("addon-definition")
public class AddonDefinitionView extends Div {

    public static final String GREET_ID = "greet";
    public static final String GREETING_ID = "greeting";

    public AddonDefinitionView() {
        GreetingBox greetingBox = new GreetingBox();
        greetingBox.setId(GREETING_ID);
        NativeButton greet = new NativeButton("Greet from the add-on",
                event -> greetingBox.greet("native").then(String.class,
                        greetingBox::setText));
        greet.setId(GREET_ID);
        add(greet, greetingBox);
    }
}
