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
package com.vaadin.flow.tutorial.routing;

import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("routing/tutorial-routing-get-registered-routes.asciidoc")
public class RountingGettingRoutes {

    public void getRoutes() {
        //@formatter:off
        Router router = UI.getCurrent().getRouter().get();
        List<RouteData> routes = router.getRoutes();

        Map<Class<? extends RouterLayout>, List<RouteData>> routesByParent = router.getRoutesByParent();
        List<RouteData> myRoutes = routesByParent.get(MyParentLayout.class);
        //@formatter:on
    }

    private abstract class MyParentLayout implements RouterLayout {

    }

}
