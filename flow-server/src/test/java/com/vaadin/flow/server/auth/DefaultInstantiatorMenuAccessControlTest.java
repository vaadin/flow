/*
 * Copyright 2000-2024 Vaadin Ltd.
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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;

public class DefaultInstantiatorMenuAccessControlTest {

    @Before
    public void init() throws NoSuchFieldException, IllegalAccessException {
        clearMenuAccessControlField();
    }

    @Test
    public void defaultInstantiator_getMenuAccessControl_defaultMenuAccessControl() {
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
    public void defaultInstantiator_getMenuAccessControl_customMenuAccessControl() {
        VaadinService service = Mockito.mock(VaadinService.class);
        mockLookup(service);
        DefaultInstantiator defaultInstantiator = new DefaultInstantiator(
                service) {
            @Override
            protected String getInitProperty(String propertyName) {
                return "com.vaadin.flow.server.auth.CustomMenuAccessControl";
            }
        };
        MenuAccessControl menuAccessControl = defaultInstantiator
                .getMenuAccessControl();
        Assert.assertNotNull(menuAccessControl);
        Assert.assertTrue(menuAccessControl instanceof CustomMenuAccessControl);
        Assert.assertSame(menuAccessControl.getPopulateClientSideMenu(),
                MenuAccessControl.PopulateClientMenu.ALWAYS);
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
