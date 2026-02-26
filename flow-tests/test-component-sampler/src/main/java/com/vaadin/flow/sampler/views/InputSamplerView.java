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
import com.vaadin.flow.component.html.RangeInput;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.sampler.SamplerMainLayout;

/**
 * Demonstrates the Input and RangeInput components.
 */
@Route(value = "input", layout = SamplerMainLayout.class)
@PageTitle("Input Sampler")
public class InputSamplerView extends Div {

    public InputSamplerView() {
        setId("input-sampler");

        add(new H1("Input Components"));
        add(new Paragraph("Native HTML input elements for various data types."));

        add(createSection("Text Input",
            "Basic text input with various configurations.",
            createTextInputDemo()));

        add(createSection("Input Types",
            "Different input types for specific data.",
            createInputTypesDemo()));

        add(createSection("Range Input",
            "Slider control for numeric ranges.",
            createRangeInputDemo()));

        add(createSection("Input Validation",
            "Inputs with validation attributes.",
            createValidationDemo()));
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

    private Div createTextInputDemo() {
        Div demo = new Div();
        demo.setId("text-input-demo");

        // Basic input
        Div basicField = createInputField("Basic Input", "basic-input", "text", "Enter text...");

        // Input with value
        Div prefilledField = new Div();
        prefilledField.getStyle().set("margin-bottom", "15px");
        NativeLabel prefilledLabel = new NativeLabel("Prefilled Input:");
        prefilledLabel.setFor("prefilled-input");
        prefilledLabel.getStyle().set("display", "block").set("margin-bottom", "5px").set("font-weight", "500");
        Input prefilledInput = new Input();
        prefilledInput.setId("prefilled-input");
        prefilledInput.setValue("Prefilled value");
        styleInput(prefilledInput);
        prefilledField.add(prefilledLabel, prefilledInput);

        // Readonly input
        Div readonlyField = new Div();
        readonlyField.getStyle().set("margin-bottom", "15px");
        NativeLabel readonlyLabel = new NativeLabel("Readonly Input:");
        readonlyLabel.setFor("readonly-input");
        readonlyLabel.getStyle().set("display", "block").set("margin-bottom", "5px").set("font-weight", "500");
        Input readonlyInput = new Input();
        readonlyInput.setId("readonly-input");
        readonlyInput.setValue("Cannot be edited");
        readonlyInput.getElement().setAttribute("readonly", true);
        styleInput(readonlyInput);
        readonlyInput.getStyle().set("background-color", "#f5f5f5");
        readonlyField.add(readonlyLabel, readonlyInput);

        // Disabled input
        Div disabledField = new Div();
        disabledField.getStyle().set("margin-bottom", "15px");
        NativeLabel disabledLabel = new NativeLabel("Disabled Input:");
        disabledLabel.setFor("disabled-input");
        disabledLabel.getStyle().set("display", "block").set("margin-bottom", "5px").set("font-weight", "500");
        Input disabledInput = new Input();
        disabledInput.setId("disabled-input");
        disabledInput.setValue("Disabled field");
        disabledInput.setEnabled(false);
        styleInput(disabledInput);
        disabledField.add(disabledLabel, disabledInput);

        demo.add(basicField, prefilledField, readonlyField, disabledField);
        return demo;
    }

    private Div createInputTypesDemo() {
        Div demo = new Div();
        demo.setId("input-types-demo");
        demo.getStyle()
            .set("display", "grid")
            .set("grid-template-columns", "repeat(auto-fill, minmax(250px, 1fr))")
            .set("gap", "20px");

        demo.add(createInputField("Email", "email-input", "email", "email@example.com"));
        demo.add(createInputField("Password", "password-input", "password", "Enter password"));
        demo.add(createInputField("Number", "number-input", "number", "0"));
        demo.add(createInputField("Date", "date-input", "date", ""));
        demo.add(createInputField("Time", "time-input", "time", ""));
        demo.add(createInputField("Color", "color-input", "color", ""));
        demo.add(createInputField("Search", "search-input", "search", "Search..."));
        demo.add(createInputField("URL", "url-input", "url", "https://"));

        return demo;
    }

    private Div createRangeInputDemo() {
        Div demo = new Div();
        demo.setId("range-input-demo");

        // Basic range
        Div basicRange = new Div();
        basicRange.getStyle().set("margin-bottom", "25px");

        NativeLabel basicLabel = new NativeLabel("Volume (0-100):");
        basicLabel.setFor("basic-range");
        basicLabel.getStyle().set("display", "block").set("margin-bottom", "10px").set("font-weight", "500");

        RangeInput basicRangeInput = new RangeInput();
        basicRangeInput.setId("basic-range");
        basicRangeInput.setMin(0);
        basicRangeInput.setMax(100);
        basicRangeInput.setValue(50.0);
        basicRangeInput.getStyle().set("width", "100%");

        Span basicValue = new Span("50");
        basicValue.setId("basic-range-value");
        basicValue.getStyle().set("font-weight", "500").set("margin-left", "10px");

        basicRangeInput.addValueChangeListener(e ->
            basicValue.setText(String.valueOf(e.getValue().intValue())));

        Div basicRow = new Div();
        basicRow.getStyle().set("display", "flex").set("align-items", "center");
        basicRow.add(basicRangeInput, basicValue);

        basicRange.add(basicLabel, basicRow);

        // Range with step
        Div stepRange = new Div();
        stepRange.getStyle().set("margin-bottom", "25px");

        NativeLabel stepLabel = new NativeLabel("Temperature (0-40, step 5):");
        stepLabel.setFor("step-range");
        stepLabel.getStyle().set("display", "block").set("margin-bottom", "10px").set("font-weight", "500");

        RangeInput stepRangeInput = new RangeInput();
        stepRangeInput.setId("step-range");
        stepRangeInput.setMin(0);
        stepRangeInput.setMax(40);
        stepRangeInput.setStep(5);
        stepRangeInput.setValue(20.0);
        stepRangeInput.getStyle().set("width", "100%");

        Span stepValue = new Span("20°C");
        stepValue.setId("step-range-value");
        stepValue.getStyle().set("font-weight", "500").set("margin-left", "10px");

        stepRangeInput.addValueChangeListener(e ->
            stepValue.setText(e.getValue().intValue() + "°C"));

        Div stepRow = new Div();
        stepRow.getStyle().set("display", "flex").set("align-items", "center");
        stepRow.add(stepRangeInput, stepValue);

        stepRange.add(stepLabel, stepRow);

        // Color preview range
        Div colorRange = new Div();
        colorRange.getStyle().set("margin-bottom", "25px");

        NativeLabel colorLabel = new NativeLabel("RGB Red Component:");
        colorLabel.setFor("color-range");
        colorLabel.getStyle().set("display", "block").set("margin-bottom", "10px").set("font-weight", "500");

        RangeInput colorRangeInput = new RangeInput();
        colorRangeInput.setId("color-range");
        colorRangeInput.setMin(0);
        colorRangeInput.setMax(255);
        colorRangeInput.setValue(128.0);
        colorRangeInput.getStyle().set("width", "200px");

        Div colorPreview = new Div();
        colorPreview.setId("color-preview");
        colorPreview.getStyle()
            .set("width", "50px")
            .set("height", "30px")
            .set("background-color", "rgb(128, 0, 0)")
            .set("border-radius", "4px")
            .set("margin-left", "15px");

        colorRangeInput.addValueChangeListener(e -> {
            int red = e.getValue().intValue();
            colorPreview.getStyle().set("background-color", "rgb(" + red + ", 0, 0)");
        });

        Div colorRow = new Div();
        colorRow.getStyle().set("display", "flex").set("align-items", "center");
        colorRow.add(colorRangeInput, colorPreview);

        colorRange.add(colorLabel, colorRow);

        demo.add(basicRange, stepRange, colorRange);
        return demo;
    }

    private Div createValidationDemo() {
        Div demo = new Div();
        demo.setId("validation-demo");

        // Required field
        Div requiredField = new Div();
        requiredField.getStyle().set("margin-bottom", "15px");
        NativeLabel requiredLabel = new NativeLabel();
        requiredLabel.getElement().setProperty("innerHTML", "Required Field <span style='color: red;'>*</span>");
        requiredLabel.setFor("required-input");
        requiredLabel.getStyle().set("display", "block").set("margin-bottom", "5px").set("font-weight", "500");
        Input requiredInput = new Input();
        requiredInput.setId("required-input");
        requiredInput.setPlaceholder("This field is required");
        requiredInput.getElement().setAttribute("required", true);
        styleInput(requiredInput);
        requiredField.add(requiredLabel, requiredInput);

        // Min/Max length
        Div lengthField = new Div();
        lengthField.getStyle().set("margin-bottom", "15px");
        NativeLabel lengthLabel = new NativeLabel("Username (3-20 characters):");
        lengthLabel.setFor("length-input");
        lengthLabel.getStyle().set("display", "block").set("margin-bottom", "5px").set("font-weight", "500");
        Input lengthInput = new Input();
        lengthInput.setId("length-input");
        lengthInput.setPlaceholder("Enter username");
        lengthInput.getElement().setAttribute("minlength", "3");
        lengthInput.getElement().setAttribute("maxlength", "20");
        styleInput(lengthInput);
        lengthField.add(lengthLabel, lengthInput);

        // Pattern validation
        Div patternField = new Div();
        patternField.getStyle().set("margin-bottom", "15px");
        NativeLabel patternLabel = new NativeLabel("Phone Number (format: XXX-XXX-XXXX):");
        patternLabel.setFor("pattern-input");
        patternLabel.getStyle().set("display", "block").set("margin-bottom", "5px").set("font-weight", "500");
        Input patternInput = new Input();
        patternInput.setId("pattern-input");
        patternInput.setPlaceholder("123-456-7890");
        patternInput.getElement().setAttribute("pattern", "[0-9]{3}-[0-9]{3}-[0-9]{4}");
        styleInput(patternInput);
        patternField.add(patternLabel, patternInput);

        // Number range
        Div rangeField = new Div();
        rangeField.getStyle().set("margin-bottom", "15px");
        NativeLabel rangeLabel = new NativeLabel("Age (18-100):");
        rangeLabel.setFor("range-validation-input");
        rangeLabel.getStyle().set("display", "block").set("margin-bottom", "5px").set("font-weight", "500");
        Input rangeInput = new Input();
        rangeInput.setId("range-validation-input");
        rangeInput.setType("number");
        rangeInput.setPlaceholder("Enter your age");
        rangeInput.getElement().setAttribute("min", "18");
        rangeInput.getElement().setAttribute("max", "100");
        styleInput(rangeInput);
        rangeField.add(rangeLabel, rangeInput);

        demo.add(requiredField, lengthField, patternField, rangeField);
        return demo;
    }

    private Div createInputField(String label, String id, String type, String placeholder) {
        Div field = new Div();
        field.getStyle().set("margin-bottom", "15px");

        NativeLabel inputLabel = new NativeLabel(label + ":");
        inputLabel.setFor(id);
        inputLabel.getStyle()
            .set("display", "block")
            .set("margin-bottom", "5px")
            .set("font-weight", "500");

        Input input = new Input();
        input.setId(id);
        input.setType(type);
        if (!placeholder.isEmpty()) {
            input.setPlaceholder(placeholder);
        }
        styleInput(input);

        field.add(inputLabel, input);
        return field;
    }

    private void styleInput(Input input) {
        input.getStyle()
            .set("width", "100%")
            .set("padding", "10px 12px")
            .set("border", "1px solid #ccc")
            .set("border-radius", "4px")
            .set("box-sizing", "border-box")
            .set("font-size", "1em");
    }
}
