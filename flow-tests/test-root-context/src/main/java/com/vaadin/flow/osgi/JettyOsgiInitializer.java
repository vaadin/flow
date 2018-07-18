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
package com.vaadin.flow.osgi;

import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.startup.AnnotationValidator;

/**
 * @author Vaadin Ltd
 *
 */
public class JettyOsgiInitializer implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext ctx)
            throws ServletException {
        // One more freaking hack to make ServiceLoader see classes inside Flow
        // bundle
        AnnotationValidator validator = new AnnotationValidator();

        try {
            OSGiHttpServiceRegistration.registerHttpService(ctx);
        } catch (NoClassDefFoundError error) {
            LoggerFactory.getLogger(OSGiHttpServiceRegistration.class)
                    .trace(error.getMessage(), error);
        }
    }

}
