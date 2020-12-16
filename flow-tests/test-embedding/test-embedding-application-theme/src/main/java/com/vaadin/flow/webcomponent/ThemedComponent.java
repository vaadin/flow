/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.webcomponent;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

public class ThemedComponent extends Div {

    public static final String TEST_TEXT_ID = "test-text";

    public static final String MY_POLYMER_ID = "field";
    public static final String MY_LIT_ID = "button";
    public static final String EMBEDDED_ID = "embedded";

    public ThemedComponent() {
        setId(EMBEDDED_ID);
        final Span textSpan = new Span(
            "Welcome to the embedded application theme test");
        textSpan.setId(TEST_TEXT_ID);

        add(textSpan);

        add(new Div());
        add(new MyPolymerField().withId(MY_POLYMER_ID));

        add(new Div());
        add(new MyLitField().withId(MY_LIT_ID));
    }
}
