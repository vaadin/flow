/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.startup.DefaultApplicationConfigurationFactory;

/**
 * Passes Spring application properties to the Vaadin application configuration.
 *
 * @author Vaadin Ltd
 *
 * @since 16.0
 */
public class SpringApplicationConfigurationFactory
        extends DefaultApplicationConfigurationFactory {

    /**
     * Creates the factory.
     */
    public SpringApplicationConfigurationFactory() {
        // Default constructor
    }

    @Override
    protected ApplicationConfigurationImpl doCreate(VaadinContext context,
            Map<String, String> properties) {
        ApplicationContext appContext = SpringLookupInitializer
                .getApplicationContext(context);
        Environment env = appContext.getBean(Environment.class);
        // Collect any vaadin.XZY properties from application.properties
        for (String name : SpringServlet.PROPERTY_NAMES) {
            String value = SpringUtil.getVaadinProperty(env, name);
            if (value != null) {
                properties.put(name, value);
            }
        }
        return super.doCreate(context, properties);
    }
}
