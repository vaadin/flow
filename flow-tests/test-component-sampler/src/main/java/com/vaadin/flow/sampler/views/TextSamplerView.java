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

import com.vaadin.flow.component.html.Abbr;
import com.vaadin.flow.component.html.Code;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Emphasis;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.sampler.SamplerMainLayout;

/**
 * Demonstrates Paragraph and other text-related components.
 */
@Route(value = "text", layout = SamplerMainLayout.class)
@PageTitle("Text Components Sampler")
public class TextSamplerView extends Div {

    public TextSamplerView() {
        setId("text-sampler");

        add(new H1("Text Components"));
        add(new Paragraph("Components for displaying and formatting text content."));

        add(createSection("Paragraph",
            "The Paragraph component represents a paragraph of text.",
            createParagraphDemo()));

        add(createSection("Pre (Preformatted Text)",
            "The Pre component displays preformatted text preserving whitespace.",
            createPreDemo()));

        add(createSection("Code",
            "The Code component represents a fragment of computer code.",
            createCodeDemo()));

        add(createSection("Emphasis",
            "The Emphasis component represents emphasized text (italic).",
            createEmphasisDemo()));

        add(createSection("Abbreviation (Abbr)",
            "The Abbr component represents an abbreviation with an optional title.",
            createAbbrDemo()));
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

    private Div createParagraphDemo() {
        Div demo = new Div();
        demo.setId("paragraph-demo");

        Paragraph p1 = new Paragraph("This is a simple paragraph with some text content.");
        p1.setId("simple-paragraph");

        Paragraph p2 = new Paragraph();
        p2.setId("styled-paragraph");
        p2.setText("This paragraph has custom styling applied.");
        p2.getStyle()
            .set("font-style", "italic")
            .set("color", "#555")
            .set("padding", "15px")
            .set("background-color", "#f9f9f9")
            .set("border-left", "4px solid #1976d2");

        Paragraph p3 = new Paragraph("This is a long paragraph that demonstrates how text wraps. " +
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor " +
            "incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud " +
            "exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.");
        p3.setId("long-paragraph");
        p3.getStyle().set("text-align", "justify");

        // Dynamic paragraph
        Paragraph dynamicP = new Paragraph("Click the button to update this paragraph.");
        dynamicP.setId("dynamic-paragraph");

        NativeButton updateBtn = new NativeButton("Update Paragraph", e -> {
            dynamicP.setText("The paragraph text has been updated at: " +
                java.time.LocalTime.now().toString());
        });
        updateBtn.setId("update-paragraph-btn");

        demo.add(p1, p2, p3, dynamicP, updateBtn);
        return demo;
    }

    private Div createPreDemo() {
        Div demo = new Div();
        demo.setId("pre-demo");

        Pre pre1 = new Pre("""
            function greet(name) {
                console.log("Hello, " + name + "!");
                return true;
            }

            greet("World");""");
        pre1.setId("code-pre");
        pre1.getStyle()
            .set("background-color", "#2d2d2d")
            .set("color", "#f8f8f2")
            .set("padding", "15px")
            .set("border-radius", "8px")
            .set("overflow-x", "auto");

        Pre pre2 = new Pre("""
            Name:     John Doe
            Email:    john@example.com
            Phone:    +1 234 567 8900
            Address:  123 Main Street
                      New York, NY 10001""");
        pre2.setId("formatted-pre");
        pre2.getStyle()
            .set("background-color", "#f5f5f5")
            .set("padding", "15px")
            .set("border", "1px solid #ddd")
            .set("border-radius", "4px");

        demo.add(pre1, pre2);
        return demo;
    }

    private Div createCodeDemo() {
        Div demo = new Div();
        demo.setId("code-demo");

        Paragraph inlineCode = new Paragraph();
        inlineCode.setId("inline-code-paragraph");
        inlineCode.setText("Use the ");
        Code code1 = new Code("console.log()");
        code1.setId("inline-code-1");
        code1.getStyle()
            .set("background-color", "#f0f0f0")
            .set("padding", "2px 6px")
            .set("border-radius", "4px")
            .set("font-family", "monospace");
        inlineCode.add(code1);
        inlineCode.add(" method to debug your JavaScript.");

        Code blockCode = new Code("const result = array.map(x => x * 2).filter(x => x > 10);");
        blockCode.setId("block-code");
        blockCode.getStyle()
            .set("display", "block")
            .set("background-color", "#1e1e1e")
            .set("color", "#9cdcfe")
            .set("padding", "15px")
            .set("border-radius", "8px")
            .set("font-family", "monospace")
            .set("margin-top", "10px");

        demo.add(inlineCode, blockCode);
        return demo;
    }

    private Div createEmphasisDemo() {
        Div demo = new Div();
        demo.setId("emphasis-demo");

        Paragraph emphasisParagraph = new Paragraph();
        emphasisParagraph.setId("emphasis-paragraph");
        emphasisParagraph.setText("This text contains ");
        Emphasis em1 = new Emphasis("emphasized content");
        em1.setId("emphasis-1");
        emphasisParagraph.add(em1);
        emphasisParagraph.add(" within a regular paragraph.");

        Emphasis styledEmphasis = new Emphasis("This entire text is emphasized with custom styling");
        styledEmphasis.setId("styled-emphasis");
        styledEmphasis.getStyle()
            .set("display", "block")
            .set("font-size", "1.2em")
            .set("color", "#d32f2f")
            .set("margin-top", "10px");

        demo.add(emphasisParagraph, styledEmphasis);
        return demo;
    }

    private Div createAbbrDemo() {
        Div demo = new Div();
        demo.setId("abbr-demo");

        Paragraph abbrParagraph = new Paragraph();
        abbrParagraph.setId("abbr-paragraph");
        abbrParagraph.setText("The ");
        Abbr abbr1 = new Abbr("HTML");
        abbr1.setId("abbr-html");
        abbr1.setTitle("HyperText Markup Language");
        abbr1.getStyle()
            .set("text-decoration", "underline dotted")
            .set("cursor", "help");
        abbrParagraph.add(abbr1);
        abbrParagraph.add(" specification is maintained by ");
        Abbr abbr2 = new Abbr("W3C");
        abbr2.setId("abbr-w3c");
        abbr2.setTitle("World Wide Web Consortium");
        abbr2.getStyle()
            .set("text-decoration", "underline dotted")
            .set("cursor", "help");
        abbrParagraph.add(abbr2);
        abbrParagraph.add(".");

        Paragraph hint = new Paragraph("(Hover over the abbreviations to see their full forms)");
        hint.getStyle()
            .set("font-size", "0.9em")
            .set("color", "#666")
            .set("font-style", "italic");

        demo.add(abbrParagraph, hint);
        return demo;
    }
}
