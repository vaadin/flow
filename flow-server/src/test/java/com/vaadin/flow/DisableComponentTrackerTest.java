/*
 * Copyright 2000-2025 Vaadin Ltd.
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

package com.vaadin.flow;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.vaadin.flow.component.internal.ComponentTracker;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

public class DisableComponentTrackerTest {
    private Object previousDisabled;
    private Field disabledField;

    @Before
    public void setup() throws Exception {
        disabledField = ComponentTracker.class.getDeclaredField("disabled");
        disabledField.setAccessible(true);
        previousDisabled = disabledField.get(null);
        disabledField.set(null, null);
    }

    @After
    public void teardown() throws Exception {
        disabledField.set(null, previousDisabled);
    }

    @Test
    public void trackCreate_disabledInProductionMode() {
        withVaadinEnvironment(appCfg -> {
            Mockito.when(appCfg.isProductionMode()).thenReturn(true);
            Mockito.when(appCfg.getBooleanProperty(ArgumentMatchers.eq(
                    InitParameters.APPLICATION_PARAMETER_DEVMODE_ENABLE_COMPONENT_TRACKER),
                    ArgumentMatchers.anyBoolean())).then(i -> i.getArgument(1));

            ComponentTrackerTest.Component1 c1 = new ComponentTrackerTest.Component1();
            Assert.assertNull(ComponentTracker.findCreate(c1));
        });
    }

    @Test
    public void trackAttach_disabledInProductionMode() {
        withVaadinEnvironment(appCfg -> {
            Mockito.when(appCfg.isProductionMode()).thenReturn(true);
            Mockito.when(appCfg.getBooleanProperty(ArgumentMatchers.eq(
                    InitParameters.APPLICATION_PARAMETER_DEVMODE_ENABLE_COMPONENT_TRACKER),
                    ArgumentMatchers.anyBoolean())).then(i -> i.getArgument(1));

            ComponentTrackerTest.Component1 c1 = new ComponentTrackerTest.Component1();
            ComponentTrackerTest.Layout layout = new ComponentTrackerTest.Layout(
                    c1);
            Assert.assertNull(ComponentTracker.findAttach(c1));
        });
    }

    @Test
    public void trackCreate_disabledByConfiguration() {
        withVaadinEnvironment(appCfg -> {
            Mockito.when(appCfg.isProductionMode()).thenReturn(false);
            Mockito.when(appCfg.getBooleanProperty(ArgumentMatchers.eq(
                    InitParameters.APPLICATION_PARAMETER_DEVMODE_ENABLE_COMPONENT_TRACKER),
                    ArgumentMatchers.anyBoolean())).thenReturn(false);

            ComponentTrackerTest.Component1 c1 = new ComponentTrackerTest.Component1();
            Assert.assertNull(ComponentTracker.findCreate(c1));
        });
    }

    @Test
    public void trackAttach_disabledByConfiguration() {
        withVaadinEnvironment(appCfg -> {
            Mockito.when(appCfg.isProductionMode()).thenReturn(false);
            Mockito.when(appCfg.getBooleanProperty(ArgumentMatchers.eq(
                    InitParameters.APPLICATION_PARAMETER_DEVMODE_ENABLE_COMPONENT_TRACKER),
                    ArgumentMatchers.anyBoolean())).thenReturn(false);

            ComponentTrackerTest.Component1 c1 = new ComponentTrackerTest.Component1();
            ComponentTrackerTest.Layout layout = new ComponentTrackerTest.Layout(
                    c1);
            Assert.assertNull(ComponentTracker.findAttach(c1));
        });
    }

    private void withVaadinEnvironment(
            Consumer<ApplicationConfiguration> action) {
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        ApplicationConfiguration applicationConfiguration = Mockito
                .mock(ApplicationConfiguration.class);
        Map<Class<?>, CurrentInstance> instances = CurrentInstance
                .getInstances();
        CurrentInstance.clearAll();
        VaadinService service = new MockVaadinServletService(configuration);
        service.getContext().setAttribute(ApplicationConfiguration.class,
                applicationConfiguration);
        try {
            CurrentInstance.set(VaadinService.class, service);
            action.accept(applicationConfiguration);
        } finally {
            CurrentInstance.restoreInstances(instances);
        }
    }
}
