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
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.router.Route;

/**
 * Test view for color scheme functionality.
 */
@Route("com.vaadin.flow.uitest.ui.theme.ColorSchemeView")
public class ColorSchemeView extends Div {

    public static final String SET_DARK_ID = "set-dark";
    public static final String SET_LIGHT_ID = "set-light";
    public static final String CLEAR_THEME_ID = "clear-theme";
    public static final String COLOR_SCHEME_DISPLAY_ID = "color-scheme-display";
    public static final String THEME_NAME_DISPLAY_ID = "theme-name-display";
    public static final String TEST_ELEMENT_ID = "test-element";

    private final Div colorSchemeDisplay;
    private final Div themeNameDisplay;
    private final Div testElement;

    public ColorSchemeView() {
        // Create buttons to control color scheme
        NativeButton setDarkButton = new NativeButton("Set Dark Theme",
                event -> {
                    getUI().ifPresent(ui -> ui.getPage()
                            .setColorScheme(ColorScheme.Value.DARK));
                    updateDisplays();
                });
        setDarkButton.setId(SET_DARK_ID);

        NativeButton setLightButton = new NativeButton("Set Light Theme",
                event -> {
                    getUI().ifPresent(ui -> ui.getPage()
                            .setColorScheme(ColorScheme.Value.LIGHT));
                    updateDisplays();
                });
        setLightButton.setId(SET_LIGHT_ID);

        // Create display elements
        colorSchemeDisplay = new Div();
        colorSchemeDisplay.setId(COLOR_SCHEME_DISPLAY_ID);

        themeNameDisplay = new Div();
        themeNameDisplay.setId(THEME_NAME_DISPLAY_ID);

        // Create a test element that will have theme-specific styling
        testElement = new Div();
        testElement.setId(TEST_ELEMENT_ID);
        testElement.setText("Test Element");
        testElement.getStyle().set("width", "100px").set("height", "100px");

        add(setDarkButton, setLightButton, colorSchemeDisplay, themeNameDisplay,
                testElement);

        // Update initial displays
        updateDisplays();
    }

    private void updateDisplays() {
        Page page = UI.getCurrentOrThrow().getPage();
        ColorScheme.Value colorScheme = page.getColorScheme();
        String themeName = page.getExtendedClientDetails().getThemeName();

        colorSchemeDisplay.setText("Color Scheme: " + colorScheme.getValue());
        themeNameDisplay.setText("Theme Name: " + themeName);
    }
}
