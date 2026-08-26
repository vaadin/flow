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
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.NativeTable;
import com.vaadin.flow.component.html.NativeTableCell;
import com.vaadin.flow.component.html.NativeTableColumn;
import com.vaadin.flow.component.html.NativeTableColumnGroup;
import com.vaadin.flow.component.html.NativeTableHeaderCell;
import com.vaadin.flow.component.html.NativeTableRow;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Replicates the examples from the MDN "HTML table basics" tutorial:
 * https://developer.mozilla.org/en-US/docs/Learn_web_development/Core/Structuring_content/HTML_table_basics
 * <p>
 * Only the {@code <colgroup>}/{@code <col>} example is present so far; the
 * remaining examples need table API that does not exist yet.
 */
@Route(value = "com.vaadin.flow.uitest.ui.HtmlTableTutorialView", layout = ViewTestLayout.class)
public class HtmlTableTutorialView extends Div {

    public HtmlTableTutorialView() {
        Element style = new Element("style");
        style.setText("""
                table {
                  border-collapse: collapse;
                  border: 2px solid rgb(200 200 200);
                  letter-spacing: 1px;
                  font-size: 0.8rem;
                }

                td,
                th {
                  border: 1px solid rgb(190 190 190);
                  padding: 10px 20px;
                }

                td {
                  text-align: center;
                }
                .column-background {
                  background-color: #97db9a;
                }

                .column-fixed-width {
                  width: 40px;
                }

                .column-background-border {
                  background-color: #dcc48e;
                  border: 4px solid #c1437a;
                }
                """);
        getElement().appendChild(style);

        add(new H2("HTML table basics — examples from MDN"));

        add(new H3("5. School timetable styled with <colgroup>/<col>"));
        SchoolTimetable timetable = new SchoolTimetable();
        timetable.setId("school-timetable");
        add(timetable);
    }

    /**
     * Example 5: school timetable, demonstrating column-level styling via
     * {@code <colgroup>}/{@code <col>}.
     */
    static class SchoolTimetable extends NativeTable {
        {
            // Mirrors the MDN colgroup: 2 plain cols, then a sequence of styled
            // cols, ending with a 2-col fixed-width group on the right.
            NativeTableColumnGroup group = addColumnGroup();
            group.addColumn(2);
            group.addColumn().addClassName("column-background");
            group.addColumn().addClassName("column-fixed-width");
            group.addColumn().addClassName("column-background");
            group.addColumn().addClassName("column-background-border");
            NativeTableColumn rightPair = group.addColumn(2);
            rightPair.addClassName("column-fixed-width");

            NativeTableRow header = addRow();
            header.addDataCell();
            addHeaderCells(header, "Mon", "Tues", "Wed", "Thurs", "Fri", "Sat",
                    "Sun");

            addPeriodRow("1st period", "English", "", "", "German", "Dutch", "",
                    "");
            addPeriodRow("2nd period", "English", "English", "", "German",
                    "Dutch", "", "");
            addPeriodRow("3rd period", "", "German", "German", "", "Dutch", "",
                    "");
            addPeriodRow("4th period", "", "English", "English", "", "Dutch",
                    "", "");
        }

        private void addPeriodRow(String period, String... days) {
            NativeTableRow row = addRow();
            addHeaderCells(row, period);
            addDataCells(row, days);
        }

        /**
         * Appends a row to the table body.
         * <p>
         * Stands in for {@code NativeTable.addRow()}.
         */
        private NativeTableRow addRow() {
            return getBody().addRow();
        }

        /**
         * Appends a header cell per text.
         * <p>
         * Stands in for {@code NativeTableRow.addHeaderCells(String...)}. The
         * MDN example also gives these cells a {@code scope}, which needs
         * {@code NativeTableHeaderCell.setScope}.
         */
        private static void addHeaderCells(NativeTableRow row,
                String... texts) {
            for (String text : texts) {
                row.add(new NativeTableHeaderCell(text));
            }
        }

        /**
         * Appends a data cell per text.
         * <p>
         * Stands in for {@code NativeTableRow.addDataCells(String...)}.
         */
        private static void addDataCells(NativeTableRow row, String... texts) {
            for (String text : texts) {
                row.add(new NativeTableCell(text));
            }
        }
    }
}
