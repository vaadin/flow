/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
