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
package com.vaadin.flow.micrometer;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.MockServletServiceSessionSetup;
import com.vaadin.tests.util.MockUI;

/**
 * Drives real UI initialization through a Flow service and verifies the
 * {@code vaadin.ui.*} meters move.
 */
public class UiLifecycleTest {

    private MockServletServiceSessionSetup setup;
    private SimpleMeterRegistry registry;

    @Before
    public void setUp() throws Exception {
        registry = new SimpleMeterRegistry();
        VaadinMicrometer.install(registry, VaadinMetricsConfig.defaults());
        setup = new MockServletServiceSessionSetup();
    }

    @After
    public void tearDown() {
        VaadinMicrometer.uninstall();
        setup.cleanup();
        CurrentInstance.clearAll();
    }

    @Test
    public void uiInitIncrementsCreatedCounterAndActiveGauge() {
        UI ui = new MockUI(setup.getSession());

        setup.getService().fireUIInitListeners(ui);

        Assert.assertEquals(1.0,
                registry.counter(MeterNames.UI_CREATED).count(), 0.0);
        Assert.assertEquals(1.0,
                registry.find(MeterNames.UI_ACTIVE).gauge().value(), 0.0);
    }

    @Test
    public void multipleUiInitsAccumulate() {
        for (int i = 0; i < 3; i++) {
            setup.getService()
                    .fireUIInitListeners(new MockUI(setup.getSession()));
        }

        Assert.assertEquals(3.0,
                registry.counter(MeterNames.UI_CREATED).count(), 0.0);
        Assert.assertEquals(3.0,
                registry.find(MeterNames.UI_ACTIVE).gauge().value(), 0.0);
    }
}
