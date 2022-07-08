/*
 * Copyright 2000-2022 Vaadin Ltd.
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
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.LoadMode;

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
@CssImport("./src/css-loading-view.css")
@StyleSheet("context://styles/stylesheet.css")
public class CssLoadingView extends Div {

    public CssLoadingView() {
        UI.getCurrent().getPage().addStyleSheet("/styles/page-stylesheet.css",
                LoadMode.INLINE);

        Paragraph p1 = new Paragraph(
                "Having @CssImport and Page.addStylesheet()");
        p1.setId("p1");
        p1.addClassNames("css-import", "page-add-stylesheet");

        Paragraph p2 = new Paragraph(
                "@CssImport, Page.addStylesheet() and @Stylesheet");
        p2.setId("p2");
        p2.addClassNames("css-import", "page-add-stylesheet", "stylesheet");

        Paragraph p3 = new Paragraph(
                "Having App Theme, @CssImport, Page.addStylesheet() and @Stylesheet");
        p3.setId("p3");
        p3.addClassNames("css-import", "page-add-stylesheet", "stylesheet",
                "global");

        add(p1, p2, p3);
        setId("styled-components");
    }
}
