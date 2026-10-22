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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ClearAndReplaceChildrenView", layout = ViewTestLayout.class)
public class ClearAndReplaceChildrenView extends AbstractDivView {

    private final Div content = new Div();

    public ClearAndReplaceChildrenView() {
        Div scroller = new Div(content);
        scroller.setId("scroller");
        scroller.getStyle().set("height", "200px").set("overflow", "auto");

        fillContent();

        add(createButton("Replace content", "replace", event -> fillContent()),
                scroller);
    }

    private void fillContent() {
        content.removeAll();

        Div rows = new Div();
        for (int i = 1; i <= 100; i++) {
            Div row = new Div("Row " + i);
            row.getStyle().set("height", "30px");
            rows.add(row);
        }
        content.add(rows);
    }
}
