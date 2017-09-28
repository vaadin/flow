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
package com.vaadin.flow.uitest.servlet;

import java.util.Optional;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import com.vaadin.server.VaadinServletConfiguration;
import com.vaadin.router.event.NavigationEvent;
import com.vaadin.router.NavigationHandler;
import com.vaadin.flow.router.Resolver;
import com.vaadin.flow.router.RouterConfiguration;
import com.vaadin.flow.router.RouterConfigurator;
import com.vaadin.flow.router.View;
import com.vaadin.flow.uitest.servlet.ViewTestServlet.ViewTestConfigurator;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletService;

@WebServlet(asyncSupported = true, urlPatterns = { "/view/*" })
@VaadinServletConfiguration(productionMode = false, routerConfigurator = ViewTestConfigurator.class)
public class ViewTestServlet extends VaadinServlet {

    private static ViewClassLocator viewLocator;

    public static class ViewTestConfigurator implements RouterConfigurator {
        @Override
        public void configure(RouterConfiguration configuration) {
            configuration.setResolver(new Resolver() {
                @Override
                public Optional<NavigationHandler> resolve(
                        NavigationEvent navigationEvent) {
                    try {
                        Class<? extends View> viewType = viewLocator
                                .findViewClass(navigationEvent.getLocation()
                                        .getFirstSegment());
                        if (VaadinServletService.getCurrentRequest()
                                .getParameter("noheader") != null) {
                            return Optional.of(new TestViewRenderer(viewType));
                        } else {
                            return Optional.of(new TestViewRenderer(viewType,
                                    ViewTestLayout.class));
                        }
                    } catch (ClassNotFoundException e) {
                        return Optional.empty();
                    }
                }
            });
            configuration.setErrorView(ErrorView.class, ViewTestLayout.class);

        }
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        viewLocator = new ViewClassLocator(getService().getClassLoader());
    }

    static ViewClassLocator getViewLocator() {
        return viewLocator;
    }

}
