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
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.sampler.SamplerMainLayout;

/**
 * Demonstrates the Section component and its features.
 */
@Route(value = "section", layout = SamplerMainLayout.class)
@PageTitle("Section Sampler")
public class SectionSamplerView extends Div {

    public SectionSamplerView() {
        setId("section-sampler");

        add(new H1("Section Component"));
        add(new Paragraph("The Section component represents a standalone section of a document."));

        add(createDemoSection("Basic Section",
            "A simple section with a heading and content.",
            createBasicSectionDemo()));

        add(createDemoSection("Multiple Sections",
            "Multiple sections can be used to organize content.",
            createMultipleSectionsDemo()));

        add(createDemoSection("Styled Sections",
            "Sections with different visual styles.",
            createStyledSectionsDemo()));

        add(createDemoSection("Nested Sections",
            "Sections can be nested within other sections.",
            createNestedSectionsDemo()));
    }

    private Div createDemoSection(String title, String description, Div content) {
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

    private Div createBasicSectionDemo() {
        Div demo = new Div();
        demo.setId("basic-section");

        Section section = new Section();
        section.setId("sample-section");
        section.getStyle()
            .set("padding", "20px")
            .set("background-color", "#f5f5f5")
            .set("border-radius", "8px");

        H3 sectionTitle = new H3("Introduction");
        sectionTitle.getStyle().set("margin-top", "0");

        Paragraph content = new Paragraph(
            "This is a basic section. Sections are used to group related content together " +
            "and typically have a heading. They help to organize and structure documents semantically.");

        section.add(sectionTitle, content);
        demo.add(section);
        return demo;
    }

    private Div createMultipleSectionsDemo() {
        Div demo = new Div();
        demo.setId("multiple-sections");

        String[][] sectionData = {
            {"About Us", "We are a company dedicated to building amazing software."},
            {"Our Mission", "To empower developers with the best tools and frameworks."},
            {"Contact", "Reach out to us at contact@example.com"}
        };

        String[] colors = {"#e8f5e9", "#e3f2fd", "#fff3e0"};

        for (int i = 0; i < sectionData.length; i++) {
            Section section = new Section();
            section.setId("section-" + (i + 1));
            section.getStyle()
                .set("padding", "20px")
                .set("margin-bottom", "15px")
                .set("background-color", colors[i])
                .set("border-radius", "8px");

            H3 title = new H3(sectionData[i][0]);
            title.getStyle().set("margin-top", "0");

            Paragraph content = new Paragraph(sectionData[i][1]);

            section.add(title, content);
            demo.add(section);
        }

        return demo;
    }

    private Div createStyledSectionsDemo() {
        Div demo = new Div();
        demo.setId("styled-sections");

        // Card-style section
        Section cardSection = new Section();
        cardSection.setId("card-section");
        cardSection.getStyle()
            .set("padding", "25px")
            .set("background-color", "white")
            .set("border-radius", "12px")
            .set("box-shadow", "0 4px 6px rgba(0,0,0,0.1)")
            .set("margin-bottom", "20px");

        H3 cardTitle = new H3("Card Style Section");
        cardTitle.getStyle().set("margin-top", "0").set("color", "#1976d2");
        cardSection.add(cardTitle, new Paragraph("This section is styled like a card with shadow."));

        // Bordered section
        Section borderedSection = new Section();
        borderedSection.setId("bordered-section");
        borderedSection.getStyle()
            .set("padding", "20px")
            .set("border", "2px solid #4caf50")
            .set("border-radius", "8px")
            .set("margin-bottom", "20px");

        H3 borderedTitle = new H3("Bordered Section");
        borderedTitle.getStyle().set("margin-top", "0").set("color", "#4caf50");
        borderedSection.add(borderedTitle, new Paragraph("This section has a colored border."));

        // Gradient section
        Section gradientSection = new Section();
        gradientSection.setId("gradient-section");
        gradientSection.getStyle()
            .set("padding", "25px")
            .set("background", "linear-gradient(135deg, #667eea 0%, #764ba2 100%)")
            .set("color", "white")
            .set("border-radius", "12px");

        H3 gradientTitle = new H3("Gradient Section");
        gradientTitle.getStyle().set("margin-top", "0").set("color", "white");
        gradientSection.add(gradientTitle, new Paragraph("This section has a gradient background."));

        demo.add(cardSection, borderedSection, gradientSection);
        return demo;
    }

    private Div createNestedSectionsDemo() {
        Div demo = new Div();
        demo.setId("nested-sections");

        Section outerSection = new Section();
        outerSection.setId("outer-section");
        outerSection.getStyle()
            .set("padding", "20px")
            .set("background-color", "#e8eaf6")
            .set("border-radius", "8px");

        H3 outerTitle = new H3("Main Section");
        outerTitle.getStyle().set("margin-top", "0");

        Paragraph outerContent = new Paragraph("This is the main section containing subsections.");

        Section subSection1 = new Section();
        subSection1.setId("sub-section-1");
        subSection1.getStyle()
            .set("padding", "15px")
            .set("margin-top", "15px")
            .set("background-color", "#c5cae9")
            .set("border-radius", "6px");

        H3 sub1Title = new H3("Subsection 1");
        sub1Title.getStyle().set("margin-top", "0").set("font-size", "1.1em");
        subSection1.add(sub1Title, new Paragraph("Content of the first subsection."));

        Section subSection2 = new Section();
        subSection2.setId("sub-section-2");
        subSection2.getStyle()
            .set("padding", "15px")
            .set("margin-top", "10px")
            .set("background-color", "#c5cae9")
            .set("border-radius", "6px");

        H3 sub2Title = new H3("Subsection 2");
        sub2Title.getStyle().set("margin-top", "0").set("font-size", "1.1em");
        subSection2.add(sub2Title, new Paragraph("Content of the second subsection."));

        outerSection.add(outerTitle, outerContent, subSection1, subSection2);
        demo.add(outerSection);
        return demo;
    }
}
