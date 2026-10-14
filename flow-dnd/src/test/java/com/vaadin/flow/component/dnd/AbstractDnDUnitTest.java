/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.dnd;

import java.util.Collections;
import java.util.Properties;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.DefaultDeploymentConfiguration;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

public abstract class AbstractDnDUnitTest {

    protected MockUI ui;
    protected boolean compatibilityMode;

    @Before
    public void setup() {
        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(appConfig.getPropertyNames())
                .thenReturn(Collections.emptyEnumeration());
        Mockito.when(appConfig.getBuildFolder()).thenReturn(".");
        VaadinContext context = Mockito.mock(VaadinContext.class);
        Mockito.when(appConfig.getContext()).thenReturn(context);

        Lookup lookup = Mockito.mock(Lookup.class);
        Mockito.when(context.getAttribute(Lookup.class)).thenReturn(lookup);
        Mockito.when(context.getAttribute(ArgumentMatchers.any(Class.class),
                ArgumentMatchers.any(Supplier.class)))
                .then(i -> i.getArgument(1, Supplier.class).get());

        DefaultDeploymentConfiguration configuration = new DefaultDeploymentConfiguration(
                appConfig, VaadinServlet.class, new Properties());

        VaadinService service = Mockito.mock(VaadinService.class);
        Mockito.when(service.resolveResource(Mockito.anyString()))
                .thenReturn("");

        VaadinSession session = Mockito.mock(VaadinSession.class);
        Mockito.when(session.getConfiguration()).thenReturn(configuration);
        Mockito.when(session.getService()).thenReturn(service);

        ui = new MockUI(session);
    }

    @Test
    public void testExtension_activated_usageStatisticsEntryAdded() {
        runStaticCreateMethodForExtension(new RouterLink());

        Assert.assertTrue("No usage statistics for generic dnd reported",
                UsageStatistics.getEntries().anyMatch(
                        entry -> entry.getName().contains("generic-dnd")));
    }

    protected abstract void runStaticCreateMethodForExtension(
            Component component);
}
