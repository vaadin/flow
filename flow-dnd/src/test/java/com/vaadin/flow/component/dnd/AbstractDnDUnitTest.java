/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import java.util.Collection;
import java.util.Properties;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.internal.DependencyList;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.DefaultDeploymentConfiguration;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.ui.Dependency;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public abstract class AbstractDnDUnitTest {

    protected MockUI ui;
    protected boolean compatibilityMode;

    @Before
    public void setup() {
        DefaultDeploymentConfiguration configuration = new DefaultDeploymentConfiguration(
                VaadinServlet.class, new Properties()) {
            @Override
            public boolean isCompatibilityMode() {
                return compatibilityMode;
            }
        };
        VaadinSession session = Mockito.mock(VaadinSession.class);
        Mockito.when(session.getConfiguration()).thenReturn(configuration);
        ui = new MockUI(session);
    }

    @Test
    public void testExtension_activated_usageStatisticsEntryAdded() {
        runStaticCreateMethodForExtension(new RouterLink());

        Assert.assertTrue("No usage statistics for generic dnd reported",
                UsageStatistics.getEntries().anyMatch(
                        entry -> entry.getName().contains("generic-dnd")));
    }

    @Test
    public void testExtension_staticApiInCompatibilityMode_connectorDependencyAddedDynamically() {
        compatibilityMode = true;
        ui.getInternals().getDependencyList().clearPendingSendToClient();

        RouterLink component = new RouterLink();
        ui.add(component);
        runStaticCreateMethodForExtension(component);

        DependencyList dependencyList = ui.getInternals().getDependencyList();
        Collection<Dependency> pendingSendToClient = dependencyList
                .getPendingSendToClient();

        Assert.assertEquals("No dependency added", 1,
                pendingSendToClient.size());

        Dependency dependency = pendingSendToClient.iterator().next();
        Assert.assertEquals("Wrong dependency loaded",
                "frontend://dndConnector.js", dependency.getUrl());
    }

    @Test
    public void testExtension_staticApi_connectorDependencyNotAddedDynamically() {
        ui.getInternals().getDependencyList().clearPendingSendToClient();

        RouterLink component = new RouterLink();
        ui.add(component);
        runStaticCreateMethodForExtension(component);

        DependencyList dependencyList = ui.getInternals().getDependencyList();
        Collection<Dependency> pendingSendToClient = dependencyList
                .getPendingSendToClient();

        Assert.assertEquals("No dependency added", 0,
                pendingSendToClient.size());
    }

    protected abstract void runStaticCreateMethodForExtension(
            Component component);
}
