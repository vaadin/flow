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
package com.vaadin.flow.server;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import net.jcip.annotations.NotThreadSafe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.tests.util.MockDeploymentConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@NotThreadSafe
public class I18NProviderTest {

    private VaadinServletService service;

    private MockDeploymentConfiguration config = new MockDeploymentConfiguration();

    @Test
    public void no_property_defined_should_leave_with_default_locale()
            throws ServletException, ServiceException {
        initServletAndService(config);

        assertEquals(Locale.getDefault(),
                VaadinSession.getCurrent().getLocale(),
                "Locale was not the expected default locale");
    }

    @Test
    public void property_defined_should_init_registry_with_provider()
            throws ServletException, ServiceException {
        config.setApplicationOrSystemProperty(InitParameters.I18N_PROVIDER,
                TestProvider.class.getName());

        initServletAndService(config);

        Instantiator instantiator = VaadinService.getCurrent()
                .getInstantiator();
        assertEquals(TestProvider.class,
                instantiator.getI18NProvider().getClass(),
                "Found wrong registry");
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
        assertNotNull(i18NProvider, "No provider for ");

        assertEquals(i18NProvider.getProvidedLocales().get(0),
                VaadinSession.getCurrent().getLocale(),
                "Locale was not the defined locale");
    }

    @Test
    public void translate_calls_provider()
            throws ServletException, ServiceException {
        config.setApplicationOrSystemProperty(InitParameters.I18N_PROVIDER,
                TestProvider.class.getName());

        initServletAndService(config);

        assertEquals("!foo.bar!", I18NProvider.translate("foo.bar"),
                "translate method should return a value");
    }

    @Test
    public void translate_withoutProvider_returnsKey()
            throws ServletException, ServiceException {
        initServletAndService(config);

        assertEquals("!{foo.bar}!", I18NProvider.translate("foo.bar"),
                "Should return the key with !{}! to show no translation available");
    }

    @Test
    public void translate_withoutVaadinService_throwIllegalStateException()
            throws ServletException, ServiceException {
        config.setApplicationOrSystemProperty(InitParameters.I18N_PROVIDER,
                TestProvider.class.getName());

        initServletAndService(config);

        VaadinService.setCurrent(null);

        assertThrows(IllegalStateException.class,
                () -> I18NProvider.translate("foo.bar"),
                "Should throw exception without active VaadinService");
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

        assertThrows(IllegalStateException.class,
                () -> I18NProvider.translate("foo.bar"),
                "Should throw exception without active VaadinService");
    }

    @BeforeEach
    public void initState()
            throws NoSuchFieldException, IllegalAccessException {
        clearI18NProviderField();
    }

    @AfterEach
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
