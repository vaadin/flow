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
        addHeightLayout();
        addInitialSplitterPositionLayout();
        addMinMaxLayout();
        addResizeNotificationLayout();
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
        VaadinSplitLayout layout = new VaadinSplitLayout();
        VaadinSplitLayout nestedLayout = new VaadinSplitLayout().setVertical(true);
        layout.addToPrimary(new Label("First content component"));
        nestedLayout.addToPrimary(new Label("Second content component"));
        nestedLayout.addToSecondary(new Label("Third content component"));
        layout.addToSecondary(nestedLayout);
        // end-source-example
        // @formatter:on
        addCard(new H3("Layout Combination"), layout);
    }

    private void addHeightLayout() {
        // begin-source-example
        // source-example-heading: Split Layout Element Height

        // end-source-example
    }

    private void addInitialSplitterPositionLayout() {
        // begin-source-example
        // source-example-heading: Initial Splitter Position

        // end-source-example
    }

    private void addMinMaxLayout() {
        // begin-source-example
        // source-example-heading: Specifying Min- and Max- Sizes

        // end-source-example
    }

    private void addResizeNotificationLayout() {
        // begin-source-example
        // source-example-heading: Resize Notification for the Nested Elements

        // end-source-example
    }
}
