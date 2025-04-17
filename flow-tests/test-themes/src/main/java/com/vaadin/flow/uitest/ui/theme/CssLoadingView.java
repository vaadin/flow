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

import java.util.HashMap;
import java.util.Map;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

/**
 * A view for testing for CSS priority computing on browser, make sure that the
 * same selectors (with same priority) from different sources (i.e. Application
 * Theme, @CssImport, @StyleSheet, Page.addStyleSheet) are overridden in a
 * correct order.
 * <p>
 * See {@code OrderedDependencyIT} for testing dependency loading order.
 *
 * @author Vaadin Ltd
 */
@Route("com.vaadin.flow.uitest.ui.theme.CssLoadingView")
@CssImport("./css-loading-view/cssimport.css")
@StyleSheet("/styles/stylesheet.css")
public class CssLoadingView extends Div {

    public static class Expected extends Div {

        public Expected(String expectedColor) {
            setClassName("expected");
            getStyle().set("background-color", expectedColor);
        }
    }

    private static final String APP_THEME = "app-theme";
    private static final String PARENT_THEME = "parent-theme";
    private static final String CSSIMPORT = "cssimport";
    private static final String STYLESHEET = "stylesheet";
    private static final String ADD_STYLESHEET = "addstylesheet";
    private static final String ADD_STYLESHEET_LATER = "addstylesheetlater";

    public static Map<String, String> idToExpectedColor = new HashMap<>();
    static {
        idToExpectedColor.put("appThemeVsParent", "green");
        idToExpectedColor.put("parentThemeVsStylesheet", "blue");
        idToExpectedColor.put("stylesheetVsAddStylesheet", "purple");
        idToExpectedColor.put("addStylesheetVsCssImport", "yellow");
        idToExpectedColor.put("laterAddStylesheetVsCssImport", "darkgoldenrod");
        idToExpectedColor.put("cssImportVsLumo", "orange");
        idToExpectedColor.put("lumo", "hsl(214, 35%, 15%)");
    }

    public CssLoadingView() {
        setClassName("margin");

        Span appThemeVsParent = new Span(
                "App theme vs parent, should be green from app theme");
        appThemeVsParent.addClassNames("compare", APP_THEME, PARENT_THEME);
        add(wrap("appThemeVsParent", appThemeVsParent));

        Span parentThemeVsStylesheet = new Span(
                "Parent theme vs @Stylesheet, should be blue from parent theme");
        parentThemeVsStylesheet.addClassNames("compare", PARENT_THEME,
                STYLESHEET);
        add(wrap("parentThemeVsStylesheet", parentThemeVsStylesheet));

        Span stylesheetVsAddStylesheet = new Span(
                "@Stylesheet vs addStylesheet, should be purple from stylesheet");
        stylesheetVsAddStylesheet.addClassNames("compare", STYLESHEET,
                ADD_STYLESHEET);
        add(wrap("stylesheetVsAddStylesheet", stylesheetVsAddStylesheet));

        Span addStylesheetVsCssImport = new Span(
                "addStylesheet vs @CSSImport , should be yellow from add stylesheet");
        addStylesheetVsCssImport.addClassNames("compare", ADD_STYLESHEET,
                CSSIMPORT);
        add(wrap("addStylesheetVsCssImport", addStylesheetVsCssImport));

        Span laterAddStylesheetVsCssImport = new Span(
                "Later addStylesheet vs @CSSImport, should be darkgoldenrod from added stylesheet after the button has been pressed");
        laterAddStylesheetVsCssImport.addClassNames("compare",
                ADD_STYLESHEET_LATER, CSSIMPORT);
        NativeButton load = new NativeButton("Load", e -> {
            getUI().get().getPage()
                    .addStyleSheet("/styles/page-addstylesheet-later.css");
        });
        load.setId("load");
        laterAddStylesheetVsCssImport.add(load);
        add(wrap("laterAddStylesheetVsCssImport",
                laterAddStylesheetVsCssImport));

        Span cssImportVsLumo = new Span(
                "CSSImport vs Lumo, should be orange from cssimport");
        cssImportVsLumo.addClassNames("compare", "bg-contrast-20");
        add(wrap("cssImportVsLumo", cssImportVsLumo));

        Span lumo = new Span("Lumo, should be quite black");
        lumo.addClassNames("compare", "bg-contrast");
        add(wrap("lumo", lumo));

    }

    private Component wrap(String id, Component component) {
        Div container = new Div();
        container.addClassName("container");
        component.setId(id);
        container.add(new Expected(idToExpectedColor.get(id)), component);
        return container;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        attachEvent.getUI().getPage()
                .addStyleSheet("/styles/page-addstylesheet.css");
    }

}
