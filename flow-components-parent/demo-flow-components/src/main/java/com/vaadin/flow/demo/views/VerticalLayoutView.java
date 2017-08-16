/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.demo.views;

import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.flow.html.Div;
import com.vaadin.generated.paper.button.GeneratedPaperButton;
import com.vaadin.ui.Component;
import com.vaadin.ui.FlexLayout.Alignment;
import com.vaadin.ui.FlexLayout.SpacingMode;
import com.vaadin.ui.VerticalLayout;

/**
 * View for the {@link VerticalLayout} component.
 */
@ComponentDemo(name = "Vertical Layout", href = "vertical-layout")
public class VerticalLayoutView extends DemoView {

    @Override
    void initView() {
        createDefaultLayout();
        createLayoutWithSpacing();
        createLayoutWithDefaultAlignment();
        createLayoutWithIndividualAlignments();
        createLayoutWithExpandRatios();
    }

    private void createDefaultLayout() {
        // begin-source-example
        // source-example-heading: Default layout
        VerticalLayout layout = new VerticalLayout();
        layout.setHeight("150px");
        layout.getStyle().set("border", "1px solid #9E9E9E");

        Component component1 = createComponent(1, "#78909C");
        Component component2 = createComponent(2, "#546E7A");
        Component component3 = createComponent(3, "#37474F");

        layout.add(component1, component2, component3);
        // end-source-example

        layout.setId("default-layout");

        addCard("Default layout", layout);
    }

    private void createLayoutWithSpacing() {
        // begin-source-example
        // source-example-heading: Layout with spacing
        VerticalLayout layout = new VerticalLayout();
        layout.setHeight("150px");
        layout.getStyle().set("border", "1px solid #9E9E9E");

        // the default is SpacingMode.BETWEEN
        layout.setSpacing(true);

        Component component1 = createComponent(1, "#78909C");
        Component component2 = createComponent(2, "#546E7A");
        Component component3 = createComponent(3, "#37474F");

        layout.add(component1, component2, component3);
        // end-source-example

        component2.getElement().setProperty("innerHTML",
                "Component 2<br>With long text");
        component3.getElement().getStyle().set("fontSize", "9px");

        Div buttons = new Div();
        buttons.add(createSpacingButton(layout, "space-between-button",
                SpacingMode.BETWEEN));
        buttons.add(createSpacingButton(layout, "space-around-button",
                SpacingMode.AROUND));
        buttons.add(createSpacingButton(layout, "space-evenly-button",
                SpacingMode.EVENLY));

        layout.setId("layout-with-spacing");

        addCard("Layout with spacing", layout, buttons);
    }

    private void createLayoutWithDefaultAlignment() {
        // begin-source-example
        // source-example-heading: Layout with general alignment
        VerticalLayout layout = new VerticalLayout();
        layout.setHeight("150px");
        layout.getStyle().set("border", "1px solid #9E9E9E");
        layout.setSpacing(true);

        // the default is Alignment.STRETCH
        layout.setDefaultComponentAlignment(Alignment.START);

        Component component1 = createComponent(1, "#78909C");
        Component component2 = createComponent(2, "#546E7A");
        Component component3 = createComponent(3, "#37474F");

        layout.add(component1, component2, component3);
        // end-source-example

        component2.getElement().setText("Component 2 with long text");
        component3.getElement().setText("C 3");

        Div buttons = new Div();
        buttons.add(createAlignmentButton(layout, "align-start-button",
                Alignment.START));
        buttons.add(createAlignmentButton(layout, "align-end-button",
                Alignment.END));
        buttons.add(createAlignmentButton(layout, "align-center-button",
                Alignment.CENTER));
        buttons.add(createAlignmentButton(layout, "align-stretch-button",
                Alignment.STRETCH));

        layout.setId("layout-with-alignment");

        addCard("Layout with general alignment", layout, buttons);
    }

    private void createLayoutWithIndividualAlignments() {
        // begin-source-example
        // source-example-heading: Layout with individual alignments
        VerticalLayout layout = new VerticalLayout();
        layout.setHeight("150px");
        layout.getStyle().set("border", "1px solid #9E9E9E");
        layout.setSpacing(true);

        Component component1 = createComponent(1, "#78909C");
        layout.setComponentAlignment(Alignment.START, component1);

        Component component2 = createComponent(2, "#546E7A");
        layout.setComponentAlignment(Alignment.CENTER, component2);

        Component component3 = createComponent(3, "#37474F");
        layout.setComponentAlignment(Alignment.END, component3);

        Component component4 = createComponent(4, "#263238");
        layout.setComponentAlignment(Alignment.STRETCH, component4);

        layout.add(component1, component2, component3, component4);
        // end-source-example

        component1.setId("start-aligned");
        component2.setId("center-aligned");
        component3.setId("end-aligned");
        component4.setId("stretch-aligned");
        layout.setId("layout-with-individual-alignments");

        addCard("Layout with individual alignments", layout);
    }

    private void createLayoutWithExpandRatios() {
        // begin-source-example
        // source-example-heading: Layout with expand ratios
        VerticalLayout layout = new VerticalLayout();
        layout.setHeight("200px");
        layout.getStyle().set("border", "1px solid #9E9E9E");

        Component component1 = createComponent(1, "#78909C");
        layout.setExpandRatio(1, component1);

        Component component2 = createComponent(2, "#546E7A");
        layout.setExpandRatio(2, component2);

        Component component3 = createComponent(3, "#37474F");
        layout.setExpandRatio(0.5, component3);

        layout.add(component1, component2, component3);
        // end-source-example

        component1.setId("ratio-1");
        component2.setId("ratio-2");
        component3.setId("ratio-0.5");
        layout.setId("layout-with-expand-ratios");

        addCard("Layout with expand ratios", layout);
    }

    private Component createComponent(int index, String color) {
        Div component = new Div();
        component.setText("Component " + index);
        component.getStyle().set("backgroundColor", color).set("color", "white")
                .set("padding", "5px 10px");
        return component;
    }

    private Component createAlignmentButton(VerticalLayout layout, String id,
            Alignment alignment) {
        GeneratedPaperButton button = new GeneratedPaperButton(
                alignment.name());
        button.setId(id);
        button.setRaised(true);
        button.addClickListener(
                event -> layout.setDefaultComponentAlignment(alignment));
        return button;
    }

    private Component createSpacingButton(VerticalLayout layout, String id,
            SpacingMode spacing) {
        GeneratedPaperButton button = new GeneratedPaperButton(spacing.name());
        button.setId(id);
        button.setRaised(true);
        button.addClickListener(event -> layout.setSpacingMode(spacing));
        return button;
    }

}
