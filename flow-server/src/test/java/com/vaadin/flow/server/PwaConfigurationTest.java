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
package com.vaadin.flow.server;

import org.junit.Assert;
import org.junit.Test;

public class PwaConfigurationTest {
    @Test
    // For https://github.com/vaadin/flow/issues/10148
    public void pwaDefaultStartUrl_should_BeDotInsteadOfEmptyString() {
        PwaConfiguration pwaConfiguration = new PwaConfiguration();
        Assert.assertEquals(PwaConfiguration.DEFAULT_START_URL,
                pwaConfiguration.getStartUrl());
    }

    @PWA(name = "name", shortName = "shortName")
    static class App {
    }

    @Test
    public void pwaOfflinePathEmpty_should_beDisabled() {
        PwaConfiguration pwaConfiguration = new PwaConfiguration(
                App.class.getAnnotation(PWA.class));
        Assert.assertFalse(pwaConfiguration.isOfflinePathEnabled());
        Assert.assertEquals("", pwaConfiguration.getOfflinePath());
    }
}
