/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.spring.test.nativeimage;

import jakarta.servlet.http.HttpServletResponse;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;

public class RerouteErrorView extends Div
        implements HasErrorParameter<RerouteView.RerouteException> {

    public static final String ERROR_ID = "error";

    public RerouteErrorView() {
        setId(ERROR_ID);
        setText("Rerouted to the error view");
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event,
            ErrorParameter<RerouteView.RerouteException> parameter) {
        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }
}
