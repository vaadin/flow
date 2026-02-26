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
package com.vaadin.flow.sampler.views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.NativeTable;
import com.vaadin.flow.component.html.NativeTableBody;
import com.vaadin.flow.component.html.NativeTableCaption;
import com.vaadin.flow.component.html.NativeTableCell;
import com.vaadin.flow.component.html.NativeTableFooter;
import com.vaadin.flow.component.html.NativeTableHeader;
import com.vaadin.flow.component.html.NativeTableHeaderCell;
import com.vaadin.flow.component.html.NativeTableRow;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.sampler.SamplerMainLayout;

/**
 * Demonstrates the NativeTable component and related elements.
 */
@Route(value = "table", layout = SamplerMainLayout.class)
@PageTitle("Table Sampler")
public class TableSamplerView extends Div {

    public TableSamplerView() {
        setId("table-sampler");

        add(new H1("NativeTable Component"));
        add(new Paragraph("Native HTML table component with header, body, and footer sections."));

        add(createSection("Basic Table",
            "Simple table with headers and data rows.",
            createBasicTableDemo()));

        add(createSection("Complete Table",
            "Table with caption, header, body, and footer.",
            createCompleteTableDemo()));

        add(createSection("Styled Table",
            "Table with custom styling.",
            createStyledTableDemo()));

        add(createSection("Dynamic Table",
            "Add and remove rows dynamically.",
            createDynamicTableDemo()));
    }

    private Div createSection(String title, String description, Div content) {
        Div section = new Div();
        section.getStyle()
            .set("margin-bottom", "40px")
            .set("padding", "20px")
            .set("border", "1px solid #e0e0e0")
            .set("border-radius", "8px");

        H2 sectionTitle = new H2(title);
        sectionTitle.getStyle().set("margin-top", "0");

        Paragraph desc = new Paragraph(description);
        desc.getStyle().set("color", "#666");

        section.add(sectionTitle, desc, new Hr(), content);
        return section;
    }

    private Div createBasicTableDemo() {
        Div demo = new Div();
        demo.setId("basic-table-demo");

        NativeTable table = new NativeTable();
        table.setId("basic-table");
        table.getStyle()
            .set("width", "100%")
            .set("border-collapse", "collapse");

        // Header
        NativeTableHeader thead = new NativeTableHeader();
        NativeTableRow headerRow = new NativeTableRow();

        for (String col : new String[]{"Name", "Email", "Role"}) {
            NativeTableHeaderCell th = new NativeTableHeaderCell(col);
            th.getStyle()
                .set("border", "1px solid #ddd")
                .set("padding", "12px")
                .set("text-align", "left")
                .set("background-color", "#f5f5f5");
            headerRow.add(th);
        }
        thead.add(headerRow);

        // Body
        NativeTableBody tbody = new NativeTableBody();

        String[][] data = {
            {"John Doe", "john@example.com", "Developer"},
            {"Jane Smith", "jane@example.com", "Designer"},
            {"Bob Johnson", "bob@example.com", "Manager"}
        };

        for (String[] row : data) {
            NativeTableRow tr = new NativeTableRow();
            for (String cell : row) {
                NativeTableCell td = new NativeTableCell(cell);
                td.getStyle()
                    .set("border", "1px solid #ddd")
                    .set("padding", "12px");
                tr.add(td);
            }
            tbody.add(tr);
        }

        table.add(thead, tbody);
        demo.add(table);
        return demo;
    }

