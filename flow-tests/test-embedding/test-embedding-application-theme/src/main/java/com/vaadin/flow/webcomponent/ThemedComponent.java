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
package com.vaadin.flow.webcomponent;

import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.uitest.ui.dependencies.TestVersion;

@NpmPackage(value = "@fortawesome/fontawesome-free", version = TestVersion.FONTAWESOME)
public class ThemedComponent extends Div {

    public static final String TEST_TEXT_ID = "test-text";

    public static final String MY_COMPONENT_ID = "field";
    public static final String CSS_IMPORT_COMPONENT_ID = "embedded-cssimport";
    public static final String EMBEDDED_ID = "embedded";

    public static final String HAND_ID = "sparkle-hand";

    public ThemedComponent() {
        setId(EMBEDDED_ID);
        final Span textSpan = new Span(
                "Welcome to the embedded application theme test");
        textSpan.setId(TEST_TEXT_ID);

        Span hand = new Span();
        hand.setId(HAND_ID);
        hand.addClassNames("internal", "fas", "fa-hand-sparkles");

        add(textSpan, hand);

        add(new Div());
        add(new MyComponent().withId(MY_COMPONENT_ID));
        add(new CssImportComponent(CSS_IMPORT_COMPONENT_ID));
    }
}
