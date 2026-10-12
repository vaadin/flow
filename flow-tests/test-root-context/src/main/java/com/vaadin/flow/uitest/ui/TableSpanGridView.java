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
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.html.TableRow;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Tables whose spans exercise the corners of the covering grid that
 * {@code TableElement.getCellCovering} resolves: a {@code rowspan} of 0, a
 * {@code rowspan} reaching past the end of its row group, and a row short of a
 * cell so that a slot is left uncovered.
 */
@Route(value = "com.vaadin.flow.uitest.ui.TableSpanGridView", layout = ViewTestLayout.class)
public class TableSpanGridView extends Div {

    public TableSpanGridView() {
        add(new H3("rowspan=0, confined to its own row group"));
        Table toEndOfGroup = new Table();
        toEndOfGroup.setId("to-end-of-group");
        TableRow first = toEndOfGroup.addRow();
        // 0 means "to the end of this row group", so it covers all three body
        // rows and must not reach into the foot
        first.addRowHeaderCell("All").setRowspan(0);
        first.addDataCell("a");
        toEndOfGroup.addRow("b");
        toEndOfGroup.addRow("c");
        toEndOfGroup.addFooterRow("f1", "f2");
        add(toEndOfGroup);

        add(new H3("rowspan reaching past the end of its row group"));
        Table overrunning = new Table();
        overrunning.setId("overrunning");
        TableRow led = overrunning.addRow();
        led.addRowHeaderCell("Long").setRowspan(9);
        led.addDataCell("a");
        overrunning.addRow("b");
        add(overrunning);

        add(new H3("a short row, leaving a slot uncovered"));
        Table ragged = new Table();
        ragged.setId("ragged");
        ragged.addRow("a", "b");
        ragged.addRow("c");
        add(ragged);
    }
}
