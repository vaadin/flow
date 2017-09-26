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

import com.vaadin.ui.html.NativeButton;
import com.vaadin.ui.html.Label;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

/**
 * @author Vaadin Ltd.
 */
public class RequestParametersHistoryUI extends UI {
    static final String REQUEST_PARAM_NAME = "testRequestParam";
    static final String NO_INPUT_TEXT = "No input";
    static final String REQUEST_PARAM_ID = "requestParamDisplayLabel";
    static final String BACK_BUTTON_ID = "backButton";

    private final Label requestParamLabel;

    public RequestParametersHistoryUI() {
        NativeButton backwardButton = new NativeButton("Go back", event -> getPage().getHistory().back());
        backwardButton.setId(BACK_BUTTON_ID);

        requestParamLabel = new Label(NO_INPUT_TEXT);
        requestParamLabel.setId(REQUEST_PARAM_ID);
        add(requestParamLabel, backwardButton);
    }

    @Override
    protected void init(VaadinRequest request) {
        super.init(request);
        String[] params = request.getParameterMap()
                .getOrDefault(REQUEST_PARAM_NAME, new String[0]);
        if (params == null || params.length == 0) {
            requestParamLabel.setText(NO_INPUT_TEXT);
        } else {
            requestParamLabel.setText(params[0]);
        }
    }
}
