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
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.sampler.SamplerMainLayout;

/**
 * Home view for the Component Sampler application.
 */
@Route(value = "", layout = SamplerMainLayout.class)
@PageTitle("Flow Component Sampler")
public class SamplerHomeView extends Div {

    public SamplerHomeView() {
        setId("sampler-home");

        H1 title = new H1("Vaadin Flow Component Sampler");
        title.setId("home-title");

        Paragraph intro = new Paragraph(
            "Welcome to the Vaadin Flow Component Sampler! This application demonstrates " +
            "all the HTML components available in Flow and their various features."
        );
        intro.setId("home-intro");

        H2 featuresTitle = new H2("Available Components");

        UnorderedList features = new UnorderedList();
        features.setId("feature-list");
        features.add(
            new ListItem("Text Components: H1-H6, Paragraph, Span, Pre, Code, Emphasis, etc."),
            new ListItem("Container Components: Div, Article, Aside, Section, Header, Footer, Main, Nav, FieldSet, Details"),
            new ListItem("Interactive Components: NativeButton, Input, RangeInput, Anchor"),
            new ListItem("Media Components: Image, IFrame, HtmlObject"),
            new ListItem("List Components: OrderedList, UnorderedList, ListItem, DescriptionList"),
            new ListItem("Table Components: NativeTable and related elements")
        );

        H2 howToTitle = new H2("How to Use");
        Paragraph howTo = new Paragraph(
            "Use the navigation menu on the left to browse different component categories. " +
            "Each component page shows various usage examples with live demonstrations."
        );

        add(title, intro, featuresTitle, features, howToTitle, howTo);
    }
}
