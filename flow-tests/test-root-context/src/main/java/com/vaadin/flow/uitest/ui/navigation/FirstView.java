/*
 * Copyright 2000-2022 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.navigation;

import java.util.Collections;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.navigation.FirstView")
public class FirstView extends Div implements HasUrlParameter<String> {

    static final String PARAM_NAV_BUTTON_ID = "firstViewPramNavButton";
    static final String NAV_BUTTON_ID = "firstViewNavButton";
    static final String QUERY_LABEL_ID = "query";

    private final Label queryLabel;

    public FirstView() {
        queryLabel = new Label();
        queryLabel.setId(QUERY_LABEL_ID);
        add(queryLabel);

        NativeButton button = new NativeButton("Change query parameter",
                e -> UI.getCurrent().navigate(
                        "com.vaadin.flow.uitest.ui.navigation.FirstView/1",
                        QueryParameters.simple(
                                Collections.singletonMap("query", "bar"))));
        button.setId(PARAM_NAV_BUTTON_ID);
        add(button);

        button = new NativeButton("Change view", e -> UI.getCurrent()
                .navigate("com.vaadin.flow.uitest.ui.navigation.SecondView"));
        button.setId(NAV_BUTTON_ID);
        add(button);

    }

    @Override
    public void setParameter(BeforeEvent beforeEvent,
            @OptionalParameter String param) {
        queryLabel.setText(beforeEvent.getLocation().getQueryParameters()
                .getQueryString());
    }
}
