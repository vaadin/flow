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
package com.vaadin.flow.uitest.ui;

import java.util.function.Consumer;

import com.vaadin.annotations.StyleSheet;
import com.vaadin.flow.html.Button;
import com.vaadin.flow.html.Div;
import com.vaadin.flow.router.LocationChangeEvent;
import com.vaadin.ui.LoadingIndicatorConfiguration;
import com.vaadin.ui.UI;

@StyleSheet("/com/vaadin/flow/uitest/ui/loading-indicator.css")
public class LoadingIndicatorView extends AbstractDivView {

    @Override
    public void onLocationChange(LocationChangeEvent event) {
        setIfPresent(event, "first",
                getLoadingIndicatorConfiguration()::setFirstDelay);
        setIfPresent(event, "second",
                getLoadingIndicatorConfiguration()::setSecondDelay);
        setIfPresent(event, "third",
                getLoadingIndicatorConfiguration()::setThirdDelay);

        add(divWithText("First delay: "
                + getLoadingIndicatorConfiguration().getFirstDelay()));
        add(divWithText("Second delay: "
                + getLoadingIndicatorConfiguration().getSecondDelay()));
        add(divWithText("Third delay: "
                + getLoadingIndicatorConfiguration().getThirdDelay()));

        int[] delays = new int[] { 100, 200, 500, 1000, 2000, 5000, 10000 };
        for (int delay : delays) {
            Button button = new Button(
                    "Trigger event which takes " + delay + "ms",
                    e -> delay(delay));
            button.setId("wait" + delay);
            add(button);
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

    private void setIfPresent(LocationChangeEvent event, String parameter,
            Consumer<Integer> setter) {
        String value = event.getPathParameter(parameter);
        if (value != null) {
            setter.accept(Integer.parseInt(value));
        }

    }

    private void delay(int timeMs) {
        try {
            Thread.sleep(timeMs);
        } catch (InterruptedException e) {
        }
    }

}
