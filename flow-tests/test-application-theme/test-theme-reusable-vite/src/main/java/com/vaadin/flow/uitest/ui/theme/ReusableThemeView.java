/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.theme;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.uitest.ui.theme.ReusableThemeView")
public class ReusableThemeView extends Div {

    public static final String MY_COMPONENT_ID = "field";
    public static final String TEST_TEXT_ID = "test-text";
    public static final String SNOWFLAKE_ID = "fortawesome";
    public static final String BUTTERFLY_ID = "butterfly";
    public static final String OCTOPUSS_ID = "octopuss";
    public static final String FONTAWESOME_ID = "font-awesome";
    public static final String SUB_COMPONENT_ID = "sub-component";

    public ReusableThemeView() {
        final Span textSpan = new Span("This is the theme test view");
        textSpan.setId(TEST_TEXT_ID);

        Span subCss = new Span();
        subCss.setId(SUB_COMPONENT_ID);

        Span butterfly = new Span();
        butterfly.setId(BUTTERFLY_ID);

        Span octopuss = new Span();
        octopuss.setId(OCTOPUSS_ID);

        Span faText = new Span("This test is FontAwesome.");
        faText.setClassName("fas fa-coffee");
        faText.setId(FONTAWESOME_ID);

        Image snowFlake = new Image(
                "themes/reusable-theme/fortawesome/icons/snowflake.svg",
                "snowflake");
        snowFlake.setHeight("1em");
        snowFlake.setId(SNOWFLAKE_ID);

        add(textSpan, snowFlake, subCss, butterfly, octopuss, faText);

        add(new Div());
        add(new MyComponent().withId(MY_COMPONENT_ID));
    }
}
