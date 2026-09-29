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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

/**
 * Reroutes to an application error view by exception type. The router creates
 * the exception through reflection and finds the error view by scanning the
 * classpath.
 */
@Route("reroute")
public class RerouteView extends Div implements BeforeEnterObserver {

    public static class RerouteException extends RuntimeException {
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.rerouteToError(RerouteException.class);
    }
}
