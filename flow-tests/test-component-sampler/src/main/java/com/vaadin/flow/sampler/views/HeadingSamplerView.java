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
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.H6;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.sampler.SamplerMainLayout;

/**
 * Demonstrates H1-H6 heading components and their features.
 */
@Route(value = "headings", layout = SamplerMainLayout.class)
@PageTitle("Headings Sampler")
public class HeadingSamplerView extends Div {

    public HeadingSamplerView() {
        setId("heading-sampler");

        add(new H1("Heading Components (H1-H6)"));
        add(new Paragraph("HTML heading elements represent six levels of section headings."));

        // Basic headings
        add(createSection("Basic Headings",
            "Headings from H1 (largest) to H6 (smallest).",
            createBasicHeadingsDemo()));

        // Headings with components
        add(createSection("Headings with Child Components",
            "Headings can contain other components.",
            createHeadingsWithComponentsDemo()));

        // Dynamic headings
        add(createSection("Dynamic Heading Text",
            "Heading text can be changed dynamically.",
            createDynamicHeadingsDemo()));

        // Styled headings
        add(createSection("Styled Headings",
            "Headings can be styled with CSS.",
            createStyledHeadingsDemo()));
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

    private Div createBasicHeadingsDemo() {
        Div demo = new Div();
        demo.setId("basic-headings");

        H1 h1 = new H1("Heading 1 (H1)");
        h1.setId("demo-h1");

        H2 h2 = new H2("Heading 2 (H2)");
        h2.setId("demo-h2");

        H3 h3 = new H3("Heading 3 (H3)");
        h3.setId("demo-h3");

        H4 h4 = new H4("Heading 4 (H4)");
        h4.setId("demo-h4");

        H5 h5 = new H5("Heading 5 (H5)");
        h5.setId("demo-h5");

        H6 h6 = new H6("Heading 6 (H6)");
        h6.setId("demo-h6");

        demo.add(h1, h2, h3, h4, h5, h6);
        return demo;
    }

    private Div createHeadingsWithComponentsDemo() {
        Div demo = new Div();
        demo.setId("headings-with-components");

        H2 h2WithIcon = new H2();
        h2WithIcon.setId("h2-with-icon");
        h2WithIcon.getElement().setProperty("innerHTML",
            "<span style='margin-right: 8px;'>&#9733;</span>Featured Section");

        H3 h3WithBadge = new H3();
        h3WithBadge.setId("h3-with-badge");
        Div badge = new Div("NEW");
        badge.getStyle()
            .set("display", "inline-block")
            .set("background-color", "#4caf50")
            .set("color", "white")
            .set("padding", "2px 8px")
            .set("border-radius", "4px")
            .set("font-size", "0.6em")
            .set("margin-left", "10px")
            .set("vertical-align", "middle");
        h3WithBadge.setText("Product Updates ");
        h3WithBadge.add(badge);

        demo.add(h2WithIcon, h3WithBadge);
        return demo;
    }

    private Div createDynamicHeadingsDemo() {
        Div demo = new Div();
        demo.setId("dynamic-headings");

        H3 dynamicH3 = new H3("Click the button to change this heading");
        dynamicH3.setId("dynamic-h3");

        int[] counter = {0};
        NativeButton changeButton = new NativeButton("Change Heading Text", e -> {
            counter[0]++;
            dynamicH3.setText("Heading changed " + counter[0] + " time(s)");
        });
        changeButton.setId("change-heading-btn");

        NativeButton resetButton = new NativeButton("Reset", e -> {
            counter[0] = 0;
            dynamicH3.setText("Click the button to change this heading");
        });
        resetButton.setId("reset-heading-btn");

        Div buttons = new Div(changeButton, resetButton);
        buttons.getStyle().set("margin-top", "10px");

        demo.add(dynamicH3, buttons);
        return demo;
    }

    private Div createStyledHeadingsDemo() {
        Div demo = new Div();
        demo.setId("styled-headings");

        H2 coloredH2 = new H2("Colored Heading");
        coloredH2.setId("colored-h2");
        coloredH2.getStyle()
            .set("color", "#1976d2")
            .set("border-bottom", "3px solid #1976d2")
            .set("padding-bottom", "10px");

        H3 gradientH3 = new H3("Gradient Background");
        gradientH3.setId("gradient-h3");
        gradientH3.getStyle()
            .set("background", "linear-gradient(90deg, #667eea 0%, #764ba2 100%)")
            .set("color", "white")
            .set("padding", "15px 20px")
            .set("border-radius", "8px");

        H4 shadowH4 = new H4("Text Shadow Effect");
        shadowH4.setId("shadow-h4");
        shadowH4.getStyle()
            .set("text-shadow", "2px 2px 4px rgba(0,0,0,0.3)")
            .set("font-size", "1.5em");

        demo.add(coloredH2, gradientH3, shadowH4);
        return demo;
    }
}