    private Div createCompleteTableDemo() {
        Div demo = new Div();
        demo.setId("complete-table-demo");

        NativeTable table = new NativeTable();
        table.setId("complete-table");
        table.getStyle()
            .set("width", "100%")
            .set("border-collapse", "collapse");

        // Caption
        NativeTableCaption caption = new NativeTableCaption("Quarterly Sales Report");
        caption.getStyle()
            .set("font-weight", "bold")
            .set("font-size", "1.2em")
            .set("padding", "10px")
            .set("caption-side", "top");

        // Header
        NativeTableHeader thead = new NativeTableHeader();
        NativeTableRow headerRow = new NativeTableRow();

        for (String col : new String[]{"Product", "Q1", "Q2", "Q3", "Q4", "Total"}) {
            NativeTableHeaderCell th = new NativeTableHeaderCell(col);
            th.getStyle()
                .set("border", "1px solid #1976d2")
                .set("padding", "12px")
                .set("background-color", "#1976d2")
                .set("color", "white")
                .set("text-align", col.equals("Product") ? "left" : "right");
            headerRow.add(th);
        }
        thead.add(headerRow);

        // Body
        NativeTableBody tbody = new NativeTableBody();

        String[][] salesData = {
            {"Widget A", "1,200", "1,500", "1,800", "2,100", "6,600"},
            {"Widget B", "800", "950", "1,100", "1,300", "4,150"},
            {"Widget C", "2,500", "2,800", "3,200", "3,500", "12,000"},
            {"Widget D", "450", "520", "610", "700", "2,280"}
        };

        for (int i = 0; i < salesData.length; i++) {
            NativeTableRow tr = new NativeTableRow();
            tr.getStyle().set("background-color", i % 2 == 0 ? "white" : "#f5f5f5");

            for (int j = 0; j < salesData[i].length; j++) {
                NativeTableCell td = new NativeTableCell(salesData[i][j]);
                td.getStyle()
                    .set("border", "1px solid #ddd")
                    .set("padding", "10px")
                    .set("text-align", j == 0 ? "left" : "right");
                tr.add(td);
            }
            tbody.add(tr);
        }

        // Footer
        NativeTableFooter tfoot = new NativeTableFooter();
        NativeTableRow footerRow = new NativeTableRow();

        String[] totals = {"Total", "4,950", "5,770", "6,710", "7,600", "25,030"};
        for (int i = 0; i < totals.length; i++) {
            NativeTableCell td = new NativeTableCell(totals[i]);
            td.getStyle()
                .set("border", "1px solid #1976d2")
                .set("padding", "12px")
                .set("font-weight", "bold")
                .set("background-color", "#e3f2fd")
                .set("text-align", i == 0 ? "left" : "right");
            footerRow.add(td);
        }
        tfoot.add(footerRow);

        table.add(caption, thead, tbody, tfoot);
        demo.add(table);
        return demo;
    }

    private Div createStyledTableDemo() {
        Div demo = new Div();
        demo.setId("styled-table-demo");

        NativeTable table = new NativeTable();
        table.setId("styled-table");
        table.getStyle()
            .set("width", "100%")
            .set("border-collapse", "separate")
            .set("border-spacing", "0")
            .set("border-radius", "8px")
            .set("overflow", "hidden")
            .set("box-shadow", "0 4px 6px rgba(0,0,0,0.1)");

        // Header
        NativeTableHeader thead = new NativeTableHeader();
        NativeTableRow headerRow = new NativeTableRow();

        for (String col : new String[]{"Status", "Task", "Priority", "Due Date"}) {
            NativeTableHeaderCell th = new NativeTableHeaderCell(col);
            th.getStyle()
                .set("padding", "15px")
                .set("background", "linear-gradient(135deg, #667eea 0%, #764ba2 100%)")
                .set("color", "white")
                .set("font-weight", "500")
                .set("text-align", "left");
            headerRow.add(th);
        }
        thead.add(headerRow);

        // Body
        NativeTableBody tbody = new NativeTableBody();

        Object[][] tasks = {
            {"done", "Complete project setup", "High", "2026-02-20"},
            {"in-progress", "Implement user authentication", "High", "2026-02-25"},
            {"pending", "Write documentation", "Medium", "2026-03-01"},
            {"pending", "Add unit tests", "Medium", "2026-03-05"},
            {"blocked", "Deploy to production", "High", "2026-03-10"}
        };

        String[] statusColors = {"#4caf50", "#ff9800", "#9e9e9e", "#9e9e9e", "#f44336"};
        String[] statusLabels = {"Done", "In Progress", "Pending", "Pending", "Blocked"};
        String[] priorityColors = {"#f44336", "#f44336", "#ff9800", "#ff9800", "#f44336"};

        for (int i = 0; i < tasks.length; i++) {
            NativeTableRow tr = new NativeTableRow();
            tr.getStyle()
                .set("background-color", i % 2 == 0 ? "white" : "#fafafa")
                .set("transition", "background-color 0.2s");

            // Status cell with badge
            NativeTableCell statusCell = new NativeTableCell();
            statusCell.getStyle().set("padding", "12px");
            statusCell.getElement().setProperty("innerHTML",
                "<span style='display:inline-block;padding:4px 12px;border-radius:20px;font-size:0.85em;background-color:" +
                statusColors[i] + ";color:white;'>" + statusLabels[i] + "</span>");
            tr.add(statusCell);

            // Task cell
            NativeTableCell taskCell = new NativeTableCell((String) tasks[i][1]);
            taskCell.getStyle().set("padding", "12px");
            tr.add(taskCell);

            // Priority cell with color
            NativeTableCell priorityCell = new NativeTableCell((String) tasks[i][2]);
            priorityCell.getStyle()
                .set("padding", "12px")
                .set("color", priorityColors[i])
                .set("font-weight", "500");
            tr.add(priorityCell);

            // Due date cell
            NativeTableCell dateCell = new NativeTableCell((String) tasks[i][3]);
            dateCell.getStyle()
                .set("padding", "12px")
                .set("color", "#666");
            tr.add(dateCell);

            tbody.add(tr);
        }

        table.add(thead, tbody);
        demo.add(table);
        return demo;
    }

