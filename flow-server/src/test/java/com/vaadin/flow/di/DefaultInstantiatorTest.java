/*
 * Copyright 2000-2023 Vaadin Ltd.
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
package com.vaadin.flow.di;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;

public class DefaultInstantiatorTest {

    @Tag(Tag.A)
    public static class TestComponent extends Component {

    }

    @Test
    public void createComponent_dontDependOnGetOrCreate() {
        DefaultInstantiator instantiator = Mockito
                .mock(DefaultInstantiator.class);

        Mockito.when(instantiator.createComponent(Mockito.any()))
                .thenCallRealMethod();

        TestComponent component = instantiator
                .createComponent(TestComponent.class);

        Assert.assertNotNull(component);

        Mockito.verify(instantiator, Mockito.times(0))
                .getOrCreate(Mockito.any());
    }

    @Test
    public void getOrCreate_lookupHasObject_returnObjectFromLookup() {
        VaadinService service = Mockito.mock(VaadinService.class);
        Lookup lookup = mockLookup(service);

        DefaultInstantiator instantiator = new DefaultInstantiator(service);

        Mockito.when(lookup.lookup(List.class)).thenReturn(new ArrayList<>());

        List<?> list = instantiator.getOrCreate(List.class);
        Assert.assertTrue(list instanceof ArrayList);
    }

    @Test
    public void getOrCreate_lookupHasNoObject_createNewObject() {
        VaadinService service = Mockito.mock(VaadinService.class);
        mockLookup(service);

        DefaultInstantiator instantiator = new DefaultInstantiator(service);

        TestComponent component = instantiator.getOrCreate(TestComponent.class);
        Assert.assertNotNull(component);
    }

    private Lookup mockLookup(VaadinService service) {
        VaadinContext context = Mockito.mock(VaadinContext.class);
        Mockito.when(service.getContext()).thenReturn(context);

        Lookup lookup = Mockito.mock(Lookup.class);
        Mockito.when(context.getAttribute(Lookup.class)).thenReturn(lookup);
        return lookup;
    }
}
