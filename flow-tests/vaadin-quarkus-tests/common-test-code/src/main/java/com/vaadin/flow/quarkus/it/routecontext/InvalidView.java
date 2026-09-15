/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.flow.quarkus.it.routecontext;

import jakarta.inject.Inject;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.quarkus.annotation.RouteScopeOwner;

@Route("invalid-injection")
public class InvalidView extends Div {

    @Inject
    @RouteScopeOwner(ErrorParentView.class)
    /*
     * There is no a ErrorParentView in navigation: this injection has no scope.
     * Pseudo-scope @RouteScoped is used here with the component to immediately
     * get an exception, for normal scope proxy is created and the exception
     * won't be thrown immediately.
     */
    private ErrorParentView bean;

    public InvalidView() {
        setId("invalid-injection");
        setText("This view should not be shown since the "
                + "injection has no scope and an exception should be thrown");
    }
}
