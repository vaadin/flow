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
import org.junit.Test;

import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.MockServletServiceSessionSetup;

/**
 * Drives a real {@link com.vaadin.flow.server.VaadinService#init()} through
 * {@link MockServletServiceSessionSetup} so that the
 * {@link MetricsServiceInitListener} is discovered via the standard Java
 * {@link java.util.ServiceLoader}. Verifies the binders register their meters
 * eagerly when {@link VaadinMicrometer#install} has been called.
 */
public class ServiceLoaderRegistrationTest {

    private MockServletServiceSessionSetup setup;

    @After
    public void tearDown() throws Exception {
        VaadinMicrometer.uninstall();
        if (setup != null) {
            setup.cleanup();
        }
        CurrentInstance.clearAll();
    }

    @Test
    public void installedRegistryGetsBindersRegisteredDuringServiceInit()
            throws Exception {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        VaadinMicrometer.install(registry, VaadinMetricsConfig.defaults());

        setup = new MockServletServiceSessionSetup();

        Assert.assertNotNull(
                "sessions.active gauge should be eagerly registered",
                registry.find(MeterNames.SESSIONS_ACTIVE).gauge());
        Assert.assertNotNull(
                "sessions.created counter should be eagerly registered",
                registry.find(MeterNames.SESSIONS_CREATED).counter());
        Assert.assertNotNull(
                "sessions.duration timer should be eagerly registered",
                registry.find(MeterNames.SESSIONS_DURATION).timer());
        Assert.assertNotNull("ui.active gauge should be eagerly registered",
                registry.find(MeterNames.UI_ACTIVE).gauge());
        Assert.assertNotNull("ui.created counter should be eagerly registered",
                registry.find(MeterNames.UI_CREATED).counter());
    }

    @Test
    public void uninstalledMeansNoMetersAreRegistered() throws Exception {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        // intentionally NOT installing

        setup = new MockServletServiceSessionSetup();

        Assert.assertNull("no meters should appear when uninstalled",
                registry.find(MeterNames.SESSIONS_ACTIVE).gauge());
        Assert.assertNull(registry.find(MeterNames.UI_ACTIVE).gauge());
    }
}
