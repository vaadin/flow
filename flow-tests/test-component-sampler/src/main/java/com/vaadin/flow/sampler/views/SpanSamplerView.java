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
 * Demonstrates the Span component and its features.
 */
@Route(value = "span", layout = SamplerMainLayout.class)
@PageTitle("Span Sampler")
public class SpanSamplerView extends Div {

    public SpanSamplerView() {
        setId("span-sampler");

        add(new H1("Span Component"));
        add(new Paragraph("The Span component is an inline container for text and other inline elements."));

        add(createSection("Basic Span",
            "Simple span with text content.",
            createBasicSpanDemo()));

        add(createSection("Inline Styling",
            "Spans are commonly used to style portions of text.",
            createInlineStylingDemo()));

        add(createSection("Nested Spans",
            "Spans can be nested within other components.",
            createNestedSpansDemo()));

        add(createSection("Badge Examples",
            "Spans are perfect for creating badge/tag elements.",
            createBadgeDemo()));

        add(createSection("Dynamic Content",
            "Span content can be updated dynamically.",
            createDynamicSpanDemo()));
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

    private Div createBasicSpanDemo() {
        Div demo = new Div();
        demo.setId("basic-span");

        Span simple = new Span("This is a simple span");
        simple.setId("simple-span");

        Span withId = new Span("Span with ID and title");
        withId.setId("span-with-id");
        withId.setTitle("This is the title attribute");

        demo.add(simple, new Div(), withId);
        return demo;
    }

    private Div createInlineStylingDemo() {
        Div demo = new Div();
        demo.setId("inline-styling");

        Paragraph styledText = new Paragraph();
        styledText.setId("styled-text");

        styledText.add("This text has ");

        Span redSpan = new Span("red");
        redSpan.setId("red-span");
        redSpan.getStyle().set("color", "red").set("font-weight", "bold");
        styledText.add(redSpan);

        styledText.add(", ");

        Span greenSpan = new Span("green");
        greenSpan.setId("green-span");
        greenSpan.getStyle().set("color", "green").set("font-weight", "bold");
        styledText.add(greenSpan);

        styledText.add(", and ");

        Span blueSpan = new Span("blue");
        blueSpan.setId("blue-span");
        blueSpan.getStyle().set("color", "blue").set("font-weight", "bold");
        styledText.add(blueSpan);

        styledText.add(" words.");

        Paragraph highlightedText = new Paragraph();
        highlightedText.setId("highlighted-text");
        highlightedText.add("You can also ");
        Span highlight = new Span("highlight important text");
        highlight.setId("highlight-span");
        highlight.getStyle()
            .set("background-color", "#ffeb3b")
            .set("padding", "2px 4px");
        highlightedText.add(highlight);
        highlightedText.add(" using background colors.");

        demo.add(styledText, highlightedText);
        return demo;
    }

    private Div createNestedSpansDemo() {
        Div demo = new Div();
        demo.setId("nested-spans");

        Paragraph nestedExample = new Paragraph();
        nestedExample.setId("nested-example");

        Span outer = new Span("Outer span containing ");
        outer.setId("outer-span");
        outer.getStyle()
            .set("background-color", "#e3f2fd")
            .set("padding", "5px 10px")
            .set("border-radius", "4px");

        Span middle = new Span("a middle span with ");
        middle.setId("middle-span");
        middle.getStyle()
            .set("background-color", "#bbdefb")
            .set("padding", "3px 6px")
            .set("border-radius", "4px");

        Span inner = new Span("an inner span");
        inner.setId("inner-span");
        inner.getStyle()
            .set("background-color", "#90caf9")
            .set("padding", "2px 4px")
            .set("border-radius", "4px")
            .set("font-weight", "bold");

        middle.add(inner);
        outer.add(middle);
        nestedExample.add(outer);

        demo.add(nestedExample);
        return demo;
    }

    private Div createBadgeDemo() {
        Div demo = new Div();
        demo.setId("badge-demo");

        Div badgeContainer = new Div();
        badgeContainer.getStyle()
            .set("display", "flex")
            .set("gap", "10px")
            .set("flex-wrap", "wrap");

        String[] badges = {"Primary", "Success", "Warning", "Error", "Info"};
        String[] colors = {"#1976d2", "#388e3c", "#f57c00", "#d32f2f", "#0288d1"};

        for (int i = 0; i < badges.length; i++) {
            Span badge = new Span(badges[i]);
            badge.setId("badge-" + badges[i].toLowerCase());
            badge.getStyle()
                .set("background-color", colors[i])
                .set("color", "white")
                .set("padding", "4px 12px")
                .set("border-radius", "16px")
                .set("font-size", "0.85em")
                .set("font-weight", "500");
            badgeContainer.add(badge);
        }

        // Counter badge example
        Div counterExample = new Div();
        counterExample.getStyle().set("margin-top", "20px");

        Span notificationBadge = new Span("Notifications ");
        notificationBadge.setId("notification-badge");

        Span counter = new Span("5");
        counter.setId("badge-counter");
        counter.getStyle()
            .set("background-color", "#f44336")
            .set("color", "white")
            .set("padding", "2px 8px")
            .set("border-radius", "50%")
            .set("font-size", "0.8em")
            .set("margin-left", "5px");

        notificationBadge.add(counter);
        counterExample.add(notificationBadge);

        demo.add(badgeContainer, counterExample);
        return demo;
    }

    private Div createDynamicSpanDemo() {
        Div demo = new Div();
        demo.setId("dynamic-span");

        Span dynamicSpan = new Span("Initial text");
        dynamicSpan.setId("dynamic-span-text");
        dynamicSpan.getStyle()
            .set("font-size", "1.2em")
            .set("padding", "10px")
            .set("background-color", "#f5f5f5")
            .set("display", "inline-block")
            .set("margin-bottom", "10px");

        Div buttons = new Div();
        buttons.getStyle().set("display", "flex").set("gap", "10px");

        NativeButton setText1 = new NativeButton("Set Text 1", e -> {
            dynamicSpan.setText("Hello, World!");
            dynamicSpan.getStyle().set("background-color", "#e8f5e9");
        });
        setText1.setId("set-text-1");

        NativeButton setText2 = new NativeButton("Set Text 2", e -> {
            dynamicSpan.setText("Vaadin Flow is awesome!");
            dynamicSpan.getStyle().set("background-color", "#e3f2fd");
        });
        setText2.setId("set-text-2");

        NativeButton clearText = new NativeButton("Clear", e -> {
            dynamicSpan.setText("");
            dynamicSpan.getStyle().set("background-color", "#ffebee");
        });
        clearText.setId("clear-text");

        buttons.add(setText1, setText2, clearText);
        demo.add(dynamicSpan, buttons);
        return demo;
    }
}
