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
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.OrderedList;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.sampler.SamplerMainLayout;

/**
 * Demonstrates list components (OrderedList, UnorderedList, ListItem).
 */
@Route(value = "lists", layout = SamplerMainLayout.class)
@PageTitle("Lists Sampler")
public class ListSamplerView extends Div {

    public ListSamplerView() {
        setId("list-sampler");

        add(new H1("List Components"));
        add(new Paragraph("Ordered (numbered) and unordered (bulleted) list components."));

        add(createSection("Unordered List",
            "Bulleted list for items without specific order.",
            createUnorderedListDemo()));

        add(createSection("Ordered List",
            "Numbered list for sequential items.",
            createOrderedListDemo()));

        add(createSection("Nested Lists",
            "Lists can be nested within other lists.",
            createNestedListsDemo()));

        add(createSection("Styled Lists",
            "Lists with custom styling.",
            createStyledListsDemo()));

        add(createSection("Dynamic Lists",
            "Add and remove items dynamically.",
            createDynamicListDemo()));
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

    private Div createUnorderedListDemo() {
        Div demo = new Div();
        demo.setId("unordered-list-demo");

        UnorderedList ul = new UnorderedList();
        ul.setId("basic-ul");

        ul.add(new ListItem("First item"));
        ul.add(new ListItem("Second item"));
        ul.add(new ListItem("Third item"));
        ul.add(new ListItem("Fourth item"));
        ul.add(new ListItem("Fifth item"));

        demo.add(ul);
        return demo;
    }

    private Div createOrderedListDemo() {
        Div demo = new Div();
        demo.setId("ordered-list-demo");
        demo.getStyle()
            .set("display", "flex")
            .set("gap", "40px")
            .set("flex-wrap", "wrap");

        // Default numbered
        Div defaultList = new Div();
        defaultList.add(new Paragraph("Default (decimal):"));
        OrderedList ol1 = new OrderedList();
        ol1.setId("decimal-ol");
        ol1.add(new ListItem("First step"));
        ol1.add(new ListItem("Second step"));
        ol1.add(new ListItem("Third step"));
        defaultList.add(ol1);

        // Roman numerals
        Div romanList = new Div();
        romanList.add(new Paragraph("Roman numerals:"));
        OrderedList ol2 = new OrderedList();
        ol2.setId("roman-ol");
        ol2.getStyle().set("list-style-type", "upper-roman");
        ol2.add(new ListItem("Chapter one"));
        ol2.add(new ListItem("Chapter two"));
        ol2.add(new ListItem("Chapter three"));
        romanList.add(ol2);

        // Alphabetical
        Div alphaList = new Div();
        alphaList.add(new Paragraph("Alphabetical:"));
        OrderedList ol3 = new OrderedList();
        ol3.setId("alpha-ol");
        ol3.getStyle().set("list-style-type", "lower-alpha");
        ol3.add(new ListItem("Option a"));
        ol3.add(new ListItem("Option b"));
        ol3.add(new ListItem("Option c"));
        alphaList.add(ol3);

        demo.add(defaultList, romanList, alphaList);
        return demo;
    }

    private Div createNestedListsDemo() {
        Div demo = new Div();
        demo.setId("nested-lists-demo");

        UnorderedList mainList = new UnorderedList();
        mainList.setId("nested-ul");

        // Item 1 with nested list
        ListItem item1 = new ListItem("Fruits");
        UnorderedList subList1 = new UnorderedList();
        subList1.add(new ListItem("Apples"));
        subList1.add(new ListItem("Oranges"));
        subList1.add(new ListItem("Bananas"));
        item1.add(subList1);

        // Item 2 with nested list
        ListItem item2 = new ListItem("Vegetables");
        UnorderedList subList2 = new UnorderedList();
        subList2.add(new ListItem("Carrots"));
        subList2.add(new ListItem("Broccoli"));

        // Deeply nested
        ListItem leafyItem = new ListItem("Leafy greens");
        UnorderedList deepList = new UnorderedList();
        deepList.add(new ListItem("Spinach"));
        deepList.add(new ListItem("Kale"));
        deepList.add(new ListItem("Lettuce"));
        leafyItem.add(deepList);

        subList2.add(leafyItem);
        item2.add(subList2);

        // Item 3 with numbered nested list
        ListItem item3 = new ListItem("Recipe steps");
        OrderedList subList3 = new OrderedList();
        subList3.add(new ListItem("Preheat oven to 350°F"));
        subList3.add(new ListItem("Mix ingredients"));
        subList3.add(new ListItem("Bake for 30 minutes"));
        subList3.add(new ListItem("Let cool"));
        item3.add(subList3);

        mainList.add(item1, item2, item3);
        demo.add(mainList);
        return demo;
    }

    private Div createStyledListsDemo() {
        Div demo = new Div();
        demo.setId("styled-lists-demo");
        demo.getStyle()
            .set("display", "flex")
            .set("gap", "40px")
            .set("flex-wrap", "wrap");

        // Custom markers
        Div customMarkers = new Div();
        customMarkers.add(new Paragraph("Custom markers:"));
        UnorderedList customUl = new UnorderedList();
        customUl.setId("custom-markers-ul");
        customUl.getStyle().set("list-style-type", "none").set("padding-left", "0");

        String[] items = {"Completed task", "Pending task", "In progress"};
        String[] markers = {"<span style='color: #4caf50;'>&#10004;</span>",
                           "<span style='color: #f44336;'>&#10008;</span>",
                           "<span style='color: #ff9800;'>&#9679;</span>"};

        for (int i = 0; i < items.length; i++) {
            ListItem li = new ListItem();
            li.getElement().setProperty("innerHTML", markers[i] + " " + items[i]);
            li.getStyle().set("padding", "5px 0");
            customUl.add(li);
        }
        customMarkers.add(customUl);

        // Bordered list
        Div borderedList = new Div();
        borderedList.add(new Paragraph("Bordered list:"));
        UnorderedList borderedUl = new UnorderedList();
        borderedUl.setId("bordered-ul");
        borderedUl.getStyle()
            .set("list-style", "none")
            .set("padding", "0")
            .set("border", "1px solid #e0e0e0")
            .set("border-radius", "8px")
            .set("overflow", "hidden");

        String[] menuItems = {"Dashboard", "Settings", "Profile", "Logout"};
        for (String item : menuItems) {
            ListItem li = new ListItem(item);
            li.getStyle()
                .set("padding", "12px 16px")
                .set("border-bottom", "1px solid #e0e0e0")
                .set("cursor", "pointer");
            borderedUl.add(li);
        }
        borderedUl.getChildren().reduce((first, second) -> second)
            .ifPresent(last -> ((ListItem) last).getStyle().remove("border-bottom"));

        borderedList.add(borderedUl);

        // Horizontal list
        Div horizontalList = new Div();
        horizontalList.add(new Paragraph("Horizontal list:"));
        UnorderedList horizontalUl = new UnorderedList();
        horizontalUl.setId("horizontal-ul");
        horizontalUl.getStyle()
            .set("list-style", "none")
            .set("padding", "0")
            .set("display", "flex")
            .set("gap", "15px");

        String[] navItems = {"Home", "About", "Services", "Contact"};
        for (String item : navItems) {
            ListItem li = new ListItem(item);
            li.getStyle()
                .set("padding", "8px 16px")
                .set("background-color", "#1976d2")
                .set("color", "white")
                .set("border-radius", "4px")
                .set("cursor", "pointer");
            horizontalUl.add(li);
        }
        horizontalList.add(horizontalUl);

        demo.add(customMarkers, borderedList, horizontalList);
        return demo;
    }

    private Div createDynamicListDemo() {
        Div demo = new Div();
        demo.setId("dynamic-list-demo");

        UnorderedList dynamicList = new UnorderedList();
        dynamicList.setId("dynamic-ul");
        dynamicList.getStyle()
            .set("min-height", "100px")
            .set("padding", "15px 30px")
            .set("background-color", "#f5f5f5")
            .set("border-radius", "8px")
            .set("margin-bottom", "15px");

        // Add some initial items
        for (int i = 1; i <= 3; i++) {
            ListItem item = new ListItem("Item " + i);
            item.setId("dynamic-item-" + i);
            dynamicList.add(item);
        }

        int[] counter = {3};

        Div controls = new Div();
        controls.getStyle().set("display", "flex").set("gap", "10px");

        NativeButton addButton = new NativeButton("Add Item", e -> {
            counter[0]++;
            ListItem newItem = new ListItem("Item " + counter[0]);
            newItem.setId("dynamic-item-" + counter[0]);
            dynamicList.add(newItem);
        });
        addButton.setId("add-list-item");

        NativeButton removeLastButton = new NativeButton("Remove Last", e -> {
            if (dynamicList.getComponentCount() > 0) {
                dynamicList.remove(dynamicList.getComponentAt(dynamicList.getComponentCount() - 1));
            }
        });
        removeLastButton.setId("remove-list-item");

        NativeButton clearButton = new NativeButton("Clear All", e -> {
            dynamicList.removeAll();
            counter[0] = 0;
        });
        clearButton.setId("clear-list");

        controls.add(addButton, removeLastButton, clearButton);

        demo.add(dynamicList, controls);
        return demo;
    }
}
