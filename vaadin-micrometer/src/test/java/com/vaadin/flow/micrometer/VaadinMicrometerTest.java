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

public class VaadinMicrometerTest {

    @After
    public void tearDown() {
        VaadinMicrometer.uninstall();
    }

    @Test
    public void uninstalledByDefault() {
        Assert.assertNull(VaadinMicrometer.registry());
        Assert.assertNull(VaadinMicrometer.config());
    }

    @Test
    public void installStoresRegistryAndConfig() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        VaadinMetricsConfig config = VaadinMetricsConfig.defaults();

        VaadinMicrometer.install(registry, config);

        Assert.assertSame(registry, VaadinMicrometer.registry());
        Assert.assertSame(config, VaadinMicrometer.config());
    }

    @Test
    public void uninstallClearsState() {
        VaadinMicrometer.install(new SimpleMeterRegistry(),
                VaadinMetricsConfig.defaults());
        VaadinMicrometer.uninstall();

        Assert.assertNull(VaadinMicrometer.registry());
        Assert.assertNull(VaadinMicrometer.config());
    }

    @Test(expected = NullPointerException.class)
    public void installRejectsNullRegistry() {
        VaadinMicrometer.install(null, VaadinMetricsConfig.defaults());
    }

    @Test(expected = NullPointerException.class)
    public void installRejectsNullConfig() {
        VaadinMicrometer.install(new SimpleMeterRegistry(), null);
    }
}
