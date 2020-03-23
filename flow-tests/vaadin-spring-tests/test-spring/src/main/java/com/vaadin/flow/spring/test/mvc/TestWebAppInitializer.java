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
package com.vaadin.flow.spring.test.mvc;

import java.util.Collection;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import com.vaadin.flow.spring.VaadinMVCWebAppInitializer;
import com.vaadin.flow.spring.test.TestConfiguration;

/**
 * The entry point for Spring MVC.
 *
 * @author Vaadin Ltd
 *
 */
public class TestWebAppInitializer extends VaadinMVCWebAppInitializer {

    @Autowired
    private ConfigurableEnvironment env;

    @Override
    protected Collection<Class<?>> getConfigurationClasses() {
        return Collections.singletonList(TestConfiguration.class);
    }

    @Override
    protected void registerConfiguration(
            AnnotationConfigWebApplicationContext context) {
        context.getEnvironment().setActiveProfiles("enabled");
        super.registerConfiguration(context);
    }
}
