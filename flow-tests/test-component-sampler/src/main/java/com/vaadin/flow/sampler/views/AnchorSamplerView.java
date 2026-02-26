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

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.sampler.SamplerMainLayout;

/**
 * Demonstrates the Anchor component and its features.
 */
@Route(value = "anchor", layout = SamplerMainLayout.class)
@PageTitle("Anchor Sampler")
public class AnchorSamplerView extends Div {

    public AnchorSamplerView() {
        setId("anchor-sampler");

        add(new H1("Anchor Component"));
        add(new Paragraph("The Anchor component creates hyperlinks to other resources."));

        add(createSection("Basic Anchors",
            "Simple links with text.",
            createBasicAnchorsDemo()));

        add(createSection("Target Attributes",
            "Links with different target attributes.",
            createTargetDemo()));

        add(createSection("Styled Anchors",
            "Links with custom styling.",
            createStyledAnchorsDemo()));

        add(createSection("Anchors with Components",
            "Anchors can wrap other components.",
            createAnchorsWithComponentsDemo()));

        add(createSection("Download Links",
            "Links configured for downloads.",
            createDownloadLinksDemo()));
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

    private Div createBasicAnchorsDemo() {
        Div demo = new Div();
        demo.setId("basic-anchors");

        Paragraph text = new Paragraph();
        text.add("Visit ");
        Anchor simpleLink = new Anchor("https://vaadin.com", "Vaadin Website");
        simpleLink.setId("simple-anchor");
        text.add(simpleLink);
        text.add(" for more information about the framework.");

        Anchor linkWithTitle = new Anchor("https://vaadin.com/docs", "Documentation");
        linkWithTitle.setId("anchor-with-title");
        linkWithTitle.setTitle("Opens Vaadin documentation");
        linkWithTitle.getStyle().set("display", "block").set("margin-top", "10px");

        Anchor internalLink = new Anchor("", "Return to Home");
        internalLink.setId("internal-anchor");
        internalLink.getStyle().set("display", "block").set("margin-top", "10px");

        demo.add(text, linkWithTitle, internalLink);
        return demo;
    }

    private Div createTargetDemo() {
        Div demo = new Div();
        demo.setId("target-demo");

        Div container = new Div();
        container.getStyle()
            .set("display", "flex")
            .set("flex-direction", "column")
            .set("gap", "15px");

        // _self target
        Div selfRow = new Div();
        Anchor selfLink = new Anchor("https://vaadin.com", "Open in same tab");
        selfLink.setId("self-target-anchor");
        selfLink.setTarget(AnchorTarget.SELF);
        Span selfDesc = new Span(" - target=\"_self\" (default)");
        selfDesc.getStyle().set("color", "#666");
        selfRow.add(selfLink, selfDesc);

        // _blank target
        Div blankRow = new Div();
        Anchor blankLink = new Anchor("https://vaadin.com", "Open in new tab");
        blankLink.setId("blank-target-anchor");
        blankLink.setTarget(AnchorTarget.BLANK);
        Span blankDesc = new Span(" - target=\"_blank\"");
        blankDesc.getStyle().set("color", "#666");
        blankRow.add(blankLink, blankDesc);

        // _parent target
        Div parentRow = new Div();
        Anchor parentLink = new Anchor("https://vaadin.com", "Open in parent frame");
        parentLink.setId("parent-target-anchor");
        parentLink.setTarget(AnchorTarget.PARENT);
        Span parentDesc = new Span(" - target=\"_parent\"");
        parentDesc.getStyle().set("color", "#666");
        parentRow.add(parentLink, parentDesc);

        // _top target
        Div topRow = new Div();
        Anchor topLink = new Anchor("https://vaadin.com", "Open in top frame");
        topLink.setId("top-target-anchor");
        topLink.setTarget(AnchorTarget.TOP);
        Span topDesc = new Span(" - target=\"_top\"");
        topDesc.getStyle().set("color", "#666");
        topRow.add(topLink, topDesc);

        container.add(selfRow, blankRow, parentRow, topRow);
        demo.add(container);
        return demo;
    }

    private Div createStyledAnchorsDemo() {
        Div demo = new Div();
        demo.setId("styled-anchors");
        demo.getStyle()
            .set("display", "flex")
            .set("flex-wrap", "wrap")
            .set("gap", "15px");

        // Button-style link
        Anchor buttonLink = new Anchor("#", "Button Style Link");
        buttonLink.setId("button-style-anchor");
        buttonLink.getStyle()
            .set("display", "inline-block")
            .set("padding", "10px 20px")
            .set("background-color", "#1976d2")
            .set("color", "white")
            .set("text-decoration", "none")
            .set("border-radius", "4px")
            .set("font-weight", "500");

        // Card link
        Anchor cardLink = new Anchor("#", "Card Style Link");
        cardLink.setId("card-style-anchor");
        cardLink.getStyle()
            .set("display", "inline-block")
            .set("padding", "20px 30px")
            .set("background-color", "#f5f5f5")
            .set("color", "#333")
            .set("text-decoration", "none")
            .set("border-radius", "8px")
            .set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)");

        // Underline animation link
        Anchor animatedLink = new Anchor("#", "Animated Underline");
        animatedLink.setId("animated-anchor");
        animatedLink.getStyle()
            .set("color", "#1976d2")
            .set("text-decoration", "none")
            .set("border-bottom", "2px solid transparent")
            .set("padding-bottom", "2px")
            .set("transition", "border-color 0.3s");

        // Icon link (using text symbol)
        Anchor iconLink = new Anchor("#", "External Link");
        iconLink.setId("icon-anchor");
        iconLink.getElement().setProperty("innerHTML", "External Link &#8599;");
        iconLink.getStyle()
            .set("color", "#1976d2")
            .set("text-decoration", "none");

        demo.add(buttonLink, cardLink, animatedLink, iconLink);
        return demo;
    }

