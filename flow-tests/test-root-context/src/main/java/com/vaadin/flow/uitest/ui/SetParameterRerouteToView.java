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
package com.vaadin.flow.uitest.ui;

import java.util.List;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.SetParameterRerouteToView", layout = ViewTestLayout.class)
public class SetParameterRerouteToView extends Div
        implements HasUrlParameter<String>, AfterNavigationObserver {

    static final String LOCATION_ID = "location";
    static final String PARAMETER_ID = "parameter";

    private final Div location;
    private final Div param;

    public SetParameterRerouteToView() {
        location = new Div();
        location.setId(LOCATION_ID);
        param = new Div();
        param.setId(PARAMETER_ID);
    }

    @Override
    public void setParameter(BeforeEvent event,
            @OptionalParameter String parameter) {
        if (parameter != null) {
            switch (parameter) {
            case "location":
                event.rerouteTo(
                        "com.vaadin.flow.uitest.ui.SetParameterRerouteToView/locationTwo");
                break;
            case "locationRouteParameter":
                event.rerouteTo(
                        "com.vaadin.flow.uitest.ui.SetParameterRerouteToView",
                        "locationRouteParameterTwo");
                break;
            case "locationRouteParameterList":
                event.rerouteTo(
                        "com.vaadin.flow.uitest.ui.SetParameterRerouteToView",
                        List.of("locationRouteParameterListTwo"));
                break;
            case "locationQueryParams":
                event.rerouteTo(
                        "com.vaadin.flow.uitest.ui.SetParameterRerouteToView/locationQueryParamsTwo",
                        QueryParameters.empty());
                break;
            }
        }
        param.setText(parameter);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        location.setText(event.getLocation().getPath());
        add(location, param);
    }

}
