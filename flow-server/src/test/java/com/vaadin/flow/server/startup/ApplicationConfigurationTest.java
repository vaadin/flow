/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.startup;

import java.util.function.Supplier;

import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.VaadinContext;

public class ApplicationConfigurationTest {

    @Test(expected = IllegalStateException.class)
    public void get_contextHasNoLookup_iseIsThrown() {
        VaadinContext context = Mockito.spy(VaadinContext.class);
        Mockito.when(context.getAttribute(Lookup.class)).thenReturn(null);
        Mockito.doAnswer(
                invocation -> invocation.getArgument(1, Supplier.class).get())
                .when(context).getAttribute(Mockito.any(), Mockito.any());
        ApplicationConfiguration.get(context);
    }

}
