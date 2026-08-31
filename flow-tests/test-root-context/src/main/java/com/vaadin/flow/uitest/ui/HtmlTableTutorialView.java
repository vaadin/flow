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

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.html.TableColumn;
import com.vaadin.flow.component.html.TableColumnGroup;
import com.vaadin.flow.component.html.TableHead;
import com.vaadin.flow.component.html.TableHeaderCell;
import com.vaadin.flow.component.html.TableHeaderCell.Scope;
import com.vaadin.flow.component.html.TableRow;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Replicates the examples from the MDN "HTML table basics" tutorial:
 * https://developer.mozilla.org/en-US/docs/Learn_web_development/Core/Structuring_content/HTML_table_basics
 * <p>
 * All five examples from the tutorial are covered.
 */
@Route(value = "com.vaadin.flow.uitest.ui.HtmlTableTutorialView", layout = ViewTestLayout.class)
@StyleSheet("./html-table-tutorial.css")
public class HtmlTableTutorialView extends Div {

    public HtmlTableTutorialView() {
        add(new H2("HTML table basics — examples from MDN"));

        add(new H3("1. Basic table with rows and cells"));
        BasicTable basic = new BasicTable();
        basic.setId("basic-table");
        add(basic);

        add(new H3("2. Adding headers with <th>"));
        DogsTable dogs = new DogsTable();
        dogs.setId("dogs-table");
        add(dogs);

        add(new H3("3. Allowing cells to span multiple rows and columns"));
        AnimalsTable animals = new AnimalsTable();
        animals.setId("animals-table");
        add(animals);

        add(new H3("4. Adding a caption with <caption>, plus <thead>/<tbody>"));
        PlanetsTable planets = new PlanetsTable();
        planets.setId("planets-table");
        add(planets);

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
            row.addRowHeaderCell(header);
            row.addDataCells(data);
        }
    }

    /** Example 3: colspan / rowspan on both {@code <td>} and {@code <th>}. */
    static class AnimalsTable extends Table {
        {
            addRow().addHeaderCell("Animals").setColspan(2);
            addRow().addHeaderCell("Hippopotamus").setColspan(2);

            TableRow horseRow = addRow();
            TableHeaderCell horse = horseRow.addHeaderCell("Horse");
            horse.setScope(Scope.ROW);
            horse.setRowspan(2);
            horseRow.addDataCell("Mare");
            addRow("Stallion");

            addRow().addHeaderCell("Crocodile").setColspan(2);

            TableRow chickenRow = addRow();
            TableHeaderCell chicken = chickenRow.addHeaderCell("Chicken");
            chicken.setScope(Scope.ROW);
            chicken.setRowspan(2);
            chickenRow.addDataCell("Hen");
            addRow("Rooster");
        }
    }

    /** Example 4: planet data with caption, thead and rowgroup spans. */
    static class PlanetsTable extends Table {
        {
            addCaption(new Html("<span>Data about the planets of our solar"
                    + " system (Source: <a href=\"https://nssdc.gsfc.nasa.gov/"
                    + "planetary/factsheet/\">Nasa's Planetary Fact Sheet -"
                    + " Metric</a>).</span>"));

            TableHead head = getHead();
            TableRow headerRow = head.addRow();
            headerRow.addDataCell().setColspan(2);
            addColHeader(headerRow, new Html("<span>Name</span>"));
            addColHeader(headerRow,
                    new Html("<span>Mass (10<sup>24</sup>kg)</span>"));
            addColHeader(headerRow, new Html("<span>Diameter (km)</span>"));
            addColHeader(headerRow,
                    new Html("<span>Density (kg/m<sup>3</sup>)</span>"));
            addColHeader(headerRow,
                    new Html("<span>Gravity (m/s<sup>2</sup>)</span>"));
            addColHeader(headerRow,
                    new Html("<span>Length of day (hours)</span>"));
            addColHeader(headerRow, new Html(
                    "<span>Distance from Sun (10<sup>6</sup>km)</span>"));
            addColHeader(headerRow,
                    new Html("<span>Mean temperature (\u00b0C)</span>"));
            addColHeader(headerRow, new Html("<span>Number of moons</span>"));
            addColHeader(headerRow, new Html("<span>Notes</span>"));

            TableRow mercury = addRow();
            TableHeaderCell terrestrial = mercury
                    .addHeaderCell("Terrestrial planets");
            terrestrial.setScope(Scope.ROWGROUP);
            terrestrial.setColspan(2);
            terrestrial.setRowspan(4);
            addRowHeader(mercury, "Mercury");
            mercury.addDataCells("0.330", "4,879", "5427", "3.7", "4222.6",
                    "57.9", "167", "0", "Closest to the Sun");

            TableRow venus = addRow();
            addRowHeader(venus, "Venus");
            venus.addDataCells("4.87", "12,104", "5243", "8.9", "2802.0",
                    "108.2", "464", "0", "");

            TableRow earth = addRow();
            addRowHeader(earth, "Earth");
            earth.addDataCells("5.97", "12,756", "5514", "9.8", "24.0",
                    "149.6", "15", "1", "Our world");

            TableRow mars = addRow();
            addRowHeader(mars, "Mars");
            mars.addDataCells("0.642", "6,792", "3933", "3.7", "24.7", "227.9",
                    "-65", "2", "The red planet");

            TableRow jupiter = addRow();
            TableHeaderCell jovian = jupiter
                    .addHeaderCell("Jovian planets");
            jovian.setScope(Scope.ROWGROUP);
            jovian.setRowspan(4);
            TableHeaderCell gasGiants = jupiter
                    .addHeaderCell("Gas giants");
            gasGiants.setScope(Scope.ROWGROUP);
            gasGiants.setRowspan(2);
            addRowHeader(jupiter, "Jupiter");
            jupiter.addDataCells("1898", "142,984", "1326", "23.1", "9.9",
                    "778.6", "-110", "67", "The largest planet");

            TableRow saturn = addRow();
            addRowHeader(saturn, "Saturn");
            saturn.addDataCells("568", "120,536", "687", "9.0", "10.7",
                    "1433.5", "-140", "62", "");

            TableRow uranus = addRow();
            TableHeaderCell iceGiants = uranus
                    .addHeaderCell("Ice giants");
            iceGiants.setScope(Scope.ROWGROUP);
            iceGiants.setRowspan(2);
            addRowHeader(uranus, "Uranus");
            uranus.addDataCells("86.8", "51,118", "1271", "8.7", "17.2",
                    "2872.5", "-195", "27", "");

            TableRow neptune = addRow();
            addRowHeader(neptune, "Neptune");
            neptune.addDataCells("102", "49,528", "1638", "11.0", "16.1",
                    "4495.1", "-200", "14", "");

            TableRow pluto = addRow();
            TableHeaderCell dwarf = pluto
                    .addHeaderCell("Dwarf planets");
            dwarf.setScope(Scope.ROWGROUP);
            dwarf.setColspan(2);
            addRowHeader(pluto, "Pluto");
            pluto.addDataCells("0.0146", "2,370", "2095", "0.7", "153.3",
                    "5906.4", "-225", "5", "Declassified as a planet in 2006,"
                            + " but this remains controversial.");
        }

        private static void addColHeader(TableRow row, Html content) {
            TableHeaderCell th = row.addHeaderCell();
            th.setScope(Scope.COL);
            th.add(content);
        }

        private static void addRowHeader(TableRow row, String text) {
            row.addRowHeaderCell(text);
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
            row.addRowHeaderCell(period);
            row.addDataCells(days);
        }
    }
}
