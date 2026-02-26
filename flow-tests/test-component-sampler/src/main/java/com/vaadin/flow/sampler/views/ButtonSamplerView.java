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
 * Demonstrates the NativeButton component and its features.
 */
@Route(value = "button", layout = SamplerMainLayout.class)
@PageTitle("Button Sampler")
public class ButtonSamplerView extends Div {

    public ButtonSamplerView() {
        setId("button-sampler");

        add(new H1("NativeButton Component"));
        add(new Paragraph("The NativeButton component represents a clickable button."));

        add(createSection("Basic Buttons",
            "Simple buttons with click handlers.",
            createBasicButtonsDemo()));

        add(createSection("Button Variants",
            "Buttons with different visual styles.",
            createButtonVariantsDemo()));

        add(createSection("Button States",
            "Enabled and disabled button states.",
            createButtonStatesDemo()));

        add(createSection("Button with Counter",
            "Button with click counter demonstration.",
            createCounterDemo()));

        add(createSection("Button Sizes",
            "Buttons of different sizes.",
            createButtonSizesDemo()));
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

    private Div createBasicButtonsDemo() {
        Div demo = new Div();
        demo.setId("basic-buttons");

        Div output = new Div("Click a button...");
        output.setId("button-output");
        output.getStyle()
            .set("padding", "15px")
            .set("background-color", "#f5f5f5")
            .set("border-radius", "4px")
            .set("margin-bottom", "15px");

        Div buttons = new Div();
        buttons.getStyle().set("display", "flex").set("gap", "10px").set("flex-wrap", "wrap");

        NativeButton btn1 = new NativeButton("Button 1", e -> output.setText("Button 1 clicked!"));
        btn1.setId("basic-btn-1");

        NativeButton btn2 = new NativeButton("Button 2", e -> output.setText("Button 2 clicked!"));
        btn2.setId("basic-btn-2");

        NativeButton btn3 = new NativeButton("Button 3", e -> output.setText("Button 3 clicked!"));
        btn3.setId("basic-btn-3");

        buttons.add(btn1, btn2, btn3);
        demo.add(output, buttons);
        return demo;
    }

    private Div createButtonVariantsDemo() {
        Div demo = new Div();
        demo.setId("button-variants");
        demo.getStyle().set("display", "flex").set("gap", "10px").set("flex-wrap", "wrap");

        // Primary button
        NativeButton primaryBtn = new NativeButton("Primary");
        primaryBtn.setId("primary-btn");
        primaryBtn.getStyle()
            .set("background-color", "#1976d2")
            .set("color", "white")
            .set("border", "none")
            .set("padding", "10px 20px")
            .set("border-radius", "4px")
            .set("cursor", "pointer");

        // Secondary button
        NativeButton secondaryBtn = new NativeButton("Secondary");
        secondaryBtn.setId("secondary-btn");
        secondaryBtn.getStyle()
            .set("background-color", "#757575")
            .set("color", "white")
            .set("border", "none")
            .set("padding", "10px 20px")
            .set("border-radius", "4px")
            .set("cursor", "pointer");

        // Success button
        NativeButton successBtn = new NativeButton("Success");
        successBtn.setId("success-btn");
        successBtn.getStyle()
            .set("background-color", "#388e3c")
            .set("color", "white")
            .set("border", "none")
            .set("padding", "10px 20px")
            .set("border-radius", "4px")
            .set("cursor", "pointer");

        // Warning button
        NativeButton warningBtn = new NativeButton("Warning");
        warningBtn.setId("warning-btn");
        warningBtn.getStyle()
            .set("background-color", "#f57c00")
            .set("color", "white")
            .set("border", "none")
            .set("padding", "10px 20px")
            .set("border-radius", "4px")
            .set("cursor", "pointer");

        // Danger button
        NativeButton dangerBtn = new NativeButton("Danger");
        dangerBtn.setId("danger-btn");
        dangerBtn.getStyle()
            .set("background-color", "#d32f2f")
            .set("color", "white")
            .set("border", "none")
            .set("padding", "10px 20px")
            .set("border-radius", "4px")
            .set("cursor", "pointer");

        // Outline button
        NativeButton outlineBtn = new NativeButton("Outline");
        outlineBtn.setId("outline-btn");
        outlineBtn.getStyle()
            .set("background-color", "transparent")
            .set("color", "#1976d2")
            .set("border", "2px solid #1976d2")
            .set("padding", "8px 18px")
            .set("border-radius", "4px")
            .set("cursor", "pointer");

        demo.add(primaryBtn, secondaryBtn, successBtn, warningBtn, dangerBtn, outlineBtn);
        return demo;
    }

    private Div createButtonStatesDemo() {
        Div demo = new Div();
        demo.setId("button-states");

        NativeButton enabledBtn = new NativeButton("Enabled Button");
        enabledBtn.setId("enabled-btn");
        enabledBtn.getStyle()
            .set("padding", "10px 20px")
            .set("border-radius", "4px")
            .set("margin-right", "10px");

        NativeButton disabledBtn = new NativeButton("Disabled Button");
        disabledBtn.setId("disabled-btn");
        disabledBtn.setEnabled(false);
        disabledBtn.getStyle()
            .set("padding", "10px 20px")
            .set("border-radius", "4px")
            .set("margin-right", "10px");

        NativeButton toggleableBtn = new NativeButton("Toggle My State");
        toggleableBtn.setId("toggleable-btn");
        toggleableBtn.getStyle()
            .set("padding", "10px 20px")
            .set("border-radius", "4px");

        Span statusSpan = new Span("Status: Enabled");
        statusSpan.setId("toggle-status");
        statusSpan.getStyle()
            .set("margin-left", "15px")
            .set("font-weight", "500");

        NativeButton toggleController = new NativeButton("Toggle Above Button", e -> {
            boolean newState = !toggleableBtn.isEnabled();
            toggleableBtn.setEnabled(newState);
            statusSpan.setText("Status: " + (newState ? "Enabled" : "Disabled"));
        });
        toggleController.setId("toggle-controller");
        toggleController.getStyle()
            .set("padding", "10px 20px")
            .set("border-radius", "4px")
            .set("margin-top", "15px")
            .set("display", "block");

        demo.add(enabledBtn, disabledBtn, toggleableBtn, statusSpan, toggleController);
        return demo;
    }

    private Div createCounterDemo() {
        Div demo = new Div();
        demo.setId("counter-demo");

        Div counterDisplay = new Div("Count: 0");
        counterDisplay.setId("counter-display");
        counterDisplay.getStyle()
            .set("font-size", "2em")
            .set("font-weight", "bold")
            .set("text-align", "center")
            .set("padding", "20px")
            .set("margin-bottom", "15px");

        int[] count = {0};

        Div buttons = new Div();
        buttons.getStyle()
            .set("display", "flex")
            .set("gap", "10px")
            .set("justify-content", "center");

        NativeButton decrementBtn = new NativeButton("-", e -> {
            count[0]--;
            counterDisplay.setText("Count: " + count[0]);
        });
        decrementBtn.setId("decrement-btn");
        decrementBtn.getStyle()
            .set("width", "50px")
            .set("height", "50px")
            .set("font-size", "1.5em")
            .set("border-radius", "50%")
            .set("border", "none")
            .set("background-color", "#f44336")
            .set("color", "white")
            .set("cursor", "pointer");

        NativeButton incrementBtn = new NativeButton("+", e -> {
            count[0]++;
            counterDisplay.setText("Count: " + count[0]);
        });
        incrementBtn.setId("increment-btn");
        incrementBtn.getStyle()
            .set("width", "50px")
            .set("height", "50px")
            .set("font-size", "1.5em")
            .set("border-radius", "50%")
            .set("border", "none")
            .set("background-color", "#4caf50")
            .set("color", "white")
            .set("cursor", "pointer");

        NativeButton resetBtn = new NativeButton("Reset", e -> {
            count[0] = 0;
            counterDisplay.setText("Count: " + count[0]);
        });
        resetBtn.setId("reset-btn");
        resetBtn.getStyle()
            .set("padding", "10px 20px")
            .set("border-radius", "4px")
            .set("border", "1px solid #ccc")
            .set("background-color", "#f5f5f5")
            .set("cursor", "pointer");

        buttons.add(decrementBtn, resetBtn, incrementBtn);
        demo.add(counterDisplay, buttons);
        return demo;
    }

    private Div createButtonSizesDemo() {
        Div demo = new Div();
        demo.setId("button-sizes");
        demo.getStyle()
            .set("display", "flex")
            .set("gap", "15px")
            .set("align-items", "center")
            .set("flex-wrap", "wrap");

        NativeButton smallBtn = new NativeButton("Small");
        smallBtn.setId("small-btn");
        smallBtn.getStyle()
            .set("padding", "5px 10px")
            .set("font-size", "0.8em")
            .set("border-radius", "4px");

        NativeButton mediumBtn = new NativeButton("Medium");
        mediumBtn.setId("medium-btn");
        mediumBtn.getStyle()
            .set("padding", "10px 20px")
            .set("font-size", "1em")
            .set("border-radius", "4px");

        NativeButton largeBtn = new NativeButton("Large");
        largeBtn.setId("large-btn");
        largeBtn.getStyle()
            .set("padding", "15px 30px")
            .set("font-size", "1.2em")
            .set("border-radius", "4px");

        NativeButton extraLargeBtn = new NativeButton("Extra Large");
        extraLargeBtn.setId("extra-large-btn");
        extraLargeBtn.getStyle()
            .set("padding", "20px 40px")
            .set("font-size", "1.4em")
            .set("border-radius", "8px");

        demo.add(smallBtn, mediumBtn, largeBtn, extraLargeBtn);
        return demo;
    }
}
