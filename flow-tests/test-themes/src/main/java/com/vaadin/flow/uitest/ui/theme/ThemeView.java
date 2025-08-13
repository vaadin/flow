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

package com.vaadin.flow.uitest.ui.theme;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.uitest.ui.theme.ThemeView")
public class ThemeView extends Div {

    public static final String MY_COMPONENT_ID = "field";
    public static final String TEST_TEXT_ID = "test-text";
    public static final String SNOWFLAKE_ID = "fortawesome";
    public static final String BUTTERFLY_ID = "butterfly";
    public static final String OCTOPUSS_ID = "octopuss";
    public static final String FONTAWESOME_ID = "font-awesome";
    public static final String SUB_COMPONENT_ID = "sub-component";
    public static final String DICE_ID = "dice";
    public static final String CSS_SNOWFLAKE = "icon-snowflake";
    public static final String KEYBOARD_ID = "keyboard";
    public static final String LEMON_ID = "lemon";
    public static final String SUN_ID = "sun";
    public static final String LUMO_BORDER_TOP_DIV = "lumo-border-top-div";

    public ThemeView() {
        UI.getCurrent().getPage()
                .executeJs("document.body.classList.add('bg')");
        final Span textSpan = new Span("This is the theme test view");
        textSpan.setId(TEST_TEXT_ID);

        Span subCss = new Span();
        subCss.setId(SUB_COMPONENT_ID);

        Span butterfly = new Span();
        butterfly.setId(BUTTERFLY_ID);

        Span octopuss = new Span();
        octopuss.setId(OCTOPUSS_ID);

        Span cssSnowflake = new Span();
        cssSnowflake.setId(CSS_SNOWFLAKE);

        Span faText = new Span("This test is FontAwesome.");
        faText.setClassName("fas fa-coffee");
        faText.setId(FONTAWESOME_ID);

        // Test npm asset
        Image snowFlake = new Image(
                "VAADIN/static/fortawesome/icons/snowflake.svg", "snowflake");
        snowFlake.setHeight("1em");
        snowFlake.setId(SNOWFLAKE_ID);

        Span diceImageSpan = new Span();
        diceImageSpan.getStyle().set("background-image",
                "url('themes/app-theme/img/dice.jpg')");
        diceImageSpan.setId(DICE_ID);

        add(textSpan, snowFlake, subCss, butterfly, octopuss, cssSnowflake,
                faText, diceImageSpan);

        Div keyboardIconFromParentTheme = new Div(
                "Keyboard Icon in theme sub folder");
        keyboardIconFromParentTheme.addClassName("icon-keyboard");
        keyboardIconFromParentTheme.setId(KEYBOARD_ID);

        Div lemonIconFromParentTheme = new Div(
                "Lemon Icon in nested theme subfolder");
        lemonIconFromParentTheme.addClassName("icon-lemon");
        lemonIconFromParentTheme.setId(LEMON_ID);

        Div sunIconFromParentTheme = new Div(
                "Sun Icon in nested theme subfolder");
        sunIconFromParentTheme.addClassName("icon-sun");
        sunIconFromParentTheme.setId(SUN_ID);
        add(keyboardIconFromParentTheme, lemonIconFromParentTheme,
                sunIconFromParentTheme);

        add(new Div());
        add(new MyComponent().withId(MY_COMPONENT_ID));

        Div lumoBorderDiv = new Div("This element has Lumo border style");
        lumoBorderDiv.addClassName("border-t");
        lumoBorderDiv.setId(LUMO_BORDER_TOP_DIV);
        add(lumoBorderDiv);
    }
}
