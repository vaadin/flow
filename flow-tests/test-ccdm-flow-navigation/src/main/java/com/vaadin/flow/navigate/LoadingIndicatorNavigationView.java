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
package com.vaadin.flow.navigate;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "loading-indicator-navigation")
@PageTitle("Loading Indicator Navigation Tests")
public class LoadingIndicatorNavigationView extends Div {

    public static final String SLOW_NAVIGATE_BUTTON_ID = "slow-navigate-button";
    public static final String FAST_NAVIGATE_BUTTON_ID = "fast-navigate-button";
    public static final String SOURCE_LABEL_ID = "source-label";

    public LoadingIndicatorNavigationView() {
        Paragraph sourceLabel = new Paragraph("Source view");
        sourceLabel.setId(SOURCE_LABEL_ID);
        add(sourceLabel);

        // Button that does server-side slow work before navigating
        NativeButton slowNavigateButton = new NativeButton("Navigate to slow view",
                event -> {
                    UI.getCurrent().navigate(
                            LoadingIndicatorNavigationSlowTargetView.class);
                });
        slowNavigateButton.setId(SLOW_NAVIGATE_BUTTON_ID);
        add(slowNavigateButton);

        // Fast navigation button (no delay) for baseline testing
        NativeButton fastNavigateButton = new NativeButton("Navigate to fast view",
                event -> UI.getCurrent()
                        .navigate(LoadingIndicatorNavigationFastTargetView.class));
        fastNavigateButton.setId(FAST_NAVIGATE_BUTTON_ID);
        add(fastNavigateButton);
    }
}
