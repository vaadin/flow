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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.flow.demo.DemoView;
import com.vaadin.flow.demo.MainLayout;
import com.vaadin.router.Route;
import com.vaadin.ui.Component;
import com.vaadin.ui.button.Button;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.layout.FlexLayout.Alignment;
import com.vaadin.ui.layout.FlexLayout.JustifyContentMode;
import com.vaadin.ui.layout.HorizontalLayout;

/**
 * View for the {@link HorizontalLayout} component.
 */
@Route(value = "horizontal-layout", layout = MainLayout.class)
@HtmlImport("bower_components/vaadin-valo-theme/vaadin-button.html")
@ComponentDemo(name = "Horizontal Layout", subcategory = "Layouts")
public class HorizontalLayoutView extends DemoView {

    @Override
    protected void initView() {
        createDefaultLayout();
        createLayoutWithSpacing();
        createLayoutWithDefaultAlignment();
        createLayoutWithIndividualAlignments();
        createLayoutWithExpandRatios();
        createLayoutWithCenterComponent();
    }

    private void createDefaultLayout() {
        // begin-source-example
        // source-example-heading: Default layout
        HorizontalLayout layout = new HorizontalLayout();
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
        // source-example-heading: Layout with justify content
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidth("100%");
        layout.getStyle().set("border", "1px solid #9E9E9E");

        Component component1 = createComponent(1, "#78909C");
        Component component2 = createComponent(2, "#546E7A");
        Component component3 = createComponent(3, "#37474F");

        // the default is JustifyContentMode.START
        layout.setJustifyContentMode(JustifyContentMode.BETWEEN);

        layout.add(component1, component2, component3);
        // end-source-example

        component2.getElement().setText("Component 2 with long text");
        component3.getElement().setText("C 3");

        Div buttons = new Div();
        buttons.add(createSpacingButton(layout, "justify-content-start-button",
                JustifyContentMode.START));
        buttons.add(createSpacingButton(layout, "justify-content-end-button",
                JustifyContentMode.END));
        buttons.add(createSpacingButton(layout,
                "justify-content-between-button", JustifyContentMode.BETWEEN));
        buttons.add(createSpacingButton(layout, "justify-content-around-button",
                JustifyContentMode.AROUND));
        buttons.add(createSpacingButton(layout, "justify-content-evenly-button",
                JustifyContentMode.EVENLY));

        layout.setId("layout-with-justify-content");

        addCard("Layout with justify content", layout, buttons);
    }

    private void createLayoutWithDefaultAlignment() {
        // begin-source-example
        // source-example-heading: Layout with general alignment
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidth("100%");
        layout.setHeight("150px");
        layout.getStyle().set("border", "1px solid #9E9E9E");
        layout.setJustifyContentMode(JustifyContentMode.BETWEEN);

        // the default is Alignment.BASELINE
        layout.setDefaultVerticalComponentAlignment(Alignment.CENTER);

        Component component1 = createComponent(1, "#78909C");
        Component component2 = createComponent(2, "#546E7A");
        Component component3 = createComponent(3, "#37474F");

        layout.add(component1, component2, component3);
        // end-source-example

        component2.getElement().getStyle().set("fontSize", "24px");
        component3.getElement().getStyle().set("fontSize", "9px");

        Div buttons = new Div();
        buttons.add(createAlignmentButton(layout, "align-start-button",
                Alignment.START));
        buttons.add(createAlignmentButton(layout, "align-end-button",
                Alignment.END));
        buttons.add(createAlignmentButton(layout, "align-center-button",
                Alignment.CENTER));
        buttons.add(createAlignmentButton(layout, "align-stretch-button",
                Alignment.STRETCH));
        buttons.add(createAlignmentButton(layout, "align-baseline-button",
                Alignment.BASELINE));

        layout.setId("layout-with-alignment");

        addCard("Layout with general alignment", layout, buttons);
    }

    private void createLayoutWithIndividualAlignments() {
        // begin-source-example
        // source-example-heading: Layout with individual alignments
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidth("100%");
        layout.setHeight("150px");
        layout.getStyle().set("border", "1px solid #9E9E9E");
        layout.setJustifyContentMode(JustifyContentMode.BETWEEN);

        Component component1 = createComponent(1, "#78909C");
        layout.setVerticalComponentAlignment(Alignment.START, component1);

        Component component2 = createComponent(2, "#546E7A");
        layout.setVerticalComponentAlignment(Alignment.CENTER, component2);

        Component component3 = createComponent(3, "#37474F");
        layout.setVerticalComponentAlignment(Alignment.END, component3);

        Component component4 = createComponent(4, "#263238");
        layout.setVerticalComponentAlignment(Alignment.STRETCH, component4);

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
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidth("100%");
        layout.getStyle().set("border", "1px solid #9E9E9E");

        Component component1 = createComponent(1, "#78909C");
        layout.expand(component1);

        Component component2 = createComponent(2, "#546E7A");
        layout.setFlexGrow(2, component2);

        Component component3 = createComponent(3, "#37474F");
        layout.setFlexGrow(0.5, component3);

        layout.add(component1, component2, component3);
        // end-source-example

        component1.setId("ratio-1");
        component2.setId("ratio-2");
        component3.setId("ratio-0.5");
        layout.setId("layout-with-expand-ratios");

        addCard("Layout with expand ratios", layout);
    }

    private void createLayoutWithCenterComponent() {
        // begin-source-example
        // source-example-heading: Layout with component in the center
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidth("200px");
        layout.getStyle().set("border", "1px solid #9E9E9E");

        Component component = createComponent(1, "#78909C");
        layout.add(component);
        layout.setJustifyContentMode(JustifyContentMode.AROUND);

        // end-source-example

        component.setId("center");
        layout.setId("layout-with-center");

        addCard("Layout with component in the center", layout);
    }

    private Component createComponent(int index, String color) {
        Div component = new Div();
        component.setText("Component " + index);
        component.getStyle().set("backgroundColor", color).set("color", "white")
                .set("padding", "5px 10px");
        return component;
    }

    private Component createAlignmentButton(HorizontalLayout layout, String id,
            Alignment alignment) {
        Button button = new Button(alignment.name());
        button.setId(id);
        button.addClickListener(event -> layout
                .setDefaultVerticalComponentAlignment(alignment));
        return button;
    }

    private Component createSpacingButton(HorizontalLayout layout, String id,
            JustifyContentMode spacing) {
        Button button = new Button(spacing.name());
        button.setId(id);
        button.addClickListener(event -> layout.setJustifyContentMode(spacing));
        return button;
    }

}
