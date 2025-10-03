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
package com.vaadin.flow.server;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.tests.util.MockDeploymentConfiguration;

@NotThreadSafe
public class I18NProviderTest {

    private VaadinServletService service;

    private MockDeploymentConfiguration config = new MockDeploymentConfiguration();

    @Test
    public void no_property_defined_should_leave_with_default_locale()
            throws ServletException, ServiceException {
        initServletAndService(config);

        Assert.assertEquals("Locale was not the expected default locale",
                Locale.getDefault(), VaadinSession.getCurrent().getLocale());
    }

    @Test
    public void property_defined_should_init_registry_with_provider()
            throws ServletException, ServiceException {
        config.setApplicationOrSystemProperty(InitParameters.I18N_PROVIDER,
                TestProvider.class.getName());

        initServletAndService(config);

        Instantiator instantiator = VaadinService.getCurrent()
                .getInstantiator();
        Assert.assertEquals("Found wrong registry", TestProvider.class,
                instantiator.getI18NProvider().getClass());
    }

    @Test
    public void with_defined_provider_locale_should_be_the_available_one()
            throws ServletException, ServiceException {
        config.setApplicationOrSystemProperty(InitParameters.I18N_PROVIDER,
                TestProvider.class.getName());

        initServletAndService(config);

        Instantiator instantiator = VaadinService.getCurrent()
                .getInstantiator();
        I18NProvider i18NProvider = instantiator.getI18NProvider();
        Assert.assertNotNull("No provider for ", i18NProvider);

        Assert.assertEquals("Locale was not the defined locale",
                i18NProvider.getProvidedLocales().get(0),
                VaadinSession.getCurrent().getLocale());
    }

    @Test
    public void translate_calls_provider()
            throws ServletException, ServiceException {
        config.setApplicationOrSystemProperty(InitParameters.I18N_PROVIDER,
                TestProvider.class.getName());

        initServletAndService(config);

        Assert.assertEquals("translate method should return a value",
                "!foo.bar!", I18NProvider.translate("foo.bar"));
    }

    @Test
    public void translate_withoutProvider_returnsKey()
            throws ServletException, ServiceException {
        initServletAndService(config);

        Assert.assertEquals(
                "Should return the key with !{}! to show no translation available",
                "!{foo.bar}!", I18NProvider.translate("foo.bar"));
    }

    @Test
    public void translate_withoutVaadinService_throwIllegalStateException()
            throws ServletException, ServiceException {
        config.setApplicationOrSystemProperty(InitParameters.I18N_PROVIDER,
                TestProvider.class.getName());

        initServletAndService(config);

        VaadinService.setCurrent(null);

        Assert.assertThrows(
                "Should throw exception without active VaadinService",
                IllegalStateException.class,
                () -> I18NProvider.translate("foo.bar"));
    }

    @Test
    public void translate_withoutInstantiator_throwIllegalStateException()
            throws ServletException, ServiceException {
        config.setApplicationOrSystemProperty(InitParameters.I18N_PROVIDER,
                TestProvider.class.getName());

        initServletAndService(config);
        service = new MockVaadinServletService(config) {
            @Override
            public Instantiator getInstantiator() {
                return null;
            }
        };

        VaadinService.setCurrent(service);

        Assert.assertThrows(
                "Should throw exception without active VaadinService",
                IllegalStateException.class,
                () -> I18NProvider.translate("foo.bar"));
    }

    @Before
    public void initState()
            throws NoSuchFieldException, IllegalAccessException {
        clearI18NProviderField();
    }

    @After
    public void clearCurrentInstances()
            throws NoSuchFieldException, IllegalAccessException {
        CurrentInstance.clearAll();
        clearI18NProviderField();
    }

    public static void clearI18NProviderField()
            throws NoSuchFieldException, IllegalAccessException {
        Field field = DefaultInstantiator.class
                .getDeclaredField("i18nProvider");
        field.setAccessible(true);
        ((AtomicReference<I18NProvider>) field.get(null)).set(null);
        field.setAccessible(false);
    }

    private void initServletAndService(DeploymentConfiguration config)
            throws ServletException, ServiceException {
        service = new MockVaadinServletService(config) {
            @Override
            public Instantiator getInstantiator() {
                return new DefaultInstantiator(service);
            }
        };

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
                service) {
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
            throw new RuntimeException(e);
        }

        VaadinService.setCurrent(service);
    }
}
