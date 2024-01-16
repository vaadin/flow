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
package com.vaadin.flow.server;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.osgi.OSGiAccess;
import com.vaadin.flow.server.startup.EnableOSGiRunner;

@RunWith(EnableOSGiRunner.class)
public class OSGiVaadinServletTest {

    @Test
    public void init_withEmptyAttributeNamesInServletContext_onlyLookupAttributeFromFakeOSGiContextAreSetFromServletContext()
            throws ServletException {
        ServletContext context = OSGiAccess.getInstance()
                .getOsgiServletContext();

        context.setAttribute("foo", "bar");

        ServletConfig config = Mockito.mock(ServletConfig.class);

        ServletContext servletContext = mockServletContext(config,
                Collections.emptyEnumeration());

        initVaadinServletStub(config);

        Mockito.verify(servletContext).setAttribute(Lookup.class.getName(),
                context.getAttribute(Lookup.class.getName()));
        Mockito.verify(servletContext, Mockito.times(0)).setAttribute("foo",
                "bar");
    }

    @Test
    public void init_withNotEmptyAttributeNamesInServletContext_onlyLookupAttributeFromFakeOSGiContextAreSetFromServletContext()
            throws ServletException {
        List<String> servletContextAttributes = Collections
                .singletonList("bar");

        ServletContext context = OSGiAccess.getInstance()
                .getOsgiServletContext();

        context.setAttribute("foo", "bar");

        ServletConfig config = Mockito.mock(ServletConfig.class);

        ServletContext servletContext = mockServletContext(config,
                Collections.enumeration(servletContextAttributes));

        initVaadinServletStub(config);

        Mockito.verify(servletContext).setAttribute(Lookup.class.getName(),
                context.getAttribute(Lookup.class.getName()));
        Mockito.verify(servletContext, Mockito.times(0)).setAttribute("foo",
                "bar");
    }

    private ServletContext mockServletContext(ServletConfig config,
            Enumeration<String> servletContextAttributes) {
        ServletContext servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(config.getServletContext()).thenReturn(servletContext);
        Mockito.when(servletContext.getAttributeNames())
                .thenReturn(servletContextAttributes);
        return servletContext;
    }

    private void initVaadinServletStub(ServletConfig config)
            throws ServletException {
        VaadinServlet servlet = new VaadinServlet() {
            @Override
            protected DeploymentConfiguration createDeploymentConfiguration() {
                return Mockito.mock(DeploymentConfiguration.class);
            }

            @Override
            protected VaadinServletService createServletService(
                    DeploymentConfiguration deploymentConfiguration) {
                VaadinServletService service = Mockito
                        .mock(VaadinServletService.class);
                Mockito.when(service.getDeploymentConfiguration())
                        .thenReturn(createDeploymentConfiguration());
                return service;
            }
        };
        servlet.init(config);
    }
}
