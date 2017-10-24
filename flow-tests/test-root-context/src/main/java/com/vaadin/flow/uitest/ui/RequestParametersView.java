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

import java.util.Collections;

import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.router.Route;
import com.vaadin.router.event.BeforeNavigationEvent;
import com.vaadin.router.event.BeforeNavigationListener;
import com.vaadin.ui.html.Div;
import com.vaadin.ui.html.Label;

@Route(value = "com.vaadin.flow.uitest.ui.RequestParametersView", layout = ViewTestLayout.class)
public class RequestParametersView extends Div
        implements BeforeNavigationListener {
    static final String REQUEST_PARAM_NAME = "testRequestParam";
    static final String NO_INPUT_TEXT = "No input";
    static final String REQUEST_PARAM_ID = "requestParamDisplayLabel";

    private final Label requestParamLabel;

    public RequestParametersView() {
        requestParamLabel = new Label(NO_INPUT_TEXT);
        requestParamLabel.setId(REQUEST_PARAM_ID);
        add(requestParamLabel);
    }

    @Override
    public void beforeNavigation(BeforeNavigationEvent event) {
        requestParamLabel.setText(event.getLocation().getQueryParameters()
                .getParameters()
                .getOrDefault(REQUEST_PARAM_NAME, Collections.emptyList())
                .stream().findFirst().orElse(NO_INPUT_TEXT));

    }
}
