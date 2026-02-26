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
package com.vaadin.flow.sampler;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.sampler.views.AnchorSamplerView;
import com.vaadin.flow.sampler.views.ArticleSamplerView;
import com.vaadin.flow.sampler.views.ButtonSamplerView;
import com.vaadin.flow.sampler.views.DescriptionListSamplerView;
import com.vaadin.flow.sampler.views.DetailsSamplerView;
import com.vaadin.flow.sampler.views.DivSamplerView;
import com.vaadin.flow.sampler.views.FieldSetSamplerView;
import com.vaadin.flow.sampler.views.HeadingSamplerView;
import com.vaadin.flow.sampler.views.IFrameSamplerView;
import com.vaadin.flow.sampler.views.ImageSamplerView;
import com.vaadin.flow.sampler.views.InputSamplerView;
import com.vaadin.flow.sampler.views.LabelSamplerView;
import com.vaadin.flow.sampler.views.ListSamplerView;
import com.vaadin.flow.sampler.views.SamplerHomeView;
import com.vaadin.flow.sampler.views.SectionSamplerView;
import com.vaadin.flow.sampler.views.SpanSamplerView;
import com.vaadin.flow.sampler.views.TableSamplerView;
import com.vaadin.flow.sampler.views.TextSamplerView;

/**
 * Main layout for the Component Sampler application.
 * Provides navigation to all component demonstration views.
 */
public class SamplerMainLayout extends Div implements RouterLayout {

    public SamplerMainLayout() {
        setId("sampler-layout");
        getStyle()
            .set("display", "flex")
            .set("min-height", "100vh");

        Nav navigation = createNavigation();
        Div content = createContentArea();

        add(navigation, content);
    }

    private Nav createNavigation() {
        Nav nav = new Nav();
        nav.setId("sampler-nav");
        nav.getStyle()
            .set("width", "250px")
            .set("min-width", "250px")
            .set("background-color", "#f5f5f5")
            .set("padding", "20px")
            .set("border-right", "1px solid #ddd")
            .set("overflow-y", "auto");

        H3 title = new H3("Component Sampler");
        title.getStyle().set("margin-top", "0");
        nav.add(title);

        // Home link
        nav.add(createNavLink("Home", SamplerHomeView.class));

        // Text Components Section
        Div textSection = createNavSection("Text Components");
        textSection.add(createNavLink("Headings (H1-H6)", HeadingSamplerView.class));
        textSection.add(createNavLink("Paragraph & Text", TextSamplerView.class));
        textSection.add(createNavLink("Span", SpanSamplerView.class));
        textSection.add(createNavLink("Label", LabelSamplerView.class));
        nav.add(textSection);

        // Container Components Section
        Div containerSection = createNavSection("Container Components");
        containerSection.add(createNavLink("Div", DivSamplerView.class));
        containerSection.add(createNavLink("Article & Aside", ArticleSamplerView.class));
        containerSection.add(createNavLink("Section", SectionSamplerView.class));
        containerSection.add(createNavLink("FieldSet", FieldSetSamplerView.class));
        containerSection.add(createNavLink("Details", DetailsSamplerView.class));
        nav.add(containerSection);

        // Interactive Components Section
        Div interactiveSection = createNavSection("Interactive Components");
        interactiveSection.add(createNavLink("Native Button", ButtonSamplerView.class));
        interactiveSection.add(createNavLink("Input", InputSamplerView.class));
        interactiveSection.add(createNavLink("Anchor", AnchorSamplerView.class));
        nav.add(interactiveSection);

        // Media Components Section
        Div mediaSection = createNavSection("Media Components");
        mediaSection.add(createNavLink("Image", ImageSamplerView.class));
        mediaSection.add(createNavLink("IFrame", IFrameSamplerView.class));
        nav.add(mediaSection);

        // List Components Section
        Div listSection = createNavSection("List Components");
        listSection.add(createNavLink("Lists (UL, OL)", ListSamplerView.class));
        listSection.add(createNavLink("Description List", DescriptionListSamplerView.class));
        nav.add(listSection);

        // Table Components Section
        Div tableSection = createNavSection("Table Components");
        tableSection.add(createNavLink("Native Table", TableSamplerView.class));
        nav.add(tableSection);

        return nav;
    }

    private Div createNavSection(String title) {
        Div section = new Div();
        section.getStyle()
            .set("margin-top", "20px")
            .set("margin-bottom", "10px");

        Div sectionTitle = new Div(title);
        sectionTitle.getStyle()
            .set("font-weight", "bold")
            .set("color", "#666")
            .set("font-size", "0.85em")
            .set("text-transform", "uppercase")
            .set("margin-bottom", "8px");
        section.add(sectionTitle);

        return section;
    }

    private RouterLink createNavLink(String text, Class<? extends com.vaadin.flow.component.Component> target) {
        RouterLink link = new RouterLink(text, target);
        link.getStyle()
            .set("display", "block")
            .set("padding", "8px 12px")
            .set("color", "#333")
            .set("text-decoration", "none")
            .set("border-radius", "4px")
            .set("margin-bottom", "4px");
        link.getElement().addEventListener("mouseover",
            e -> {}).addEventData("element.style.backgroundColor='#e0e0e0'");
        return link;
    }

    private Div createContentArea() {
        Div content = new Div();
        content.setId("sampler-content");
        content.getStyle()
            .set("flex", "1")
            .set("padding", "30px")
            .set("overflow-y", "auto");
        return content;
    }
}
