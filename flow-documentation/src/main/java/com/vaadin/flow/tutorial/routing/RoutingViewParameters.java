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

import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.flow.html.Div;
import com.vaadin.flow.router.LocationChangeEvent;
import com.vaadin.flow.router.RouterConfiguration;
import com.vaadin.flow.router.RouterConfigurator;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.router.View;
import com.vaadin.server.VaadinServlet;

@CodeFor("tutorial-routing-view-parameters.asciidoc")
public class RoutingViewParameters {

    private interface Product {
        String getId();

        String getName();
    }

    @WebServlet(urlPatterns = "/*", name = "MyServlet", asyncSupported = true)
    @VaadinServletConfiguration(routerConfigurator = MyRouterConfigurator.class, productionMode = false)
    public static class MyServlet extends VaadinServlet {
    }

    public static class MyRouterConfigurator implements RouterConfigurator {
        @Override
        public void configure(RouterConfiguration configuration) {
            configuration.setRoute("product/{id}", ProductView.class);
            configuration.setRoute("docs/*", DocsView.class);
        }
    }

    public class ProductView extends Div implements View {

        public ProductView() {
            setText("This is the home view");
        }

        @Override
        public void onLocationChange(LocationChangeEvent locationChangeEvent) {
            String productId = locationChangeEvent.getPathParameter("id");
            System.out.println(productId);
        }

    }

    public class DocsView extends ProductView {
        @Override
        public void onLocationChange(LocationChangeEvent locationChangeEvent) {
            String docsSubPage = locationChangeEvent.getPathWildcard();
            System.out.println(docsSubPage);
        }

    }

    @SuppressWarnings("null")
    void routerLink() {
        Product product = null;

        new RouterLink(product.getName(), ProductView.class, product.getId());
    }
}
