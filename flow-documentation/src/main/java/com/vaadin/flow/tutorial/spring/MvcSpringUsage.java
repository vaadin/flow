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
package com.vaadin.flow.tutorial.spring;

import java.util.Collection;
import java.util.Collections;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import com.vaadin.flow.spring.SpringServlet;
import com.vaadin.flow.spring.VaadinMVCWebAppInitializer;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("spring/tutorial-spring-basic-mvc.asciidoc")
public class MvcSpringUsage {

    public abstract class ExampleWebAppInitializer
            implements WebApplicationInitializer {

        @Override
        public void onStartup(ServletContext servletContext)
                throws ServletException {
            AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
            registerConfiguration(context);
            servletContext.addListener(new ContextLoaderListener(context));

            ServletRegistration.Dynamic registration = servletContext
                    .addServlet("dispatcher", new SpringServlet(context));
            registration.setLoadOnStartup(1);
            registration.addMapping("/*");
        }

        private void registerConfiguration(
                AnnotationConfigWebApplicationContext context) {
            // register your configuration classes here
        }
    }

    public class SampleWebAppInitializer extends VaadinMVCWebAppInitializer {

        @Override
        protected Collection<Class<?>> getConfigurationClasses() {
            return Collections.singletonList(SampleConfiguration.class);
        }
    }

    @Configuration
    @ComponentScan
    public class SampleConfiguration {
    }
}
