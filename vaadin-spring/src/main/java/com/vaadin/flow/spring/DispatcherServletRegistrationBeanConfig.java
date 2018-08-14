/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Creates a {@link DispatcherServletRegistrationBean} instance for a dispatcher
 * servlet in case Vaadin servlet is mapped to the root.
 * <p>
 * This is a workaround for spring boot 2.0.4 compatibility (see spring#331).
 *
 * @see #dispatcherServletRegistration()
 *
 * @author Vaadin Ltd
 *
 */
@Configuration
@Conditional(RootMappedCondition.class)
@ConditionalOnClass(DispatcherServletRegistrationBean.class)
public class DispatcherServletRegistrationBeanConfig {

    @Autowired
    private WebApplicationContext context;

    /**
     * Creates a {@link DispatcherServletRegistrationBean} instance for a
     * dispatcher servlet in case Vaadin servlet is mapped to the root.
     * <p>
     * This is needed for correct servlet path (and path info) values available
     * in Vaadin servlet because it works via forwarding controller which is not
     * properly mapped without this registration.
     *
     * @return a custom DispatcherServletRegistrationBean instance for
     *         dispatcher servlet
     */
    @Bean
    public DispatcherServletRegistrationBean dispatcherServletRegistration() {
        DispatcherServlet servlet = context.getBean(DispatcherServlet.class);
        DispatcherServletRegistrationBean registration = new DispatcherServletRegistrationBean(
                servlet, "/*");
        registration.setName("dispatcher");
        return registration;
    }
}
