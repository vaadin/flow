/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Shows what the opt-in table stylesheet does to a native
 * <code>&lt;table&gt;</code>, both on its own and through each of the
 * {@code theme} variants it supports.
 * <p>
 * The whole opt-in is the {@code @StyleSheet} below; nothing on the tables
 * themselves asks to be styled.
 */
@Route(value = "com.vaadin.flow.uitest.ui.AuraTableStylesView", layout = ViewTestLayout.class)
@StyleSheet(Table.AURA_STYLESHEET)
public class AuraTableStylesView extends Div {

    public AuraTableStylesView() {
        add(new H2("Native tables with the opt-in table stylesheet"));

        add(new H3("Plain table with a caption, a header and a footer"));
        add(planets("default-table", null));

        add(new H3("theme=\"row-stripes\""));
        add(planets("row-stripes-table", "row-stripes"));

        add(new H3("theme=\"column-borders\""));
        add(planets("column-borders-table", "column-borders"));

        add(new H3("theme=\"no-row-borders\""));
        add(planets("no-row-borders-table", "no-row-borders"));

        add(new H3("theme=\"compact\""));
        add(planets("compact-table", "compact"));
    }

    private static Table planets(String id, String themeVariant) {
        Table table = new Table();
        table.setId(id);
        table.setCaptionText("Planets of the inner solar system");
        table.addHeaderRow("Name", "Mass (10²⁴kg)", "Diameter (km)", "Moons");
        table.addRowWithHeader("Mercury", "0.330", "4,879", "0");
        table.addRowWithHeader("Venus", "4.87", "12,104", "0");
        table.addRowWithHeader("Earth", "5.97", "12,756", "1");
        table.addRowWithHeader("Mars", "0.642", "6,792", "2");
        table.addFooterRow("Total", "11.81", "36,531", "3");
        if (themeVariant != null) {
            table.getElement().getThemeList().add(themeVariant);
        }
        return table;
    }
}
