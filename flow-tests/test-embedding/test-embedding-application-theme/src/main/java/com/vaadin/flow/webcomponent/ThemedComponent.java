/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
    }
}
