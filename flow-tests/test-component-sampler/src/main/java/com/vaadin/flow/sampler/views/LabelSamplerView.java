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
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.sampler.SamplerMainLayout;

/**
 * Demonstrates the NativeLabel component and its features.
 */
@Route(value = "label", layout = SamplerMainLayout.class)
@PageTitle("Label Sampler")
public class LabelSamplerView extends Div {

    public LabelSamplerView() {
        setId("label-sampler");

        add(new H1("NativeLabel Component"));
        add(new Paragraph("The NativeLabel component represents a label for a form element."));

        add(createSection("Basic Label",
            "Simple label with text.",
            createBasicLabelDemo()));

        add(createSection("Label with For Attribute",
            "Labels can be associated with form elements using the 'for' attribute.",
            createLabelForDemo()));

        add(createSection("Styled Labels",
            "Labels can be styled with CSS.",
            createStyledLabelsDemo()));

        add(createSection("Required Field Labels",
            "Labels can indicate required fields.",
            createRequiredLabelsDemo()));
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

    private Div createBasicLabelDemo() {
        Div demo = new Div();
        demo.setId("basic-label");

        NativeLabel simpleLabel = new NativeLabel("Simple Label");
        simpleLabel.setId("simple-label");

        NativeLabel labelWithText = new NativeLabel();
        labelWithText.setId("label-with-text");
        labelWithText.setText("Label set via setText()");

        demo.add(simpleLabel, new Div(), labelWithText);
        return demo;
    }

    private Div createLabelForDemo() {
        Div demo = new Div();
        demo.setId("label-for-demo");

        // First field
        Div field1 = new Div();
        field1.getStyle().set("margin-bottom", "15px");

        NativeLabel nameLabel = new NativeLabel("Name:");
        nameLabel.setId("name-label");
        nameLabel.setFor("name-input");
        nameLabel.getStyle()
            .set("display", "block")
            .set("margin-bottom", "5px")
            .set("font-weight", "500");

        Input nameInput = new Input();
        nameInput.setId("name-input");
        nameInput.setPlaceholder("Enter your name");
        nameInput.getStyle()
            .set("padding", "8px")
            .set("border", "1px solid #ccc")
            .set("border-radius", "4px")
            .set("width", "250px");

        field1.add(nameLabel, nameInput);

        // Second field
        Div field2 = new Div();
        field2.getStyle().set("margin-bottom", "15px");

        NativeLabel emailLabel = new NativeLabel("Email:");
        emailLabel.setId("email-label");
        emailLabel.setFor("email-input");
        emailLabel.getStyle()
            .set("display", "block")
            .set("margin-bottom", "5px")
            .set("font-weight", "500");

        Input emailInput = new Input();
        emailInput.setId("email-input");
        emailInput.setType("email");
        emailInput.setPlaceholder("Enter your email");
        emailInput.getStyle()
            .set("padding", "8px")
            .set("border", "1px solid #ccc")
            .set("border-radius", "4px")
            .set("width", "250px");

        field2.add(emailLabel, emailInput);

        Paragraph hint = new Paragraph("Click on the labels to focus the corresponding input fields.");
        hint.getStyle().set("font-size", "0.9em").set("color", "#666").set("font-style", "italic");

        demo.add(field1, field2, hint);
        return demo;
    }

    private Div createStyledLabelsDemo() {
        Div demo = new Div();
        demo.setId("styled-labels");

        NativeLabel boldLabel = new NativeLabel("Bold Label");
        boldLabel.setId("bold-label");
        boldLabel.getStyle()
            .set("font-weight", "bold")
            .set("font-size", "1.1em")
            .set("display", "block")
            .set("margin-bottom", "10px");

        NativeLabel coloredLabel = new NativeLabel("Colored Label");
        coloredLabel.setId("colored-label");
        coloredLabel.getStyle()
            .set("color", "#1976d2")
            .set("display", "block")
            .set("margin-bottom", "10px");

        NativeLabel uppercaseLabel = new NativeLabel("Uppercase Label");
        uppercaseLabel.setId("uppercase-label");
        uppercaseLabel.getStyle()
            .set("text-transform", "uppercase")
            .set("letter-spacing", "2px")
            .set("font-size", "0.85em")
            .set("color", "#666")
            .set("display", "block")
            .set("margin-bottom", "10px");

        demo.add(boldLabel, coloredLabel, uppercaseLabel);
        return demo;
    }

    private Div createRequiredLabelsDemo() {
        Div demo = new Div();
        demo.setId("required-labels");

        Div field = new Div();
        field.getStyle().set("margin-bottom", "15px");

        NativeLabel requiredLabel = new NativeLabel();
        requiredLabel.setId("required-label");
        requiredLabel.getElement().setProperty("innerHTML", "Username <span style='color: red;'>*</span>");
        requiredLabel.setFor("username-input");
        requiredLabel.getStyle()
            .set("display", "block")
            .set("margin-bottom", "5px")
            .set("font-weight", "500");

        Input usernameInput = new Input();
        usernameInput.setId("username-input");
        usernameInput.setPlaceholder("Required field");
        usernameInput.getElement().setAttribute("required", true);
        usernameInput.getStyle()
            .set("padding", "8px")
            .set("border", "1px solid #ccc")
            .set("border-radius", "4px")
            .set("width", "250px");

        field.add(requiredLabel, usernameInput);

        Paragraph legend = new Paragraph("Fields marked with * are required.");
        legend.getStyle()
            .set("font-size", "0.85em")
            .set("color", "#666");

        demo.add(field, legend);
        return demo;
    }
}
