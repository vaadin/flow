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
package com.vaadin.flow.server.auth;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;

import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.InvalidMenuAccessControlException;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;

import static org.junit.Assert.assertThrows;

@NotThreadSafe
public class DefaultInstantiatorMenuAccessControlTest {
    private ClassLoader contextClassLoader;
    private ClassLoader classLoader;

    @Before
    public void init() throws NoSuchFieldException, IllegalAccessException,
            ClassNotFoundException {
        clearMenuAccessControlField();
        contextClassLoader = Thread.currentThread().getContextClassLoader();

        classLoader = Mockito.mock(ClassLoader.class);
        Mockito.when(classLoader.loadClass(Mockito.any()))
                .thenAnswer(AdditionalAnswers.delegatesTo(contextClassLoader));
        Thread.currentThread().setContextClassLoader(classLoader);
    }

    @After
    public void destroy() throws NoSuchFieldException, IllegalAccessException {
        Thread.currentThread().setContextClassLoader(contextClassLoader);
    }

    @Test
    public void defaultInstantiator_getMenuAccessControl_defaultMenuAccessControl()
            throws ClassNotFoundException {
        VaadinService service = Mockito.mock(VaadinService.class);
        mockLookup(service);
        DefaultInstantiator defaultInstantiator = new DefaultInstantiator(
                service);
        MenuAccessControl menuAccessControl = defaultInstantiator
                .getMenuAccessControl();
        Assert.assertNotNull(menuAccessControl);
        Assert.assertTrue(
                menuAccessControl instanceof DefaultMenuAccessControl);
        Assert.assertSame(menuAccessControl.getPopulateClientSideMenu(),
                MenuAccessControl.PopulateClientMenu.AUTOMATIC);
    }

    @Test
    public void defaultInstantiator_getMenuAccessControl_customMenuAccessControl()
            throws ClassNotFoundException {
        String customMenuAccessControlClassName = "com.vaadin.flow.server.auth.CustomMenuAccessControl";

        VaadinService service = Mockito.mock(VaadinService.class);
        mockLookup(service);
        DefaultInstantiator defaultInstantiator = new DefaultInstantiator(
                service) {
            @Override
            protected String getInitProperty(String propertyName) {
                return customMenuAccessControlClassName;
            }
        };
        MenuAccessControl menuAccessControl = defaultInstantiator
                .getMenuAccessControl();
        Assert.assertNotNull(menuAccessControl);
        Assert.assertTrue(menuAccessControl instanceof CustomMenuAccessControl);
        Assert.assertSame(menuAccessControl.getPopulateClientSideMenu(),
                MenuAccessControl.PopulateClientMenu.ALWAYS);

        Mockito.verify(classLoader).loadClass(customMenuAccessControlClassName);
    }

    @Test
    public void defaultInstantiator_getMenuAccessControlWithInvalidType_throwException() {
        VaadinService service = Mockito.mock(VaadinService.class);
        mockLookup(service);
        DefaultInstantiator defaultInstantiator = new DefaultInstantiator(
                service) {
            @Override
            protected String getInitProperty(String propertyName) {
                return "com.vaadin.flow.server.auth.InvalidMenuAccessControl";
            }
        };
        String errorMessage = assertThrows(
                InvalidMenuAccessControlException.class,
                () -> defaultInstantiator.getMenuAccessControl()).getMessage();
        Assert.assertEquals(
                "Menu access control implementation class property '"
                        + InitParameters.MENU_ACCESS_CONTROL
                        + "' is set to 'com.vaadin.flow.server.auth.InvalidMenuAccessControl' but it's not "
                        + MenuAccessControl.class.getSimpleName()
                        + " implementation",
                errorMessage);
    }

    public static void clearMenuAccessControlField()
            throws NoSuchFieldException, IllegalAccessException {
        Field field = DefaultInstantiator.class
                .getDeclaredField("menuAccessControl");
        field.setAccessible(true);
        ((AtomicReference<MenuAccessControl>) field.get(null)).set(null);
        field.setAccessible(false);
    }

    private Lookup mockLookup(VaadinService service) {
        VaadinContext context = Mockito.mock(VaadinContext.class);
        Mockito.when(service.getContext()).thenReturn(context);

        Lookup lookup = Mockito.mock(Lookup.class);
        Mockito.when(context.getAttribute(Lookup.class)).thenReturn(lookup);
        return lookup;
    }
}
