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
package com.vaadin.flow.uitest.ui;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.ui.html.Div;
import com.vaadin.flow.router.RouterConfiguration;
import com.vaadin.flow.router.RouterConfigurator;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.router.View;
import com.vaadin.flow.uitest.ui.RouterLinkTestServlet.MyRouterConfigurator;
import com.vaadin.server.VaadinServlet;

@WebServlet(asyncSupported = true, urlPatterns = { "/router-link/*" })
@VaadinServletConfiguration(productionMode = false, routerConfigurator = MyRouterConfigurator.class)
public class RouterLinkTestServlet extends VaadinServlet {

    public static class MyRouterConfigurator implements RouterConfigurator {

        @Override
        public void configure(RouterConfiguration configuration) {
            configuration.setRoute("", ViewImpl.class);
            configuration.setRoute("another", AnotherView.class);
        }

    }

    public static class ViewImpl extends Div implements View {
        public ViewImpl() {
            RouterLink link = new RouterLink("Home", ViewImpl.class);
            link.addClassName("root-link");
            add(link);
            getElement().appendChild(ElementFactory.createBr());
            link = new RouterLink("Another view", AnotherView.class);
            add(link);
        }
    }

    public static class AnotherView extends Div implements View {
    }
}
