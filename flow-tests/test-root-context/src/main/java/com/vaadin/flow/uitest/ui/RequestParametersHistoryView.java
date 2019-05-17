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

import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;

/**
 * @author Vaadin Ltd
 * @since 1.0.
 */
@Route("com.vaadin.flow.uitest.ui.RequestParametersHistoryView")
public class RequestParametersHistoryView extends AbstractDivView {
    static final String REQUEST_PARAM_NAME = "testRequestParam";
    static final String NO_INPUT_TEXT = "No input";
    static final String REQUEST_PARAM_ID = "requestParamDisplayLabel";
    static final String BACK_BUTTON_ID = "backButton";

    private final Label requestParamLabel;

    public RequestParametersHistoryView() {
        NativeButton backwardButton = createButton("Go back", BACK_BUTTON_ID,
                event -> getPage().getHistory().back());

        requestParamLabel = new Label(NO_INPUT_TEXT);
        requestParamLabel.setId(REQUEST_PARAM_ID);
        add(requestParamLabel, backwardButton);

        String[] params = VaadinService.getCurrentRequest().getParameterMap()
                .getOrDefault(REQUEST_PARAM_NAME, new String[0]);
        if (params == null || params.length == 0) {
            requestParamLabel.setText(NO_INPUT_TEXT);
        } else {
            requestParamLabel.setText(params[0]);
        }
    }
}
