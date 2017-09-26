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

import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.router.HasUrlParameter;
import com.vaadin.router.NotFoundException;
import com.vaadin.router.Route;
import com.vaadin.router.Router;
import com.vaadin.router.event.BeforeNavigationEvent;
import com.vaadin.ui.UI;
import com.vaadin.ui.html.Anchor;
import com.vaadin.ui.html.Div;

@CodeFor("routing/tutorial-routing-url-generation.asciidoc")
public class RoutingUrlGeneration {

    @Route("path")
    public class PathComponent extends Div {
        public PathComponent() {
            setText("Hello @Route!");
        }
    }

    public class Menu extends Div {
        public Menu() {
            try {
                String route = ((Router) UI.getCurrent().getRouter().get())
                        .getUrl(PathComponent.class);
                Anchor link = new Anchor(route, "Path");
                add(link);
            } catch (NotFoundException e) {
                // Omitted
            }
        }
    }

    @Route(value = "greet")
    public class GreetingComponent extends Div
            implements HasUrlParameter<String> {

        @Override
        public void setParameter(BeforeNavigationEvent event,
                String parameter) {
            setText(String.format("Hello, %s!", parameter));
        }
    }

    public class ParameterMenu extends Div {
        public ParameterMenu() {
            try {
                String route = ((Router) UI.getCurrent().getRouter().get())
                        .getUrl(GreetingComponent.class, "anonymous");
                Anchor link = new Anchor(route, "Greeting");
                add(link);
            } catch (NotFoundException e) {
                // Omitted
            }
        }
    }
}
