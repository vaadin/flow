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

import com.vaadin.hummingbird.dom.Element;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

public class LoadingIndicatorUI extends UI {

    @Override
    protected void init(VaadinRequest request) {
        setIfPresent(request, "first",
                getLoadingIndicatorConfiguration()::setFirstDelay);
        setIfPresent(request, "second",
                getLoadingIndicatorConfiguration()::setSecondDelay);
        setIfPresent(request, "third",
                getLoadingIndicatorConfiguration()::setThirdDelay);

        getElement()
                .appendChild(new Element("div").setTextContent("First delay: "
                        + getLoadingIndicatorConfiguration().getFirstDelay()));
        getElement()
                .appendChild(new Element("div").setTextContent("Second delay: "
                        + getLoadingIndicatorConfiguration().getSecondDelay()));
        getElement()
                .appendChild(new Element("div").setTextContent("Third delay: "
                        + getLoadingIndicatorConfiguration().getThirdDelay()));

        int[] delays = new int[] { 100, 200, 500, 1000, 2000, 5000, 10000 };
        for (int delay : delays) {
            Element button = new Element("button").setTextContent(
                    "Trigger event which takes " + delay + "ms");
            button.setAttribute("id", "wait" + delay);
            button.addEventListener("click", e -> delay(delay));
            getElement().appendChild(button);
        }
    }

    private void setIfPresent(VaadinRequest request, String parameter,
            Consumer<Integer> setter) {
        String value = request.getParameter(parameter);
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
