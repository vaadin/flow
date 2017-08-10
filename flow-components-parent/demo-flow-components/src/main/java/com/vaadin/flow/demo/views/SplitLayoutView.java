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
import com.vaadin.flow.html.Label;
import com.vaadin.ui.SplitLayout;

/**
 * View for {@link SplitLayout} demo.
 */
@ComponentDemo(name = "Vaadin Split Layout", href = "vaadin-split-layout")
public class SplitLayoutView extends DemoView {

    private static final String FIRST_CONTENT_TEXT = "First content component";
    private static final String SECOND_CONTENT_TEXT = "Second content component";

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
        Label firstLabel = new Label(FIRST_CONTENT_TEXT);
        Label secondLabel = new Label(SECOND_CONTENT_TEXT);
        // begin-source-example
        // source-example-heading: Horizontal Split Layout (Default)
        SplitLayout layout = new SplitLayout();
        layout.addToPrimary(firstLabel);
        layout.addToSecondary(secondLabel);
        // end-source-example

        addCard("Horizontal Split Layout (Default)", layout);
    }

    private void addVerticalLayout() {
        // begin-source-example
        // source-example-heading: Vertical Split Layout
        SplitLayout layout = new SplitLayout().setVertical(true);
        layout.addToPrimary(new Label("Top content component"));
        layout.addToSecondary(new Label("Bottom content component"));
        // end-source-example

        addCard("Vertical Split Layout", layout);
    }

    private void addLayoutCombination() {
        Label firstLabel = new Label(FIRST_CONTENT_TEXT);
        Label secondLabel = new Label(SECOND_CONTENT_TEXT);
        Label thirdLabel = new Label("Third content component");
        // @formatter:off
        // begin-source-example
        // source-example-heading: Layout Combination
        SplitLayout layout = new SplitLayout()
            .addToPrimary(firstLabel)
            .addToSecondary(
                    new SplitLayout().setVertical(true)
                            .addToPrimary(secondLabel)
                            .addToSecondary(thirdLabel));
        // end-source-example
        // @formatter:on

        layout.getPrimaryComponent().setId("first-component");
        layout.getSecondaryComponent().setId("nested-layout");
        ((SplitLayout) layout.getSecondaryComponent()).getPrimaryComponent()
                .setId("second-component");
        ((SplitLayout) layout.getSecondaryComponent()).getSecondaryComponent()
                .setId("third-component");
        addCard("Layout Combination", layout);
    }

    private void addResizeNotificationLayout() {
        Label firstLabel = new Label(FIRST_CONTENT_TEXT);
        Label secondLabel = new Label(SECOND_CONTENT_TEXT);
        // begin-source-example
        // source-example-heading: Resize Notification for the Nested Elements
        SplitLayout layout = new SplitLayout().addToPrimary(firstLabel)
                .addToSecondary(secondLabel);
        Label message = new Label();
        AtomicInteger resizeCounter = new AtomicInteger();
        layout.addIronResizeListener(event -> message.setText(
                "Resized " + resizeCounter.getAndIncrement() + " times."));
        // end-source-example

        message.setId("resize-message");
        addCard("Resize Events", layout, message);
    }

    private void addInitialSplitterPositionLayout() {
        Label firstLabel = new Label(FIRST_CONTENT_TEXT);
        Label secondLabel = new Label(SECOND_CONTENT_TEXT);
        // @formatter:off
        // begin-source-example
        // source-example-heading: Split Layout with Initial Splitter Position
        SplitLayout layout = new SplitLayout(firstLabel, secondLabel);
        layout.setSplitterPosition(80);
        // end-source-example
        // @formatter:on\

        layout.getPrimaryComponent().setId("initial-sp-first-component");
        layout.getSecondaryComponent().setId("initial-sp-second-component");
        addCard("Split Layout with Initial Splitter Position", layout);
    }

    private void addMinMaxWidthLayout() {
        Label firstLabel = new Label(FIRST_CONTENT_TEXT);
        Label secondLabel = new Label(SECOND_CONTENT_TEXT);
        // begin-source-example
        // source-example-heading: Split Layout with Minimum and Maximum Widths
        SplitLayout layout = new SplitLayout().addToPrimary(firstLabel)
                .addToSecondary(secondLabel)
                .setPrimaryStyle("minWidth", "100px")
                .setPrimaryStyle("maxWidth", "150px");
        // end-source-example

        layout.getPrimaryComponent().setId("min-max-first-component");
        addCard("Split Layout with Minimum and Maximum Widths", layout);
    }
}