    private Div createDynamicTableDemo() {
        Div demo = new Div();
        demo.setId("dynamic-table-demo");

        NativeTable table = new NativeTable();
        table.setId("dynamic-table");
        table.getStyle()
            .set("width", "100%")
            .set("border-collapse", "collapse")
            .set("margin-bottom", "15px");

        // Header
        NativeTableHeader thead = new NativeTableHeader();
        NativeTableRow headerRow = new NativeTableRow();

        for (String col : new String[]{"ID", "Item", "Quantity", "Actions"}) {
            NativeTableHeaderCell th = new NativeTableHeaderCell(col);
            th.getStyle()
                .set("border", "1px solid #ddd")
                .set("padding", "10px")
                .set("background-color", "#f5f5f5")
                .set("text-align", "left");
            headerRow.add(th);
        }
        thead.add(headerRow);

        // Body
        NativeTableBody tbody = new NativeTableBody();
        tbody.setId("dynamic-tbody");

        table.add(thead, tbody);

        // Initial rows
        int[] rowCounter = {0};
        for (int i = 0; i < 3; i++) {
            rowCounter[0]++;
            addTableRow(tbody, rowCounter[0]);
        }

        // Controls
        Div controls = new Div();
        controls.getStyle().set("display", "flex").set("gap", "10px");

        NativeButton addButton = new NativeButton("Add Row", e -> {
            rowCounter[0]++;
            addTableRow(tbody, rowCounter[0]);
        });
        addButton.setId("add-row-btn");

        NativeButton clearButton = new NativeButton("Clear All", e -> {
            tbody.removeAll();
            rowCounter[0] = 0;
        });
        clearButton.setId("clear-rows-btn");

        controls.add(addButton, clearButton);

        demo.add(table, controls);
        return demo;
    }

    private void addTableRow(NativeTableBody tbody, int id) {
        NativeTableRow tr = new NativeTableRow();
        tr.setId("row-" + id);

        NativeTableCell idCell = new NativeTableCell(String.valueOf(id));
        idCell.getStyle().set("border", "1px solid #ddd").set("padding", "10px");

        NativeTableCell itemCell = new NativeTableCell("Item " + id);
        itemCell.getStyle().set("border", "1px solid #ddd").set("padding", "10px");

        NativeTableCell qtyCell = new NativeTableCell(String.valueOf((int) (Math.random() * 100) + 1));
        qtyCell.getStyle().set("border", "1px solid #ddd").set("padding", "10px");

        NativeTableCell actionsCell = new NativeTableCell();
        actionsCell.getStyle().set("border", "1px solid #ddd").set("padding", "10px");

        NativeButton deleteBtn = new NativeButton("Delete", e -> tbody.remove(tr));
        deleteBtn.getStyle()
            .set("padding", "5px 10px")
            .set("background-color", "#f44336")
            .set("color", "white")
            .set("border", "none")
            .set("border-radius", "4px")
            .set("cursor", "pointer");

        actionsCell.add(deleteBtn);
        tr.add(idCell, itemCell, qtyCell, actionsCell);
        tbody.add(tr);
    }
}
