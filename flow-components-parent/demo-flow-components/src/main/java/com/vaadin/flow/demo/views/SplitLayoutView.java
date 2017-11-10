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
import com.vaadin.flow.demo.DemoView;
import com.vaadin.flow.demo.MainLayout;
import com.vaadin.router.Route;
import com.vaadin.ui.common.HasStyle;
import com.vaadin.ui.html.Label;
import com.vaadin.ui.splitlayout.SplitLayout;

/**
 * View for {@link SplitLayout} demo.
 */
@Route(value = "vaadin-split-layout", layout = MainLayout.class)
@ComponentDemo(name = "Split Layout", subcategory = "Layouts")
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

        setMinHeightForLayout(layout);
        addCard("Horizontal Split Layout (Default)", layout);
    }

    private void addVerticalLayout() {
        // begin-source-example
        // source-example-heading: Vertical Split Layout
        SplitLayout layout = new SplitLayout();
        layout.setVertical(true);
        layout.addToPrimary(new Label("Top content component"));
        layout.addToSecondary(new Label("Bottom content component"));
        // end-source-example

        setMinHeightForLayout(layout);
        addCard("Vertical Split Layout", layout);
    }

    private void addLayoutCombination() {
        Label firstLabel = new Label(FIRST_CONTENT_TEXT);
        Label secondLabel = new Label(SECOND_CONTENT_TEXT);
        Label thirdLabel = new Label("Third content component");
        // begin-source-example
        // source-example-heading: Layout Combination
        SplitLayout secondLayout = new SplitLayout();
        secondLayout.setVertical(true);
        secondLayout.addToPrimary(secondLabel);
        secondLayout.addToSecondary(thirdLabel);
        SplitLayout layout = new SplitLayout();
        layout.addToPrimary(firstLabel);
        layout.addToSecondary(secondLayout);
        // end-source-example

        layout.getPrimaryComponent().setId("first-component");
        layout.getSecondaryComponent().setId("nested-layout");
        secondLayout.getPrimaryComponent().setId("second-component");
        secondLayout.getSecondaryComponent().setId("third-component");
        setMinHeightForLayout(layout);
        addCard("Layout Combination", layout);
    }

    private void addResizeNotificationLayout() {
        Label firstLabel = new Label(FIRST_CONTENT_TEXT);
        Label secondLabel = new Label(SECOND_CONTENT_TEXT);
        // begin-source-example
        // source-example-heading: Resize Events
        SplitLayout layout = new SplitLayout();
        layout.addToPrimary(firstLabel);
        layout.addToSecondary(secondLabel);
        Label message = new Label();
        AtomicInteger resizeCounter = new AtomicInteger();
        layout.addIronResizeListener(event -> message.setText(
                "Resized " + resizeCounter.getAndIncrement() + " times."));
        // end-source-example

        message.setId("resize-message");
        setMinHeightForLayout(layout);
        addCard("Resize Events", layout, message);
    }

    private void addInitialSplitterPositionLayout() {
        Label firstLabel = new Label(FIRST_CONTENT_TEXT);
        Label secondLabel = new Label(SECOND_CONTENT_TEXT);

        // begin-source-example
        // source-example-heading: Split Layout with Initial Splitter Position
        SplitLayout layout = new SplitLayout(firstLabel, secondLabel);
        layout.setSplitterPosition(80);
        // end-source-example

        layout.getPrimaryComponent().setId("initial-sp-first-component");
        layout.getSecondaryComponent().setId("initial-sp-second-component");
        setMinHeightForLayout(layout);
        addCard("Split Layout with Initial Splitter Position", layout);
    }

    private void addMinMaxWidthLayout() {
        Label firstLabel = new Label(FIRST_CONTENT_TEXT);
        Label secondLabel = new Label(SECOND_CONTENT_TEXT);
        // begin-source-example
        // source-example-heading: Split Layout with Minimum and Maximum Widths
        SplitLayout layout = new SplitLayout();
        layout.addToPrimary(firstLabel);
        layout.addToSecondary(secondLabel);
        layout.setPrimaryStyle("minWidth", "100px");
        layout.setPrimaryStyle("maxWidth", "150px");
        // end-source-example

        layout.getPrimaryComponent().setId("min-max-first-component");
        setMinHeightForLayout(layout);
        addCard("Split Layout with Minimum and Maximum Widths", layout);
    }

    private void setMinHeightForLayout(HasStyle layout) {
        layout.getStyle().set("minHeight", "100px");
    }
}
