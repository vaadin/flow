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

import jakarta.annotation.PostConstruct;

import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.quarkus.annotation.RouteScopeOwner;
import com.vaadin.quarkus.annotation.RouteScoped;

@RouteScoped
@RouteScopeOwner(ErrorParentView.class)
@Route("error-layout")
public class ErrorParentView extends AbstractCountedView
        implements RouterLayout {

    public static final String ROOT = "root";

    @PostConstruct
    private void init() {
        add(new RouterLink(ROOT, RootView.class));
    }

}
