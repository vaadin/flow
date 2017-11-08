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
package com.vaadin.server;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.shared.ApplicationConstants;

public class I18NRegistryTest {

    @Before
    public void init() throws NoSuchFieldException, IllegalAccessException {
        Field initialized = I18NRegistry.class.getDeclaredField("initialized");
        initialized.setAccessible(true);
        initialized.set(I18NRegistry.getInstance(), new AtomicReference<>());
    }

    @Test
    public void no_property_defined_should_init_registy()
            throws ServletException, ServiceException {
        initServletAndService(new Properties());

        Assert.assertTrue("Registry should have been initialized",
                I18NRegistry.getInstance().isInitialized());
    }

    @Test
    public void no_property_defined_should_leave_with_default_locale()
            throws ServletException, ServiceException {
        initServletAndService(new Properties());

        Assert.assertEquals("Locale was not the expected default locale",
                Locale.getDefault(), VaadinSession.getCurrent().getLocale());
    }

    @Test
    public void property_defined_should_init_registy_with_provider()
            throws ServletException, ServiceException {
        Properties initParams = new Properties();
        initParams.setProperty(Constants.I18N_PROVIDER,
                "com.vaadin.server.TestProvider");

        initServletAndService(initParams);

        Assert.assertTrue("Registry should have been initialized",
                I18NRegistry.getInstance().isInitialized());

        Assert.assertEquals("Found wrong registry", TestProvider.class,
                I18NRegistry.getInstance().getProvider().getClass());
    }

    @Test
    public void with_defined_provider_locale_should_be_the_available_one()
            throws ServletException, ServiceException {
        Properties initParams = new Properties();
        initParams.setProperty(Constants.I18N_PROVIDER,
                "com.vaadin.server.TestProvider");

        initServletAndService(initParams);

        Assert.assertNotNull("No provider for ",
                I18NRegistry.getInstance().getProvider());

        Assert.assertEquals(
                "Locale was not the defined locale", I18NRegistry.getInstance()
                        .getProvider().getProvidedLocales().get(0),
                VaadinSession.getCurrent().getLocale());

    }

    private VaadinServlet initServletAndService(Properties initParams)
            throws ServletException, ServiceException {
        ServletConfig servletConfig = new MockServletConfig(initParams);
        VaadinServlet servlet = new VaadinServlet();
        servlet.init(servletConfig);
        VaadinService service = servlet.getService();

        HttpServletRequest httpServletRequest = Mockito
                .mock(HttpServletRequest.class);
        HttpSession mockHttpSession = Mockito.mock(HttpSession.class);
        WrappedSession mockWrappedSession = new WrappedHttpSession(
                mockHttpSession) {
            final ReentrantLock lock = new ReentrantLock();
            {
                lock.lock();
            }

            @Override
            public Object getAttribute(String name) {
                Object res;
                String lockAttribute = service.getServiceName() + ".lock";
                if (lockAttribute.equals(name)) {
                    res = lock;
                } else {
                    res = super.getAttribute(name);
                }
                return res;
            }
        };

        VaadinRequest request = new VaadinServletRequest(httpServletRequest,
                servlet.getService()) {
            @Override
            public String getParameter(String name) {
                if (ApplicationConstants.REQUEST_TYPE_PARAMETER.equals(name)) {
                    return null;
                }
                return "1";
            }

            @Override
            public WrappedSession getWrappedSession(
                    boolean allowSessionCreation) {
                return mockWrappedSession;
            }
        };

        try {
            service.findVaadinSession(request);
        } catch (SessionExpiredException e) {
            e.printStackTrace();
        }

        service.init();

        return servlet;
    }

}
