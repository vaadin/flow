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
package com.vaadin.flow.component.dnd;

import java.util.Collections;
import java.util.Properties;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

abstract class AbstractDnDUnitTest {

    protected MockUI ui;
    protected boolean compatibilityMode;

    @BeforeEach
    void setup() {
        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        when(appConfig.getPropertyNames())
                .thenReturn(Collections.emptyEnumeration());
        when(appConfig.getBuildFolder()).thenReturn(".");
        VaadinContext context = mock(VaadinContext.class);
        when(appConfig.getContext()).thenReturn(context);

        Lookup lookup = mock(Lookup.class);
        when(context.getAttribute(Lookup.class)).thenReturn(lookup);
        when(context.getAttribute(any(Class.class), any(Supplier.class)))
                .then(i -> i.getArgument(1, Supplier.class).get());

        DefaultDeploymentConfiguration configuration = new DefaultDeploymentConfiguration(
                appConfig, VaadinServlet.class, new Properties());

        VaadinService service = mock(VaadinService.class);
        when(service.resolveResource(anyString())).thenReturn("");

        VaadinSession session = mock(VaadinSession.class);
        when(session.getConfiguration()).thenReturn(configuration);
        when(session.getService()).thenReturn(service);

        ui = new MockUI(session);
    }

    @Test
    void testExtension_activated_usageStatisticsEntryAdded() {
        runStaticCreateMethodForExtension(new RouterLink());

        assertTrue(
                UsageStatistics.getEntries().anyMatch(
                        entry -> entry.getName().contains("generic-dnd")),
                "No usage statistics for generic dnd reported");
    }

    protected abstract void runStaticCreateMethodForExtension(
            Component component);
}
