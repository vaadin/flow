/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.uitest.ui;

import java.util.function.Consumer;

import com.vaadin.hummingbird.router.LocationChangeEvent;
import com.vaadin.hummingbird.uitest.component.Button;
import com.vaadin.hummingbird.uitest.component.Div;
import com.vaadin.ui.LoadingIndicatorConfiguration;
import com.vaadin.ui.UI;

public class LoadingIndicatorView extends AbstractDivView {

    @Override
    public void onLocationChange(LocationChangeEvent event) {
        setIfPresent(event, "first",
                getLoadingIndicatorConfiguration()::setFirstDelay);
        setIfPresent(event, "second",
                getLoadingIndicatorConfiguration()::setSecondDelay);
        setIfPresent(event, "third",
                getLoadingIndicatorConfiguration()::setThirdDelay);

        addComponent(new Div("First delay: "
                + getLoadingIndicatorConfiguration().getFirstDelay()));
        addComponent(new Div("Second delay: "
                + getLoadingIndicatorConfiguration().getSecondDelay()));
        addComponent(new Div("Third delay: "
                + getLoadingIndicatorConfiguration().getThirdDelay()));

        int[] delays = new int[] { 100, 200, 500, 1000, 2000, 5000, 10000 };
        for (int delay : delays) {
            Button button = new Button(
                    "Trigger event which takes " + delay + "ms");
            button.setId("wait" + delay);
            button.getElement().addEventListener("click", e -> delay(delay));
            addComponent(button);
        }
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
