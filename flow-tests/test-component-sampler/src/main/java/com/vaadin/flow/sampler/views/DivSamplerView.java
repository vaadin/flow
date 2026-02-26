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
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.sampler.SamplerMainLayout;

/**
 * Demonstrates the Div component and its features.
 */
@Route(value = "div", layout = SamplerMainLayout.class)
@PageTitle("Div Sampler")
public class DivSamplerView extends Div {

    public DivSamplerView() {
        setId("div-sampler");

        add(new H1("Div Component"));
        add(new Paragraph("The Div component is a generic block-level container for grouping content."));

        add(createSection("Basic Div",
            "Simple div containers with text and components.",
            createBasicDivDemo()));

        add(createSection("Flexbox Layout",
            "Div with CSS Flexbox for layout.",
            createFlexboxDemo()));

        add(createSection("Grid Layout",
            "Div with CSS Grid for layout.",
            createGridDemo()));

        add(createSection("Nested Divs",
            "Divs can be nested to create complex layouts.",
            createNestedDivsDemo()));

        add(createSection("Click Events",
            "Div supports click events.",
            createClickEventsDemo()));

        add(createSection("Dynamic Children",
            "Add and remove children dynamically.",
            createDynamicChildrenDemo()));
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

    private Div createBasicDivDemo() {
        Div demo = new Div();
        demo.setId("basic-div");

        Div simpleDiv = new Div("Simple div with text");
        simpleDiv.setId("simple-div");
        simpleDiv.getStyle()
            .set("padding", "15px")
            .set("background-color", "#f5f5f5")
            .set("margin-bottom", "10px");

        Div styledDiv = new Div();
        styledDiv.setId("styled-div");
        styledDiv.setText("Styled div with border and shadow");
        styledDiv.getStyle()
            .set("padding", "20px")
            .set("border", "2px solid #1976d2")
            .set("border-radius", "8px")
            .set("box-shadow", "0 4px 6px rgba(0,0,0,0.1)")
            .set("margin-bottom", "10px");

        Div divWithComponents = new Div();
        divWithComponents.setId("div-with-components");
        divWithComponents.getStyle()
            .set("padding", "15px")
            .set("background-color", "#e8f5e9")
            .set("border-radius", "4px");
        divWithComponents.add(
            new Span("This div contains "),
            new Span("multiple"),
            new Span(" span elements.")
        );

        demo.add(simpleDiv, styledDiv, divWithComponents);
        return demo;
    }

    private Div createFlexboxDemo() {
        Div demo = new Div();
        demo.setId("flexbox-demo");

        // Horizontal flex
        Div flexRow = new Div();
        flexRow.setId("flex-row");
        flexRow.getStyle()
            .set("display", "flex")
            .set("gap", "10px")
            .set("margin-bottom", "20px");

        for (int i = 1; i <= 4; i++) {
            Div item = new Div("Item " + i);
            item.setId("flex-item-" + i);
            item.getStyle()
                .set("flex", "1")
                .set("padding", "20px")
                .set("text-align", "center")
                .set("background-color", "#bbdefb")
                .set("border-radius", "4px");
            flexRow.add(item);
        }

        // Flex with justify-content
        Div flexJustify = new Div();
        flexJustify.setId("flex-justify");
        flexJustify.getStyle()
            .set("display", "flex")
            .set("justify-content", "space-between")
            .set("background-color", "#f5f5f5")
            .set("padding", "10px")
            .set("margin-bottom", "20px")
            .set("border-radius", "4px");

        for (String align : new String[]{"Start", "Center", "End"}) {
            Div item = new Div(align);
            item.getStyle()
                .set("padding", "15px 25px")
                .set("background-color", "#90caf9")
                .set("border-radius", "4px");
            flexJustify.add(item);
        }

        // Flex wrap
        Div flexWrap = new Div();
        flexWrap.setId("flex-wrap");
        flexWrap.getStyle()
            .set("display", "flex")
            .set("flex-wrap", "wrap")
            .set("gap", "10px");

        for (int i = 1; i <= 8; i++) {
            Div item = new Div("Wrap " + i);
            item.getStyle()
                .set("width", "calc(25% - 8px)")
                .set("padding", "15px")
                .set("text-align", "center")
                .set("background-color", "#c5e1a5")
                .set("border-radius", "4px")
                .set("box-sizing", "border-box");
            flexWrap.add(item);
        }

        demo.add(flexRow, flexJustify, flexWrap);
        return demo;
    }

    private Div createGridDemo() {
        Div demo = new Div();
        demo.setId("grid-demo");

        Div gridContainer = new Div();
        gridContainer.setId("grid-container");
        gridContainer.getStyle()
            .set("display", "grid")
            .set("grid-template-columns", "repeat(3, 1fr)")
            .set("gap", "15px");

        String[] colors = {"#ffcdd2", "#f8bbd0", "#e1bee7", "#d1c4e9",
                          "#c5cae9", "#bbdefb", "#b3e5fc", "#b2ebf2", "#b2dfdb"};

        for (int i = 1; i <= 9; i++) {
            Div cell = new Div("Cell " + i);
            cell.setId("grid-cell-" + i);
            cell.getStyle()
                .set("padding", "30px")
                .set("text-align", "center")
                .set("background-color", colors[i-1])
                .set("border-radius", "8px")
                .set("font-weight", "500");
            gridContainer.add(cell);
        }

        demo.add(gridContainer);
        return demo;
    }

    private Div createNestedDivsDemo() {
        Div demo = new Div();
        demo.setId("nested-divs");

        Div outer = new Div();
        outer.setId("outer-div");
        outer.getStyle()
            .set("padding", "20px")
            .set("background-color", "#e3f2fd")
            .set("border-radius", "8px");
        outer.add(new Span("Outer Div"));

        Div middle = new Div();
        middle.setId("middle-div");
        middle.getStyle()
            .set("padding", "20px")
            .set("margin-top", "10px")
            .set("background-color", "#90caf9")
            .set("border-radius", "8px");
        middle.add(new Span("Middle Div"));

        Div inner = new Div();
        inner.setId("inner-div");
        inner.getStyle()
            .set("padding", "20px")
            .set("margin-top", "10px")
            .set("background-color", "#42a5f5")
            .set("color", "white")
            .set("border-radius", "8px");
        inner.add(new Span("Inner Div"));

        middle.add(inner);
        outer.add(middle);

        demo.add(outer);
        return demo;
    }

    private Div createClickEventsDemo() {
        Div demo = new Div();
        demo.setId("click-events");

        Div clickableDiv = new Div("Click me!");
        clickableDiv.setId("clickable-div");
        clickableDiv.getStyle()
            .set("padding", "30px")
            .set("text-align", "center")
            .set("background-color", "#4caf50")
            .set("color", "white")
            .set("border-radius", "8px")
            .set("cursor", "pointer")
            .set("user-select", "none")
            .set("transition", "background-color 0.3s");

        Div clickCount = new Div("Click count: 0");
        clickCount.setId("click-count");
        clickCount.getStyle().set("margin-top", "15px").set("font-weight", "500");

        int[] count = {0};
        clickableDiv.addClickListener(e -> {
            count[0]++;
            clickCount.setText("Click count: " + count[0]);
            clickableDiv.getStyle().set("background-color",
                count[0] % 2 == 0 ? "#4caf50" : "#388e3c");
        });

        demo.add(clickableDiv, clickCount);
        return demo;
    }

    private Div createDynamicChildrenDemo() {
        Div demo = new Div();
        demo.setId("dynamic-children");

        Div container = new Div();
        container.setId("dynamic-container");
        container.getStyle()
            .set("min-height", "100px")
            .set("padding", "15px")
            .set("background-color", "#f5f5f5")
            .set("border-radius", "8px")
            .set("margin-bottom", "15px");

        int[] counter = {0};

        NativeButton addButton = new NativeButton("Add Child", e -> {
            counter[0]++;
            Div child = new Div("Child " + counter[0]);
            child.setId("child-" + counter[0]);
            child.getStyle()
                .set("display", "inline-block")
                .set("padding", "10px 15px")
                .set("margin", "5px")
                .set("background-color", "#81c784")
                .set("border-radius", "4px");
            container.add(child);
        });
        addButton.setId("add-child-btn");

        NativeButton removeLastButton = new NativeButton("Remove Last", e -> {
            if (container.getComponentCount() > 0) {
                container.remove(container.getComponentAt(container.getComponentCount() - 1));
            }
        });
        removeLastButton.setId("remove-last-btn");

        NativeButton clearButton = new NativeButton("Clear All", e -> {
            container.removeAll();
            counter[0] = 0;
        });
        clearButton.setId("clear-all-btn");

        Div buttons = new Div(addButton, removeLastButton, clearButton);
        buttons.getStyle().set("display", "flex").set("gap", "10px");

        demo.add(container, buttons);
        return demo;
    }
}
