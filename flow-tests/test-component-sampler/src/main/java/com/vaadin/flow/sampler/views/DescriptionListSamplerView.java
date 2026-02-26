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

import com.vaadin.flow.component.html.DescriptionList;
import com.vaadin.flow.component.html.DescriptionList.Description;
import com.vaadin.flow.component.html.DescriptionList.Term;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.sampler.SamplerMainLayout;

/**
 * Demonstrates the DescriptionList component and its features.
 */
@Route(value = "description-list", layout = SamplerMainLayout.class)
@PageTitle("Description List Sampler")
public class DescriptionListSamplerView extends Div {

    public DescriptionListSamplerView() {
        setId("description-list-sampler");

        add(new H1("Description List Component"));
        add(new Paragraph("The DescriptionList component displays term-description pairs."));

        add(createSection("Basic Description List",
            "Simple term and description pairs.",
            createBasicDemo()));

        add(createSection("Glossary Style",
            "Description list styled as a glossary.",
            createGlossaryDemo()));

        add(createSection("Key-Value Pairs",
            "Description list for displaying data.",
            createKeyValueDemo()));

        add(createSection("Multiple Descriptions",
            "Terms with multiple descriptions.",
            createMultipleDescriptionsDemo()));

        add(createSection("Styled Description List",
            "Custom styling for description lists.",
            createStyledDemo()));
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

    private Div createBasicDemo() {
        Div demo = new Div();
        demo.setId("basic-dl");

        DescriptionList dl = new DescriptionList();
        dl.setId("basic-description-list");

        dl.add(new Term("HTML"));
        dl.add(new Description("HyperText Markup Language"));

        dl.add(new Term("CSS"));
        dl.add(new Description("Cascading Style Sheets"));

        dl.add(new Term("JavaScript"));
        dl.add(new Description("A programming language for the web"));

        demo.add(dl);
        return demo;
    }

    private Div createGlossaryDemo() {
        Div demo = new Div();
        demo.setId("glossary-dl");

        DescriptionList dl = new DescriptionList();
        dl.setId("glossary-description-list");
        dl.getStyle().set("max-width", "600px");

        String[][] glossaryItems = {
            {"Component", "A reusable piece of UI that encapsulates both appearance and behavior."},
            {"State", "Data that can change over time and affects how a component renders."},
            {"Props", "Input values passed from a parent component to a child component."},
            {"Event", "An action or occurrence that can be detected and handled by the application."},
            {"Lifecycle", "The series of stages a component goes through from creation to destruction."}
        };

        for (String[] item : glossaryItems) {
            Term term = new Term(item[0]);
            term.getStyle()
                .set("font-weight", "bold")
                .set("color", "#1976d2")
                .set("margin-top", "15px");

            Description desc = new Description(item[1]);
            desc.getStyle()
                .set("margin-left", "20px")
                .set("padding-left", "15px")
                .set("border-left", "3px solid #e0e0e0");

            dl.add(term, desc);
        }

        demo.add(dl);
        return demo;
    }

    private Div createKeyValueDemo() {
        Div demo = new Div();
        demo.setId("key-value-dl");

        Div card = new Div();
        card.getStyle()
            .set("padding", "20px")
            .set("background-color", "#f5f5f5")
            .set("border-radius", "8px")
            .set("max-width", "400px");

        DescriptionList dl = new DescriptionList();
        dl.setId("key-value-description-list");
        dl.getStyle()
            .set("display", "grid")
            .set("grid-template-columns", "auto 1fr")
            .set("gap", "10px 20px")
            .set("margin", "0");

        String[][] userData = {
            {"Name", "John Doe"},
            {"Email", "john.doe@example.com"},
            {"Phone", "+1 234 567 8900"},
            {"Location", "New York, USA"},
            {"Role", "Senior Developer"},
            {"Status", "Active"}
        };

        for (String[] pair : userData) {
            Term term = new Term(pair[0] + ":");
            term.getStyle()
                .set("font-weight", "500")
                .set("color", "#666");

            Description desc = new Description(pair[1]);

            dl.add(term, desc);
        }

        card.add(dl);
        demo.add(card);
        return demo;
    }

    private Div createMultipleDescriptionsDemo() {
        Div demo = new Div();
        demo.setId("multiple-desc-dl");

        DescriptionList dl = new DescriptionList();
        dl.setId("multiple-description-list");

        // Term with multiple descriptions
        Term term1 = new Term("Firefox");
        term1.getStyle()
            .set("font-weight", "bold")
            .set("font-size", "1.1em")
            .set("margin-top", "15px");

        Description desc1a = new Description("A free, open source, cross-platform, graphical web browser developed by Mozilla.");
        Description desc1b = new Description("Originally named Phoenix, later renamed to Firebird, then finally Firefox.");
        Description desc1c = new Description("First released in 2004, it was the second most popular web browser for many years.");

        desc1a.getStyle().set("margin-left", "20px");
        desc1b.getStyle().set("margin-left", "20px").set("font-style", "italic");
        desc1c.getStyle().set("margin-left", "20px");

        // Another term with multiple descriptions
        Term term2 = new Term("Chrome");
        term2.getStyle()
            .set("font-weight", "bold")
            .set("font-size", "1.1em")
            .set("margin-top", "15px");

        Description desc2a = new Description("A cross-platform web browser developed by Google.");
        Description desc2b = new Description("First released in 2008 for Microsoft Windows.");
        Description desc2c = new Description("Uses the Blink rendering engine and V8 JavaScript engine.");

        desc2a.getStyle().set("margin-left", "20px");
        desc2b.getStyle().set("margin-left", "20px").set("font-style", "italic");
        desc2c.getStyle().set("margin-left", "20px");

        dl.add(term1, desc1a, desc1b, desc1c, term2, desc2a, desc2b, desc2c);

        demo.add(dl);
        return demo;
    }

    private Div createStyledDemo() {
        Div demo = new Div();
        demo.setId("styled-dl");
        demo.getStyle()
            .set("display", "flex")
            .set("gap", "30px")
            .set("flex-wrap", "wrap");

        // Card style
        Div cardStyle = new Div();
        cardStyle.getStyle().set("flex", "1").set("min-width", "250px");

        DescriptionList cardDl = new DescriptionList();
        cardDl.setId("card-style-dl");
        cardDl.getStyle()
            .set("padding", "20px")
            .set("background-color", "white")
            .set("border-radius", "12px")
            .set("box-shadow", "0 4px 6px rgba(0,0,0,0.1)");

        String[][] cardData = {
            {"Plan", "Premium"},
            {"Price", "$29/month"},
            {"Users", "Unlimited"}
        };

        for (String[] pair : cardData) {
            Term t = new Term(pair[0]);
            t.getStyle()
                .set("font-size", "0.85em")
                .set("color", "#666")
                .set("text-transform", "uppercase")
                .set("margin-top", "10px");

            Description d = new Description(pair[1]);
            d.getStyle()
                .set("font-size", "1.2em")
                .set("font-weight", "bold")
                .set("color", "#1976d2")
                .set("margin", "0");

            cardDl.add(t, d);
        }

        cardStyle.add(cardDl);

        // Horizontal style
        Div horizontalStyle = new Div();
        horizontalStyle.getStyle().set("flex", "1").set("min-width", "300px");

        DescriptionList horizontalDl = new DescriptionList();
        horizontalDl.setId("horizontal-style-dl");
        horizontalDl.getStyle()
            .set("display", "flex")
            .set("flex-wrap", "wrap")
            .set("gap", "20px")
            .set("padding", "15px")
            .set("background-color", "#e8f5e9")
            .set("border-radius", "8px");

        String[][] horizontalData = {
            {"Commits", "1,234"},
            {"Branches", "45"},
            {"Tags", "89"}
        };

        for (String[] pair : horizontalData) {
            Div wrapper = new Div();
            wrapper.getStyle().set("text-align", "center");

            Term t = new Term(pair[1]);
            t.getStyle()
                .set("display", "block")
                .set("font-size", "1.5em")
                .set("font-weight", "bold")
                .set("color", "#388e3c");

            Description d = new Description(pair[0]);
            d.getStyle()
                .set("display", "block")
                .set("font-size", "0.9em")
                .set("color", "#666")
                .set("margin", "0");

            horizontalDl.add(t, d);
        }

        horizontalStyle.add(horizontalDl);

        demo.add(cardStyle, horizontalStyle);
        return demo;
    }
}
