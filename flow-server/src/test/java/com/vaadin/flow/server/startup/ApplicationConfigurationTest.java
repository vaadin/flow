/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.flow.server.startup;

import java.util.function.Supplier;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ApplicationConfigurationTest {

    @Before
    public void before() {
        CurrentInstance.clearAll();
    }

    @After
    public void after() {
        CurrentInstance.clearAll();
    }

    @Test(expected = IllegalStateException.class)
    public void get_contextHasNoLookup_iseIsThrown() {
        VaadinContext context = Mockito.spy(VaadinContext.class);
        Mockito.when(context.getAttribute(Lookup.class)).thenReturn(null);
        Mockito.doAnswer(
                invocation -> invocation.getArgument(1, Supplier.class).get())
                .when(context).getAttribute(Mockito.any(), Mockito.any());
        ApplicationConfiguration.get(context);
    }

    @Test
    public void getCurrentUsesCurrentService() {
        ApplicationConfiguration applicationConfiguration = Mockito
                .mock(ApplicationConfiguration.class);

        ApplicationConfigurationFactory applicationConfigurationFactory = Mockito
                .mock(ApplicationConfigurationFactory.class);
        Mockito.when(applicationConfigurationFactory.create(Mockito.any()))
                .thenReturn(applicationConfiguration);
        VaadinService service = Mockito.mock(VaadinService.class);
        VaadinContext context = Mockito.mock(VaadinContext.class);
        Mockito.when(service.getContext()).thenReturn(context);
        Lookup lookup = Mockito.mock(Lookup.class);
        Mockito.when(context.getAttribute(Lookup.class)).thenReturn(lookup);
        Mockito.when(lookup.lookup(ApplicationConfigurationFactory.class))
                .thenReturn(applicationConfigurationFactory);
        Mockito.doAnswer(
                invocation -> invocation.getArgument(1, Supplier.class).get())
                .when(context).getAttribute(Mockito.any(), Mockito.any());

        VaadinService.setCurrent(service);
        Assert.assertSame(applicationConfiguration,
                ApplicationConfiguration.getCurrent());

    }
}
