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
import com.vaadin.flow.component.html.NativeDetails;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.sampler.SamplerMainLayout;

/**
 * Demonstrates the NativeDetails component and its features.
 */
@Route(value = "details", layout = SamplerMainLayout.class)
@PageTitle("Details Sampler")
public class DetailsSamplerView extends Div {

    public DetailsSamplerView() {
        setId("details-sampler");

        add(new H1("NativeDetails Component"));
        add(new Paragraph("The NativeDetails component creates a disclosure widget where content is visible only when toggled open."));

        add(createSection("Basic Details",
            "Simple details with summary and content.",
            createBasicDetailsDemo()));

        add(createSection("Initially Open",
            "Details can be set to open by default.",
            createInitiallyOpenDemo()));

        add(createSection("Accordion Style",
            "Multiple details arranged as an accordion.",
            createAccordionDemo()));

        add(createSection("Styled Details",
            "Details with custom styling.",
            createStyledDetailsDemo()));

        add(createSection("Programmatic Control",
            "Open and close details programmatically.",
            createProgrammaticDemo()));
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

    private Div createBasicDetailsDemo() {
        Div demo = new Div();
        demo.setId("basic-details");

        NativeDetails details = new NativeDetails();
        details.setId("basic-details-widget");
        details.setSummaryText("Click to expand");
        details.getStyle()
            .set("padding", "10px")
            .set("border", "1px solid #ddd")
            .set("border-radius", "4px");

        Paragraph content = new Paragraph(
            "This is the hidden content that appears when you expand the details widget. " +
            "It can contain any HTML content including text, images, and other components.");
        details.add(content);

        demo.add(details);
        return demo;
    }

    private Div createInitiallyOpenDemo() {
        Div demo = new Div();
        demo.setId("initially-open");

        NativeDetails openDetails = new NativeDetails();
        openDetails.setId("open-details-widget");
        openDetails.setSummaryText("I'm open by default");
        openDetails.setOpen(true);
        openDetails.getStyle()
            .set("padding", "10px")
            .set("border", "1px solid #ddd")
            .set("border-radius", "4px");

        openDetails.add(new Paragraph(
            "This content is visible immediately because the details element is set to open."));

        demo.add(openDetails);
        return demo;
    }

    private Div createAccordionDemo() {
        Div demo = new Div();
        demo.setId("accordion-details");

        String[][] faqData = {
            {"What is Vaadin Flow?", "Vaadin Flow is a Java web framework for building modern web applications. It allows you to write UI code entirely in Java while the framework handles all client-server communication."},
            {"How does it work?", "Flow maintains the UI state on the server and automatically synchronizes changes with the browser. When users interact with the UI, events are sent to the server where your Java code handles them."},
            {"What are the benefits?", "Benefits include type-safe Java development, automatic security, no need to write JavaScript, seamless backend integration, and a rich set of pre-built components."},
            {"How do I get started?", "You can start by using the Vaadin starter projects, reading the documentation, or following the tutorials available on vaadin.com."}
        };

        for (int i = 0; i < faqData.length; i++) {
            NativeDetails faq = new NativeDetails();
            faq.setId("faq-" + (i + 1));
            faq.setSummaryText(faqData[i][0]);
            faq.getStyle()
                .set("padding", "15px")
                .set("border", "1px solid #e0e0e0")
                .set("border-radius", i == 0 ? "8px 8px 0 0" : (i == faqData.length - 1 ? "0 0 8px 8px" : "0"))
                .set("margin-top", i == 0 ? "0" : "-1px")
                .set("background-color", "#fafafa");

            Paragraph answer = new Paragraph(faqData[i][1]);
            answer.getStyle().set("margin", "10px 0 0 0").set("color", "#555");
            faq.add(answer);

            demo.add(faq);
        }

        return demo;
    }

    private Div createStyledDetailsDemo() {
        Div demo = new Div();
        demo.setId("styled-details");

        // Info style
        NativeDetails infoDetails = new NativeDetails();
        infoDetails.setId("info-details");
        infoDetails.setSummaryText("Information");
        infoDetails.getStyle()
            .set("padding", "15px")
            .set("border", "1px solid #0288d1")
            .set("border-radius", "8px")
            .set("background-color", "#e1f5fe")
            .set("margin-bottom", "15px");

        infoDetails.add(new Paragraph("This is an informational message styled with blue colors."));

        // Success style
        NativeDetails successDetails = new NativeDetails();
        successDetails.setId("success-details");
        successDetails.setSummaryText("Success");
        successDetails.getStyle()
            .set("padding", "15px")
            .set("border", "1px solid #388e3c")
            .set("border-radius", "8px")
            .set("background-color", "#e8f5e9")
            .set("margin-bottom", "15px");

        successDetails.add(new Paragraph("This is a success message styled with green colors."));

        // Warning style
        NativeDetails warningDetails = new NativeDetails();
        warningDetails.setId("warning-details");
        warningDetails.setSummaryText("Warning");
        warningDetails.getStyle()
            .set("padding", "15px")
            .set("border", "1px solid #f57c00")
            .set("border-radius", "8px")
            .set("background-color", "#fff3e0")
            .set("margin-bottom", "15px");

        warningDetails.add(new Paragraph("This is a warning message styled with orange colors."));

        // Error style
        NativeDetails errorDetails = new NativeDetails();
        errorDetails.setId("error-details");
        errorDetails.setSummaryText("Error");
        errorDetails.getStyle()
            .set("padding", "15px")
            .set("border", "1px solid #d32f2f")
            .set("border-radius", "8px")
            .set("background-color", "#ffebee");

        errorDetails.add(new Paragraph("This is an error message styled with red colors."));

        demo.add(infoDetails, successDetails, warningDetails, errorDetails);
        return demo;
    }

    private Div createProgrammaticDemo() {
        Div demo = new Div();
        demo.setId("programmatic-details");

        NativeDetails controllableDetails = new NativeDetails();
        controllableDetails.setId("controllable-details");
        controllableDetails.setSummaryText("Controlled Details");
        controllableDetails.getStyle()
            .set("padding", "15px")
            .set("border", "1px solid #ddd")
            .set("border-radius", "8px")
            .set("margin-bottom", "15px");

        controllableDetails.add(new Paragraph("This content can be shown or hidden using the buttons below."));

        Span statusSpan = new Span("Status: Closed");
        statusSpan.setId("details-status");
        statusSpan.getStyle().set("font-weight", "500").set("margin-right", "20px");

        controllableDetails.addOpenedChangeListener(e -> {
            statusSpan.setText("Status: " + (e.isOpened() ? "Open" : "Closed"));
        });

        Div controls = new Div();
        controls.getStyle().set("display", "flex").set("gap", "10px").set("align-items", "center");

        NativeButton openBtn = new NativeButton("Open", e -> controllableDetails.setOpen(true));
        openBtn.setId("open-details-btn");

        NativeButton closeBtn = new NativeButton("Close", e -> controllableDetails.setOpen(false));
        closeBtn.setId("close-details-btn");

        NativeButton toggleBtn = new NativeButton("Toggle", e -> controllableDetails.setOpen(!controllableDetails.isOpen()));
        toggleBtn.setId("toggle-details-btn");

        controls.add(statusSpan, openBtn, closeBtn, toggleBtn);

        demo.add(controllableDetails, controls);
        return demo;
    }
}
