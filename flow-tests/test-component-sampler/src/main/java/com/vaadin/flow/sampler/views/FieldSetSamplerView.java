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
import com.vaadin.flow.component.html.FieldSet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.sampler.SamplerMainLayout;

/**
 * Demonstrates the FieldSet component and its features.
 */
@Route(value = "fieldset", layout = SamplerMainLayout.class)
@PageTitle("FieldSet Sampler")
public class FieldSetSamplerView extends Div {

    public FieldSetSamplerView() {
        setId("fieldset-sampler");

        add(new H1("FieldSet Component"));
        add(new Paragraph("The FieldSet component groups related form elements together."));

        add(createSection("Basic FieldSet",
            "A simple fieldset with a legend and form fields.",
            createBasicFieldSetDemo()));

        add(createSection("Multiple FieldSets",
            "Multiple fieldsets to organize a complex form.",
            createMultipleFieldSetsDemo()));

        add(createSection("Styled FieldSets",
            "FieldSets with custom styling.",
            createStyledFieldSetsDemo()));

        add(createSection("Disabled FieldSet",
            "FieldSet can be disabled to disable all contained elements.",
            createDisabledFieldSetDemo()));
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

    private Div createBasicFieldSetDemo() {
        Div demo = new Div();
        demo.setId("basic-fieldset");

        FieldSet fieldSet = new FieldSet("Personal Information");
        fieldSet.setId("basic-fieldset-demo");
        fieldSet.getStyle()
            .set("padding", "20px")
            .set("border-radius", "8px");

        Div nameField = createFormField("Full Name:", "name-input", "Enter your name");
        Div emailField = createFormField("Email:", "email-input", "Enter your email");
        Div phoneField = createFormField("Phone:", "phone-input", "Enter your phone");

        fieldSet.add(nameField, emailField, phoneField);

        demo.add(fieldSet);
        return demo;
    }

    private Div createMultipleFieldSetsDemo() {
        Div demo = new Div();
        demo.setId("multiple-fieldsets");

        // Personal info fieldset
        FieldSet personalFieldSet = new FieldSet("Personal Details");
        personalFieldSet.setId("personal-fieldset");
        personalFieldSet.getStyle()
            .set("padding", "20px")
            .set("margin-bottom", "20px")
            .set("border-radius", "8px");

        personalFieldSet.add(
            createFormField("First Name:", "first-name", "John"),
            createFormField("Last Name:", "last-name", "Doe"),
            createFormField("Date of Birth:", "dob", "1990-01-01")
        );

        // Address fieldset
        FieldSet addressFieldSet = new FieldSet("Address");
        addressFieldSet.setId("address-fieldset");
        addressFieldSet.getStyle()
            .set("padding", "20px")
            .set("margin-bottom", "20px")
            .set("border-radius", "8px");

        addressFieldSet.add(
            createFormField("Street:", "street", "123 Main St"),
            createFormField("City:", "city", "New York"),
            createFormField("ZIP Code:", "zip", "10001")
        );

        // Preferences fieldset
        FieldSet prefsFieldSet = new FieldSet("Preferences");
        prefsFieldSet.setId("prefs-fieldset");
        prefsFieldSet.getStyle()
            .set("padding", "20px")
            .set("border-radius", "8px");

        prefsFieldSet.add(
            createFormField("Language:", "language", "English"),
            createFormField("Timezone:", "timezone", "UTC-5")
        );

        demo.add(personalFieldSet, addressFieldSet, prefsFieldSet);
        return demo;
    }

    private Div createStyledFieldSetsDemo() {
        Div demo = new Div();
        demo.setId("styled-fieldsets");
        demo.getStyle()
            .set("display", "flex")
            .set("flex-wrap", "wrap")
            .set("gap", "20px");

        // Primary styled
        FieldSet primaryFieldSet = new FieldSet("Primary Style");
        primaryFieldSet.setId("primary-fieldset");
        primaryFieldSet.getStyle()
            .set("padding", "20px")
            .set("border", "2px solid #1976d2")
            .set("border-radius", "8px")
            .set("background-color", "#e3f2fd")
            .set("flex", "1")
            .set("min-width", "250px");

        primaryFieldSet.add(createFormField("Username:", "primary-user", "username"));

        // Success styled
        FieldSet successFieldSet = new FieldSet("Success Style");
        successFieldSet.setId("success-fieldset");
        successFieldSet.getStyle()
            .set("padding", "20px")
            .set("border", "2px solid #4caf50")
            .set("border-radius", "8px")
            .set("background-color", "#e8f5e9")
            .set("flex", "1")
            .set("min-width", "250px");

        successFieldSet.add(createFormField("Verified:", "success-verified", "Yes"));

        // Warning styled
        FieldSet warningFieldSet = new FieldSet("Warning Style");
        warningFieldSet.setId("warning-fieldset");
        warningFieldSet.getStyle()
            .set("padding", "20px")
            .set("border", "2px solid #ff9800")
            .set("border-radius", "8px")
            .set("background-color", "#fff3e0")
            .set("flex", "1")
            .set("min-width", "250px");

        warningFieldSet.add(createFormField("Status:", "warning-status", "Pending"));

        demo.add(primaryFieldSet, successFieldSet, warningFieldSet);
        return demo;
    }

    private Div createDisabledFieldSetDemo() {
        Div demo = new Div();
        demo.setId("disabled-fieldset");

        FieldSet fieldSet = new FieldSet("Account Settings");
        fieldSet.setId("disabled-fieldset-demo");
        fieldSet.getStyle()
            .set("padding", "20px")
            .set("border-radius", "8px")
            .set("margin-bottom", "15px");

        fieldSet.add(
            createFormField("Username:", "disabled-username", "johndoe"),
            createFormField("Email:", "disabled-email", "john@example.com")
        );

        NativeButton toggleButton = new NativeButton("Disable FieldSet", e -> {
            boolean isDisabled = fieldSet.getElement().getProperty("disabled", false);
            if (isDisabled) {
                fieldSet.getElement().removeProperty("disabled");
                e.getSource().setText("Disable FieldSet");
                fieldSet.getStyle().remove("opacity");
            } else {
                fieldSet.getElement().setProperty("disabled", true);
                e.getSource().setText("Enable FieldSet");
                fieldSet.getStyle().set("opacity", "0.6");
            }
        });
        toggleButton.setId("toggle-disabled-btn");

        Paragraph hint = new Paragraph("When a fieldset is disabled, all form elements inside it become disabled.");
        hint.getStyle().set("font-size", "0.9em").set("color", "#666").set("font-style", "italic");

        demo.add(fieldSet, toggleButton, hint);
        return demo;
    }

    private Div createFormField(String labelText, String inputId, String placeholder) {
        Div field = new Div();
        field.getStyle().set("margin-bottom", "15px");

        NativeLabel label = new NativeLabel(labelText);
        label.setFor(inputId);
        label.getStyle()
            .set("display", "block")
            .set("margin-bottom", "5px")
            .set("font-weight", "500");

        Input input = new Input();
        input.setId(inputId);
        input.setPlaceholder(placeholder);
        input.getStyle()
            .set("width", "100%")
            .set("padding", "8px 12px")
            .set("border", "1px solid #ccc")
            .set("border-radius", "4px")
            .set("box-sizing", "border-box");

        field.add(label, input);
        return field;
    }
}
