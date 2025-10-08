/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.InitialExtendedClientDetailsView")
public class InitialExtendedClientDetailsView extends Div {

    public InitialExtendedClientDetailsView() {
        UI.getCurrent().getPage().retrieveExtendedClientDetails(details -> {
            addSpan("screenWidth", details.getScreenWidth());
            addSpan("screenHeight", details.getScreenHeight());
            addSpan("windowInnerWidth", details.getWindowInnerWidth());
            addSpan("windowInnerHeight", details.getWindowInnerHeight());
            addSpan("bodyClientWidth", details.getBodyClientWidth());
            addSpan("bodyClientHeight", details.getBodyClientHeight());
            addSpan("timezoneOffset", details.getTimezoneOffset());
            addSpan("timeZoneId", details.getTimeZoneId());
            addSpan("rawTimezoneOffset", details.getRawTimezoneOffset());
            addSpan("DSTSavings", details.getDSTSavings());
            addSpan("DSTInEffect", details.isDSTInEffect());
            addSpan("currentDate", details.getCurrentDate());
            addSpan("touchDevice", details.isTouchDevice());
            addSpan("devicePixelRatio", details.getDevicePixelRatio());
            addSpan("windowName", details.getWindowName());
        });
    }

    private void addSpan(String name, Object value) {
        Span span = new Span(value.toString());
        span.setId(name);
        add(span);

    }
}
