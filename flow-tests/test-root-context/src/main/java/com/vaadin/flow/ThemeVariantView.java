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
package com.vaadin.flow;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.ThemeVariantView")
public class ThemeVariantView extends Div {

    private Span currentThemeSpan;

    public ThemeVariantView() {
        // Display initial theme variant
        String initialTheme = UI.getCurrent().getPage().getThemeVariant();
        Span initialSpan = new Span("Initial theme: "
                + (initialTheme.isEmpty() ? "(empty)" : initialTheme));
        initialSpan.setId("initial-theme");
        add(initialSpan);

        // Display current theme variant
        currentThemeSpan = new Span("Current theme: "
                + (initialTheme.isEmpty() ? "(empty)" : initialTheme));
        currentThemeSpan.setId("current-theme");
        add(currentThemeSpan);

        // Button to set theme to "dark"
        NativeButton setDarkButton = new NativeButton("Set Dark Theme",
                event -> {
                    UI.getCurrent().getPage().setThemeVariant("dark");
                    updateCurrentTheme();
                });
        setDarkButton.setId("set-dark");
        add(setDarkButton);

        // Button to set theme to "light"
        NativeButton setLightButton = new NativeButton("Set Light Theme",
                event -> {
                    UI.getCurrent().getPage().setThemeVariant("light");
                    updateCurrentTheme();
                });
        setLightButton.setId("set-light");
        add(setLightButton);

        // Button to clear theme
        NativeButton clearThemeButton = new NativeButton("Clear Theme",
                event -> {
                    UI.getCurrent().getPage().setThemeVariant(null);
                    updateCurrentTheme();
                });
        clearThemeButton.setId("clear-theme");
        add(clearThemeButton);

        // Button to get theme from browser
        NativeButton getThemeButton = new NativeButton("Get Theme", event -> {
            updateCurrentTheme();
        });
        getThemeButton.setId("get-theme");
        add(getThemeButton);
    }

    private void updateCurrentTheme() {
        String currentTheme = UI.getCurrent().getPage().getThemeVariant();
        currentThemeSpan.setText("Current theme: "
                + (currentTheme.isEmpty() ? "(empty)" : currentTheme));
    }
}
