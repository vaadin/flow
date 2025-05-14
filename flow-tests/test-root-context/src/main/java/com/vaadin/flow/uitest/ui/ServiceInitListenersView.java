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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ServiceInitListenersView", layout = ViewTestLayout.class)
public class ServiceInitListenersView extends Div
        implements HasUrlParameter<Integer> {
    private static final String OPTIONAL_PARAMETER_LABEL_TEXT_PREFIX = "Before init count: ";
    private final NativeLabel optionalParameterLabel;

    public ServiceInitListenersView() {
        optionalParameterLabel = new NativeLabel();
        add(optionalParameterLabel);
        add(new NativeLabel(
                "Init count: " + TestingServiceInitListener.getInitCount()));
        add(new NativeLabel("Request count: "
                + TestingServiceInitListener.getRequestCount()));
    }

    @Override
    public void setParameter(BeforeEvent event,
            @OptionalParameter Integer parameter) {
        optionalParameterLabel
                .setText(OPTIONAL_PARAMETER_LABEL_TEXT_PREFIX + parameter);
    }
}
