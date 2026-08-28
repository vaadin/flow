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
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.html.TableColumn;
import com.vaadin.flow.component.html.TableColumnGroup;
import com.vaadin.flow.component.html.TableHeaderCell.Scope;
import com.vaadin.flow.component.html.TableRow;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Replicates the examples from the MDN "HTML table basics" tutorial:
 * https://developer.mozilla.org/en-US/docs/Learn_web_development/Core/Structuring_content/HTML_table_basics
 * <p>
 * The examples are added as the table API they need becomes available; the ones
 * still missing need {@code colspan}/{@code rowspan} on <code>&lt;th&gt;</code>
 * and caption content API.
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

        add(new H3("1. Basic table with rows and cells"));
        BasicTable basic = new BasicTable();
        basic.setId("basic-table");
        add(basic);

        add(new H3("2. Adding headers with <th>"));
        DogsTable dogs = new DogsTable();
        dogs.setId("dogs-table");
        add(dogs);

        add(new H3("5. School timetable styled with <colgroup>/<col>"));
        SchoolTimetable timetable = new SchoolTimetable();
        timetable.setId("school-timetable");
        add(timetable);
    }

    /** Example 1: minimal 2-row, 4-column table — no header. */
    static class BasicTable extends Table {
        {
            addRow("Hi, I'm your first cell.", "I'm your second cell.",
                    "I'm your third cell.", "I'm your fourth cell.");
            addRow("Second row, first cell.", "Cell 2.", "Cell 3.", "Cell 4.");
        }
    }

    /** Example 2: row-headers and column-headers in the same table. */
    static class DogsTable extends Table {
        {
            TableRow headerRow = addRow();
            headerRow.addDataCell();
            for (String name : new String[] { "Knocky", "Flor", "Ella",
                    "Juan" }) {
                headerRow.addHeaderCell(name).setScope(Scope.COL);
            }
            addRowWithRowHeader("Breed", "Jack Russell", "Poodle", "Streetdog",
                    "Cocker Spaniel");
            addRowWithRowHeader("Age", "16", "9", "10", "5");
            addRowWithRowHeader("Owner", "Mother-in-law", "Me", "Me",
                    "Sister-in-law");
            addRowWithRowHeader("Eating habits", "Eats everyone's leftovers",
                    "Nibbles at food", "Hearty eater",
                    "Will eat till he explodes");
        }

        private void addRowWithRowHeader(String header, String... data) {
            TableRow row = addRow();
            row.addHeaderCell(header).setScope(Scope.ROW);
            row.addDataCells(data);
        }
    }

    /**
     * Example 5: school timetable, demonstrating column-level styling via
     * {@code <colgroup>}/{@code <col>}.
     */
    static class SchoolTimetable extends Table {
        {
            // Mirrors the MDN colgroup: 2 plain cols, then a sequence of styled
            // cols, ending with a 2-col fixed-width group on the right.
            TableColumnGroup group = addColumnGroup();
            group.addColumn(2);
            group.addColumn().addClassName("column-background");
            group.addColumn().addClassName("column-fixed-width");
            group.addColumn().addClassName("column-background");
            group.addColumn().addClassName("column-background-border");
            TableColumn rightPair = group.addColumn(2);
            rightPair.addClassName("column-fixed-width");

            TableRow header = addRow();
            header.addDataCell();
            header.addHeaderCells("Mon", "Tues", "Wed", "Thurs", "Fri", "Sat",
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
            TableRow row = addRow();
            row.addHeaderCell(period).setScope(Scope.ROW);
            row.addDataCells(days);
        }
    }
}