    private Div createAnchorsWithComponentsDemo() {
        Div demo = new Div();
        demo.setId("anchors-with-components");
        demo.getStyle()
            .set("display", "flex")
            .set("gap", "20px")
            .set("flex-wrap", "wrap");

        // Anchor wrapping a card
        Anchor cardAnchor = new Anchor("#");
        cardAnchor.setId("card-anchor");
        cardAnchor.getStyle()
            .set("display", "block")
            .set("text-decoration", "none")
            .set("color", "inherit");

        Div card = new Div();
        card.getStyle()
            .set("padding", "20px")
            .set("border", "1px solid #e0e0e0")
            .set("border-radius", "8px")
            .set("width", "200px")
            .set("transition", "box-shadow 0.3s");

        Div cardTitle = new Div("Learn Vaadin");
        cardTitle.getStyle()
            .set("font-weight", "bold")
            .set("margin-bottom", "10px")
            .set("color", "#1976d2");

        Div cardDesc = new Div("Click to learn more about building web apps with Vaadin Flow.");
        cardDesc.getStyle()
            .set("font-size", "0.9em")
            .set("color", "#666");

        card.add(cardTitle, cardDesc);
        cardAnchor.add(card);

        // Anchor with badge
        Anchor badgeAnchor = new Anchor("#");
        badgeAnchor.setId("badge-anchor");
        badgeAnchor.getStyle()
            .set("display", "inline-flex")
            .set("align-items", "center")
            .set("gap", "8px")
            .set("padding", "10px 15px")
            .set("background-color", "#f5f5f5")
            .set("border-radius", "20px")
            .set("text-decoration", "none")
            .set("color", "#333");

        Span text = new Span("Notifications");
        Span badge = new Span("5");
        badge.getStyle()
            .set("background-color", "#f44336")
            .set("color", "white")
            .set("padding", "2px 8px")
            .set("border-radius", "10px")
            .set("font-size", "0.8em");

        badgeAnchor.add(text, badge);

        demo.add(cardAnchor, badgeAnchor);
        return demo;
    }

    private Div createDownloadLinksDemo() {
        Div demo = new Div();
        demo.setId("download-anchors");

        Paragraph intro = new Paragraph(
            "Download links use the download attribute to prompt file downloads.");
        intro.getStyle().set("margin-bottom", "15px");

        Div container = new Div();
        container.getStyle()
            .set("display", "flex")
            .set("flex-direction", "column")
            .set("gap", "10px");

        Anchor pdfDownload = new Anchor("#", "Download PDF");
        pdfDownload.setId("pdf-download");
        pdfDownload.getElement().setAttribute("download", "document.pdf");
        pdfDownload.getStyle()
            .set("display", "inline-flex")
            .set("align-items", "center")
            .set("color", "#d32f2f")
            .set("text-decoration", "none");

        Anchor csvDownload = new Anchor("#", "Download CSV");
        csvDownload.setId("csv-download");
        csvDownload.getElement().setAttribute("download", "data.csv");
        csvDownload.getStyle()
            .set("display", "inline-flex")
            .set("align-items", "center")
            .set("color", "#388e3c")
            .set("text-decoration", "none");

        Anchor zipDownload = new Anchor("#", "Download ZIP Archive");
        zipDownload.setId("zip-download");
        zipDownload.getElement().setAttribute("download", "archive.zip");
        zipDownload.getStyle()
            .set("display", "inline-flex")
            .set("align-items", "center")
            .set("color", "#f57c00")
            .set("text-decoration", "none");

        Paragraph note = new Paragraph("Note: These are demonstration links. In a real application, " +
            "the href would point to actual file resources.");
        note.getStyle()
            .set("font-size", "0.85em")
            .set("color", "#666")
            .set("font-style", "italic")
            .set("margin-top", "15px");

        container.add(pdfDownload, csvDownload, zipDownload);
        demo.add(intro, container, note);
        return demo;
    }
}
