/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
