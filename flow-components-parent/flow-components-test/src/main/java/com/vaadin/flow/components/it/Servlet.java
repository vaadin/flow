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
package com.vaadin.flow.components.it;

import java.util.List;

import javax.servlet.annotation.WebServlet;

import com.vaadin.server.VaadinServletConfiguration;
import com.vaadin.flow.components.it.Servlet.TestRouterConfigurator;
import com.vaadin.flow.router.RouterConfiguration;
import com.vaadin.flow.router.RouterConfigurator;
import com.vaadin.server.VaadinServlet;

/**
 * The test application servlet.
 */
@WebServlet(urlPatterns = "/*", name = "TestServlet", asyncSupported = true)
@VaadinServletConfiguration(routerConfigurator = TestRouterConfigurator.class, productionMode = false, usingNewRouting = false)
public class Servlet extends VaadinServlet {

    /**
     * Router configuration that collects all test views.
     */
    public static class TestRouterConfigurator implements RouterConfigurator {
        RouterConfiguration configuration;

        @Override
        public void configure(RouterConfiguration configuration) {
            this.configuration = configuration;
            List<Class<? extends TestView>> availableViews = TestViewRegistry
                    .getAvailableViews();
            availableViews.forEach(this::createRoute);
        }

        private void createRoute(Class<? extends TestView> aClass) {
            configuration.setRoute(aClass.getSimpleName(), aClass);
        }
    }
}
