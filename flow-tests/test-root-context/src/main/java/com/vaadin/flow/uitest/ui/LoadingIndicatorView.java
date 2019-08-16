/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.page.LoadingIndicatorConfiguration;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.LoadingIndicatorView", layout = ViewTestLayout.class)
public class LoadingIndicatorView extends AbstractDivView {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {

        NativeButton disableButton = new NativeButton("Disable default loading indicator theme and add custom");
        disableButton.setId("disable-theme");
        disableButton.addClickListener(
                clickEvent -> {
                    clickEvent.getSource().getUI().get().getLoadingIndicatorConfiguration().setApplyDefaultTheme(false);
                    clickEvent.getSource().getUI().get().getPage().addStyleSheet("frontend://com/vaadin/flow/uitest/ui/loading-indicator.css");
                }
        );
        add(disableButton);
        add(divWithText("First delay: "
                + getLoadingIndicatorConfiguration().getFirstDelay()));
        add(divWithText("Second delay: "
                + getLoadingIndicatorConfiguration().getSecondDelay()));
        add(divWithText("Third delay: "
                + getLoadingIndicatorConfiguration().getThirdDelay()));

        int[] delays = new int[]{100, 200, 500, 1000, 2000, 5000, 10000};
        for (int delay : delays) {
            add(createButton("Trigger event which takes " + delay + "ms",
                    "wait" + delay, e -> delay(delay)));
        }
    }

    private static Div divWithText(String text) {
        Div div = new Div();
        div.setText(text);
        return div;
    }

    private LoadingIndicatorConfiguration getLoadingIndicatorConfiguration() {
        return UI.getCurrent().getLoadingIndicatorConfiguration();
    }

    private void delay(int timeMs) {
        try {
            Thread.sleep(timeMs);
        } catch (InterruptedException e) {
        }
    }

}
