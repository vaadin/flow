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
package com.vaadin.flow.server.startup;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import com.vaadin.experimental.CoreFeatureFlagProvider;
import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.MockVaadinContext;
import com.vaadin.flow.server.VaadinContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationConfigurationTest {

    @TempDir
    Path tempDir;

    private MockVaadinContext context;
    private File frontendFolder;
    private Map<String, String> properties;

    @BeforeEach
    void before() throws Exception {
        context = new MockVaadinContext();
        frontendFolder = Files.createDirectory(tempDir.resolve("frontend"))
                .toFile();
        properties = new HashMap<>();
        // FeatureFlags.setEnabled requires an ApplicationConfiguration in
        // the context and dev mode.
        ApplicationConfiguration confForFlags = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(confForFlags.isProductionMode()).thenReturn(false);
        context.setAttribute(ApplicationConfiguration.class, confForFlags);
        File flagsDir = Files.createDirectory(tempDir.resolve("flags"))
                .toFile();
        FeatureFlags.get(context).setPropertiesLocation(flagsDir);
    }

    @AfterEach
    void after() {
        FeatureFlags.get(context).setEnabled(
                CoreFeatureFlagProvider.TAILWIND_CSS.getId(), false);
    }

    @Test
    void get_contextHasNoLookup_iseIsThrown() {
        assertThrows(IllegalStateException.class, () -> {
            VaadinContext context = Mockito.spy(VaadinContext.class);
            Mockito.when(context.getAttribute(Lookup.class)).thenReturn(null);
            Mockito.doAnswer(invocation -> invocation
                    .getArgument(1, Supplier.class).get()).when(context)
                    .getAttribute(Mockito.any(), Mockito.any());
            ApplicationConfiguration.get(context);
        });
    }

    @Test
    void modeReason_hotdeployExplicitlyTrue() {
        properties.put(InitParameters.FRONTEND_HOTDEPLOY, "true");
        assertEquals(
                "the '" + InitParameters.FRONTEND_HOTDEPLOY
                        + "' configuration parameter is set to true",
                newConfiguration().getModeReason());
    }

    @Test
    void modeReason_hotdeployExplicitlyFalse() {
        properties.put(InitParameters.FRONTEND_HOTDEPLOY, "false");
        assertEquals(
                "the '" + InitParameters.FRONTEND_HOTDEPLOY
                        + "' configuration parameter is set to false",
                newConfiguration().getModeReason());
    }

    @Test
    void modeReason_tailwindForcesHotdeploy() {
        FeatureFlags.get(context)
                .setEnabled(CoreFeatureFlagProvider.TAILWIND_CSS.getId(), true);
        String reason = newConfiguration().getModeReason();
        assertTrue(
                reason.contains(CoreFeatureFlagProvider.TAILWIND_CSS.getId()),
                "Reason should mention the Tailwind feature flag, was: "
                        + reason);
    }

    @Test
    void modeReason_productionMode() {
        TestConfiguration config = newConfiguration();
        config.productionMode = true;
        assertTrue(config.getModeReason().contains("production mode"),
                "Reason should mention production mode, was: "
                        + config.getModeReason());
    }

    @Test
    void modeReason_defaultNoFeatures() {
        // No Hilla, no Tailwind, hotdeploy not set
        assertEquals(
                "no Hilla views, Tailwind, or '"
                        + InitParameters.FRONTEND_HOTDEPLOY
                        + "' configuration parameter were detected",
                newConfiguration().getModeReason());
    }

    private TestConfiguration newConfiguration() {
        return new TestConfiguration();
    }

    private final class TestConfiguration implements ApplicationConfiguration {
        boolean productionMode;

        @Override
        public boolean isProductionMode() {
            return productionMode;
        }

        @Override
        public String getStringProperty(String name, String defaultValue) {
            return properties.getOrDefault(name, defaultValue);
        }

        @Override
        public boolean getBooleanProperty(String name, boolean defaultValue) {
            String v = properties.get(name);
            return v == null ? defaultValue : Boolean.parseBoolean(v);
        }

        @Override
        public File getFrontendFolder() {
            return frontendFolder;
        }

        @Override
        public Enumeration<String> getPropertyNames() {
            return Collections.enumeration(properties.keySet());
        }

        @Override
        public VaadinContext getContext() {
            return context;
        }

        @Override
        public boolean isDevModeSessionSerializationEnabled() {
            return false;
        }
    }
}
