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
package com.vaadin.flow.di;

import java.util.ArrayList;
import java.util.List;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.SyntheticState;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;

class DefaultInstantiatorTest {

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

        Assertions.assertNotNull(component);

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
        Assertions.assertTrue(list instanceof ArrayList);
    }

    @Test
    public void getOrCreate_lookupHasNoObject_createNewObject() {
        VaadinService service = Mockito.mock(VaadinService.class);
        mockLookup(service);

        DefaultInstantiator instantiator = new DefaultInstantiator(service);

        TestComponent component = instantiator.getOrCreate(TestComponent.class);
        Assertions.assertNotNull(component);
    }

    @Test
    public void getApplicationClass_regularClass_getsSameClass() {
        VaadinService service = Mockito.mock(VaadinService.class);
        mockLookup(service);

        DefaultInstantiator instantiator = new DefaultInstantiator(service);

        TestComponent instance = instantiator.getOrCreate(TestComponent.class);
        Assertions.assertSame(TestComponent.class,
                instantiator.getApplicationClass(instance));
        Assertions.assertSame(TestComponent.class,
                instantiator.getApplicationClass(instance.getClass()));
    }

    @Test
    public void getApplicationClass_syntheticClass_getsApplicationClass()
            throws Exception {
        VaadinService service = Mockito.mock(VaadinService.class);
        mockLookup(service);
        DefaultInstantiator instantiator = new DefaultInstantiator(service);

        Class<? extends TestComponent> syntheticClass = new ByteBuddy()
                .subclass(TestComponent.class)
                .modifiers(Visibility.PUBLIC, SyntheticState.SYNTHETIC).make()
                .load(getClass().getClassLoader(),
                        ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        TestComponent instance = syntheticClass.getDeclaredConstructor()
                .newInstance();

        Assertions.assertNotSame(TestComponent.class, instance.getClass());
        Assertions.assertSame(TestComponent.class,
                instantiator.getApplicationClass(instance));
        Assertions.assertSame(TestComponent.class,
                instantiator.getApplicationClass(instance.getClass()));
    }

    private Lookup mockLookup(VaadinService service) {
        VaadinContext context = Mockito.mock(VaadinContext.class);
        Mockito.when(service.getContext()).thenReturn(context);

        Lookup lookup = Mockito.mock(Lookup.class);
        Mockito.when(context.getAttribute(Lookup.class)).thenReturn(lookup);
        return lookup;
    }
}
