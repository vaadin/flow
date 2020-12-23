/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.frontend.FallbackChunk;
import com.vaadin.flow.server.startup.DefaultApplicationConfigurationFactory;

/**
 * Passes Spring application properties to the Vaadin application configuration.
 * 
 * @author Vaadin Ltd
 * @since
 *
 */
public class SpringApplicationConfigurationFactory
        extends DefaultApplicationConfigurationFactory {

    @Component
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    private static class AppContextHolder {

        @Autowired
        private ApplicationContext context;

        private static AtomicReference<AppContextHolder> self = new AtomicReference<>();

        private AppContextHolder() {
            self.set(this);
        }
    }

    @Override
    protected ApplicationConfigurationImpl doCreate(VaadinContext context,
            FallbackChunk chunk, Map<String, String> properties) {
        // don't use Spring component and injection because there is no way to
        // get the object of this class as a Spring bean (it comes from standard
        // WAR Lookup which instantiates everything directly via default CTOR)
        VaadinServletContext servletContext = (VaadinServletContext) context;
        ApplicationContext appContext = WebApplicationContextUtils
                .getWebApplicationContext(servletContext.getContext());
        // sometimes for deployable WAR (couldn't find exact circumstances) the
        // web app context is not yet available here, let's use a hack to get an
        // app context from Spring singelton bean
        if (appContext == null) {
            appContext = AppContextHolder.self.get().context;
        }
        Environment env = appContext.getBean(Environment.class);
        // Collect any vaadin.XZY properties from application.properties
        SpringServlet.PROPERTY_NAMES.stream()
                .filter(name -> env.getProperty("vaadin." + name) != null)
                .forEach(name -> properties.put(name,
                        env.getProperty("vaadin." + name)));
        return super.doCreate(context, chunk, properties);
    }
}
