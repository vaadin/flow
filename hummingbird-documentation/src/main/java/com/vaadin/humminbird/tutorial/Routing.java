/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.humminbird.tutorial;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.humminbird.tutorial.annotations.CodeFor;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.ElementFactory;
import com.vaadin.hummingbird.router.ModifiableRouterConfiguration;
import com.vaadin.hummingbird.router.RouterConfigurator;
import com.vaadin.hummingbird.router.View;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.UI;

@CodeFor("tutorial-routing.asciidoc")
public class Routing {

    @WebServlet(urlPatterns = "/*", name = "MyServlet", asyncSupported = true)
    @VaadinServletConfiguration(routerConfigurator = MyRouterConfigurator.class, productionMode = false)
    public static class MyServlet extends VaadinServlet {
    }

    public static class MyRouterConfigurator implements RouterConfigurator {
        @Override
        public void configure(ModifiableRouterConfiguration configuration) {
            configuration.setRoute("", HomeView.class);
            configuration.setRoute("company", CompanyView.class);
        }

    }

    public class HomeView implements View {

        private Element element;

        public HomeView() {
            element = ElementFactory.createDiv("This is the home view");
        }

        @Override
        public Element getElement() {
            return element;
        }
    }

    public class CompanyView extends HomeView {

    }

    void navigation() {
        Element button = ElementFactory.createButton("Navigate to company");
        button.addEventListener("click", e -> {
            UI.getCurrent().navigateTo("company");
        });

    }
}
