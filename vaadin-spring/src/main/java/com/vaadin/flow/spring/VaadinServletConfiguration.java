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
package com.vaadin.flow.spring;

import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.ServletForwardingController;

import com.vaadin.flow.server.VaadinServlet;

/**
 * Vaadin servlet configuration.
 * <p>
 * The configuration is used only when the Vaadin servlet is mapped to the root
 * ({@literal "/*"}) because in this case the servlet is mapped to
 * {@literal "/vaadinServlet/*"} instead of ({@literal "/*"}). It's done to make
 * possible to configure other Spring services (like endpoints) which have
 * overlapping path.
 *
 *
 * @author Vaadin Ltd
 *
 */
@Configuration
@Conditional(RootMappedCondition.class)
public class VaadinServletConfiguration {

    static final String VAADIN_SERVLET_MAPPING = "/vaadinServlet/*";

    /**
     * Makes an url handler mapping allowing to forward requests from a
     * {@link DispatcherServlet} to {@link VaadinServlet}.
     *
     * @return an url handler mapping instance which forwards requests to vaadin
     *         servlet
     */
    @Bean
    public SimpleUrlHandlerMapping vaadinRootMapping() {
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setOrder(Ordered.LOWEST_PRECEDENCE - 1);

        mapping.setUrlMap(
                Collections.singletonMap("/*", vaadinForwardingController()));

        return mapping;
    }

    /**
     * Makes a forwarding controller.
     *
     * @return a forwarding controller
     */
    @Bean
    public Controller vaadinForwardingController() {
        ServletForwardingController controller = new ServletForwardingController();
        controller.setServletName(
                ClassUtils.getShortNameAsProperty(SpringServlet.class));
        return controller;
    }

}
