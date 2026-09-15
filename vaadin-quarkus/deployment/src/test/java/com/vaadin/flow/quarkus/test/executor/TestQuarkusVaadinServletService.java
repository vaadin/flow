/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.quarkus.test.executor;

import java.util.Properties;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.servlet.ServletContext;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.quarkus.QuarkusVaadinServlet;
import com.vaadin.quarkus.QuarkusVaadinServletService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TestQuarkusVaadinServletService extends QuarkusVaadinServletService {

    TestQuarkusVaadinServletService(BeanManager beanManager) {
        super(mock(QuarkusVaadinServlet.class),
                mock(DeploymentConfiguration.class), beanManager);
        when(getServlet().getServletName()).thenReturn("QuarkusVaadinServlet");
        when(getServlet().getService()).thenReturn(this);
        final ServletContext servletcontext = mock(ServletContext.class);
        when(getServlet().getServletContext()).thenReturn(servletcontext);
        when(servletcontext.getAttribute(Lookup.class.getName()))
                .thenReturn(mock(Lookup.class));
        DeploymentConfiguration config = getDeploymentConfiguration();
        Properties properties = new Properties();
        when(config.getInitParameters()).thenReturn(properties);
    }

    // We have nothing to do with atmosphere,
    // and mocking is much easier without it.
    @Override
    protected boolean isAtmosphereAvailable() {
        return false;
    }

    @Override
    public void setClassLoader(ClassLoader classLoader) {
        if (classLoader != null) {
            super.setClassLoader(classLoader);
        }
    }

}
