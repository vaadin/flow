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
package com.vaadin.flow.demo;

import javax.servlet.annotation.WebServlet;
import java.util.List;

import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.flow.demo.Servlet.MyRouterConfigurator;
import com.vaadin.flow.demo.views.DemoView;
import com.vaadin.flow.router.RouterConfiguration;
import com.vaadin.flow.router.RouterConfigurator;
import com.vaadin.server.VaadinServlet;

/**
 * The main servlet for the application.
 */
@WebServlet(urlPatterns = "/*", name = "DemoServlet", asyncSupported = true)
@VaadinServletConfiguration(routerConfigurator = MyRouterConfigurator.class, productionMode = false)
public class Servlet extends VaadinServlet {

    /**
     * The router configurator defines the how to map URLs to views.
     */
    public static class MyRouterConfigurator implements RouterConfigurator {
        RouterConfiguration configuration;

        @Override
        public void configure(RouterConfiguration configuration) {
            this.configuration = configuration;
            List<Class<? extends DemoView>> availableViews = ComponentDemoRegister
                    .getAvailableViews();
            availableViews.forEach(this::createRoute);

            if (!availableViews.isEmpty()) {
                configuration.setRoute("", availableViews.get(0),
                        MainLayout.class);
            }
        }

        private void createRoute(Class<? extends DemoView> aClass) {
            ComponentDemo annotation = aClass
                    .getAnnotation(ComponentDemo.class);
            configuration.setRoute(annotation.href(), aClass, MainLayout.class);
        }
    }
}
