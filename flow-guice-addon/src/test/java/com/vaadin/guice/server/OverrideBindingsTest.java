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
package com.vaadin.guice.server;

import com.google.inject.ConfigurationException;
import com.vaadin.guice.annotation.PackagesToScan;
import com.vaadin.guice.testClasses.*;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.Enumeration;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class OverrideBindingsTest {

    @Test
    public void dynamically_loaded_modules_should_override() throws ReflectiveOperationException, ServletException {
        GuiceVaadinServlet GuiceVaadinServlet = new Servlet1();

        AnInterface anInterface = GuiceVaadinServlet.getInjector().getInstance(AnInterface.class);

        assertNotNull(anInterface);
        assertTrue(anInterface instanceof ASecondImplementation);

        AnotherInterface anotherInterface = GuiceVaadinServlet.getInjector().getInstance(AnotherInterface.class);

        assertNotNull(anotherInterface);
        assertTrue(anotherInterface instanceof AnotherInterfaceImplementation);
    }

    @Test
    public void statically_loaded_modules_should_be_considered() throws ReflectiveOperationException {
        GuiceVaadinServlet guiceVaadinServlet = new Servlet2();

        AnInterface anInterface = guiceVaadinServlet.getInjector().getInstance(AnInterface.class);

        assertNotNull(anInterface);
        assertThat(anInterface, instanceOf(AnImplementation.class));

        AnotherInterface anotherInterface = guiceVaadinServlet.getInjector().getInstance(AnotherInterface.class);

        assertNotNull(anotherInterface);
        assertThat(anotherInterface, instanceOf(AnotherInterfaceImplementation.class));
    }

    @Test
    public void dynamically_loaded_modules_should_be_considered() throws ReflectiveOperationException {
        GuiceVaadinServlet GuiceVaadinServlet = new Servlet3();

        AnInterface anInterface = GuiceVaadinServlet.getInjector().getInstance(AnInterface.class);

        assertNotNull(anInterface);
        assertTrue(anInterface instanceof ASecondImplementation);
    }

    @Test(expected = ConfigurationException.class)
    public void unbound_classes_should_not_be_available() throws ReflectiveOperationException {
        GuiceVaadinServlet GuiceVaadinServlet = new Servlet3();

        GuiceVaadinServlet.getInjector().getInstance(AnotherInterface.class);
    }

    static class TestServlet extends GuiceVaadinServlet {
        TestServlet() {
            try {
                final ServletConfig servletConfig = Mockito.mock(ServletConfig.class);

                final ServletContext servletContext = Mockito.mock(ServletContext.class);

                when(servletConfig.getServletContext()).thenReturn(servletContext);

                final Enumeration<String> initParameters = new Enumeration<String>() {
                    @Override
                    public boolean hasMoreElements() {
                        return false;
                    }

                    @Override
                    public String nextElement() {
                        return null;
                    }
                };

                when(servletConfig.getInitParameterNames()).thenReturn(initParameters);
                when(servletContext.getInitParameterNames()).thenReturn(initParameters);

                init(servletConfig);
            } catch (ServletException e) {
                throw new RuntimeException(e);
            }
        }

    }

    @PackagesToScan({"com.vaadin.guice.testClasses", "com.vaadin.guice.override", "com.vaadin.guice.nonoverride"})
    static class Servlet1 extends TestServlet {
    }

    @PackagesToScan({"com.vaadin.guice.testClasses", "com.vaadin.guice.nonoverride"})
    static class Servlet2 extends TestServlet {
    }

    @PackagesToScan({"com.vaadin.guice.testClasses", "com.vaadin.guice.override"})
    static class Servlet3 extends TestServlet {
    }
}
