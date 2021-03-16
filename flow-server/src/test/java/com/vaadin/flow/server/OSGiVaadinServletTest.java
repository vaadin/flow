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

import java.util.Collections;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

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
    public void init_attributesFromFakeOSGiContextAreSetFromServletContext()
            throws ServletException {
        ServletContext context = OSGiAccess.getInstance()
                .getOsgiServletContext();

        context.setAttribute("foo", "bar");

        ServletConfig config = Mockito.mock(ServletConfig.class);

        ServletContext servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(config.getServletContext()).thenReturn(servletContext);
        Mockito.when(servletContext.getAttributeNames())
                .thenReturn(Collections.emptyEnumeration());

        VaadinServlet servlet = new VaadinServlet() {
            @Override
            protected DeploymentConfiguration createDeploymentConfiguration()
                    throws ServletException {
                return Mockito.mock(DeploymentConfiguration.class);
            }

            @Override
            protected VaadinServletService createServletService(
                    DeploymentConfiguration deploymentConfiguration)
                    throws ServiceException {
                return new MockVaadinServletService(deploymentConfiguration);
            }
        };

        servlet.init(config);

        Mockito.verify(servletContext).setAttribute(Lookup.class.getName(),
                context.getAttribute(Lookup.class.getName()));
        Mockito.verify(servletContext).setAttribute("foo", "bar");
    }
}
