/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
