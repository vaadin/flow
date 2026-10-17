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
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultInstantiatorTest {

    @Tag(Tag.A)
    public static class TestComponent extends Component {

    }

    @Test
    void createComponent_dontDependOnGetOrCreate() {
        DefaultInstantiator instantiator = Mockito
                .mock(DefaultInstantiator.class);

        when(instantiator.createComponent(any())).thenCallRealMethod();

        TestComponent component = instantiator
                .createComponent(TestComponent.class);

        assertNotNull(component);

        verify(instantiator, times(0)).getOrCreate(any());
    }

    @Test
    void getOrCreate_lookupHasObject_returnObjectFromLookup() {
        VaadinService service = mock(VaadinService.class);
        Lookup lookup = mockLookup(service);

        DefaultInstantiator instantiator = new DefaultInstantiator(service);

        when(lookup.lookup(List.class)).thenReturn(new ArrayList<>());

        List<?> list = instantiator.getOrCreate(List.class);
        assertTrue(list instanceof ArrayList);
    }

    @Test
    void getOrCreate_lookupHasNoObject_createNewObject() {
        VaadinService service = mock(VaadinService.class);
        mockLookup(service);

        DefaultInstantiator instantiator = new DefaultInstantiator(service);

        TestComponent component = instantiator.getOrCreate(TestComponent.class);
        assertNotNull(component);
    }

    @Test
    void getApplicationClass_regularClass_getsSameClass() {
        VaadinService service = mock(VaadinService.class);
        mockLookup(service);

        DefaultInstantiator instantiator = new DefaultInstantiator(service);

        TestComponent instance = instantiator.getOrCreate(TestComponent.class);
        assertSame(TestComponent.class,
                instantiator.getApplicationClass(instance));
        assertSame(TestComponent.class,
                instantiator.getApplicationClass(instance.getClass()));
    }

    @Test
    void getApplicationClass_syntheticClass_getsApplicationClass()
            throws Exception {
        VaadinService service = mock(VaadinService.class);
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

        assertNotSame(TestComponent.class, instance.getClass());
        assertSame(TestComponent.class,
                instantiator.getApplicationClass(instance));
        assertSame(TestComponent.class,
                instantiator.getApplicationClass(instance.getClass()));
    }

    private Lookup mockLookup(VaadinService service) {
        VaadinContext context = mock(VaadinContext.class);
        when(service.getContext()).thenReturn(context);

        Lookup lookup = mock(Lookup.class);
        when(context.getAttribute(Lookup.class)).thenReturn(lookup);
        return lookup;
    }
}
