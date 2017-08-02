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

import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.flow.html.H3;
import com.vaadin.flow.html.Label;
import com.vaadin.ui.VaadinSplitLayout;

/**
 * View for {@link VaadinSplitLayout} demo.
 */
@ComponentDemo(name = "Vaadin Split Layout", href = "vaadin-split-layout")
public class VaadinSplitLayoutView extends DemoView {

    @Override
    public void initView() {
        addHorizontalLayout();
        addVerticalLayout();
        addLayoutCombination();
        addResizeNotificationLayout();
        addInitialSplitterPositionLayout();
        addMinMaxWidthLayout();
    }

    private void addHorizontalLayout() {
        // begin-source-example
        // source-example-heading: Horizontal Split Layout (Default)
        VaadinSplitLayout layout = new VaadinSplitLayout();
        layout.addToPrimary(new Label("First content component"));
        layout.addToSecondary(new Label("Second content component"));
        // end-source-example
        add(new H3("Horizontal Split Layout (Default)"), layout);
    }

    private void addVerticalLayout() {
        // begin-source-example
        // source-example-heading: Vertical Split Layout
        VaadinSplitLayout layout = new VaadinSplitLayout().setVertical(true);
        layout.addToPrimary(new Label("Top content component"));
        layout.addToSecondary(new Label("Bottom content component"));
        // end-source-example
        addCard(new H3("Vertical Split Layout"), layout);
    }

    private void addLayoutCombination() {
        // @formatter:off
        // begin-source-example
        // source-example-heading: Layout Combination
        VaadinSplitLayout layout = new VaadinSplitLayout()
            .addToPrimary(new Label("First content component"))
            .addToSecondary(
                    new VaadinSplitLayout().setVertical(true)
                            .addToPrimary(new Label("Second content component"))
                            .addToSecondary(new Label("Third content component")));
        // end-source-example
        // @formatter:on
        layout.getPrimaryComponent().setId("first-component");
        layout.getSecondaryComponent().setId("nested-layout");
        ((VaadinSplitLayout) layout.getSecondaryComponent())
                .getPrimaryComponent().setId("second-component");
        ((VaadinSplitLayout) layout.getSecondaryComponent())
                .getSecondaryComponent().setId("third-component");
        addCard(new H3("Layout Combination"), layout);
    }

    private void addResizeNotificationLayout() {
        // begin-source-example
        // source-example-heading: Resize Notification for the Nested Elements
        VaadinSplitLayout layout = new VaadinSplitLayout()
                .addToPrimary(new Label("First content component"))
                .addToSecondary(new Label("Second content component"));
        Label message = new Label();
        AtomicInteger resizeCounter = new AtomicInteger();
        layout.addIronResizeListener(event -> message.setText(
                "Resized " + resizeCounter.getAndIncrement() + " times."));
        // end-source-example
        message.setId("resize-message");
        addCard(new H3("Resize Events"), layout, message);
    }

    private void addInitialSplitterPositionLayout() {
        // begin-source-example
        // source-example-heading: Split Layout with Initial Splitter Position
        VaadinSplitLayout layout = new VaadinSplitLayout(
                new Label("First content component"),
                new Label("Second content component"));
        layout.setSplitterPosition(80);
        // end-source-example
        layout.getPrimaryComponent().setId("initial-sp-first-component");
        layout.getSecondaryComponent().setId("initial-sp-second-component");
        addCard(new H3("Split Layout with Initial Splitter Position"), layout);
    }
    
    private void addMinMaxWidthLayout() {
        // begin-source-example
        // source-example-heading: Split Layout with Minimum and Maximum Widths
        VaadinSplitLayout layout = new VaadinSplitLayout()
                .addToPrimary(new Label("First content component"))
                .addToSecondary(new Label("Second content component"))
                .setPrimaryStyle("minWidth", "100px")
                .setPrimaryStyle("maxWidth", "150px");
        // end-source-example
        layout.getPrimaryComponent().setId("min-max-first-component");
        addCard(new H3("Split Layout with Minimum and Maximum Widths"), layout);
    }
}
