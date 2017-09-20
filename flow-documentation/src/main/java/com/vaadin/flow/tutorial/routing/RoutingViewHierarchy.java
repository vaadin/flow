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

import javax.servlet.annotation.WebServlet;

import com.vaadin.server.VaadinServletConfiguration;
import com.vaadin.flow.dom.Element;
import com.vaadin.ui.html.Div;
import com.vaadin.flow.router.HasChildView;
import com.vaadin.flow.router.RouterConfiguration;
import com.vaadin.flow.router.RouterConfigurator;
import com.vaadin.flow.router.View;
import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Component;

@CodeFor("routing/tutorial-routing-view-hierarchy.asciidoc")
public class RoutingViewHierarchy {

    @WebServlet(urlPatterns = "/*", name = "MyServlet", asyncSupported = true)
    @VaadinServletConfiguration(routerConfigurator = MyRouterConfigurator.class, productionMode = false)
    public static class MyServlet extends VaadinServlet {
    }

    public static class MyRouterConfigurator implements RouterConfigurator {
        @Override
        public void configure(RouterConfiguration configuration) {
            //@formatter:off - custom line wrapping
            configuration.setRoute("company", CompanyView.class, MainLayout.class);
            //@formatter:on
        }
    }

    public static class MyRouterConfigurator2 implements RouterConfigurator {
        @Override
        public void configure(RouterConfiguration configuration) {
            //@formatter:off - custom line wrapping
            configuration.setRoute("company", CompanyView.class, CompanySideBarView.class);
            configuration.setParentView(CompanySideBarView.class, MainLayout.class);
            //@formatter:on
        }
    }

    public class CompanySideBarView implements HasChildView, View {

        @Override
        public Element getElement() {
            return null;
        }

        @Override
        public void setChildView(View childView) {

        }

    }

    public class MainLayout extends Div implements HasChildView {

        private Div childContainer;

        public MainLayout() {
            // Initialize the main layout DOM
            //@formatter:off
            Div header = new Div();
            header.setText("This header will always be shown");
            //@formatter:on

            childContainer = new Div();
            add(header, childContainer);
        }

        @Override
        public void setChildView(View childView) {
            childContainer.removeAll();
            childContainer.add((Component) childView);
        }
    }

    public class CompanyView extends Div implements View {

        public CompanyView() {
            super();
            setText("This is the home view");
        }

    }

}
