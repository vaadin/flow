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
package com.vaadin.flow.server.frontend;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import com.vaadin.experimental.Feature;
import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.MockVaadinContext;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import static com.vaadin.flow.internal.FrontendUtils.FRONTEND;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskGenerateFeatureFlagsTest {

    @TempDir
    File temporaryFolder;

    private TaskGenerateFeatureFlags taskGenerateFeatureFlags;
    private FeatureFlags featureFlags;

    @BeforeEach
    void setUp() throws Exception {
        VaadinContext context = new MockVaadinContext();
        ApplicationConfiguration configuration = Mockito
                .mock(ApplicationConfiguration.class);
        context.setAttribute(ApplicationConfiguration.class, configuration);

        File frontendFolder = new File(temporaryFolder, FRONTEND);

        frontendFolder.mkdirs();
        featureFlags = FeatureFlags.get(context);
        Options options = new Options(Mockito.mock(Lookup.class), null)
                .withFrontendDirectory(frontendFolder)
                .withFeatureFlags(featureFlags);
        taskGenerateFeatureFlags = new TaskGenerateFeatureFlags(options);
    }

    @Test
    void should_disableTypeChecksForGlobals() throws ExecutionFailedException {
        taskGenerateFeatureFlags.execute();
        String content = taskGenerateFeatureFlags.getFileContent();
        assertTrue(content.startsWith("// @ts-nocheck"));
    }

    @Test
    void should_setupFeatureFlagsGlobal() throws ExecutionFailedException {
        taskGenerateFeatureFlags.execute();
        String content = taskGenerateFeatureFlags.getFileContent();
        assertTrue(content.contains("window.Vaadin = window.Vaadin || {};"));
        assertTrue(content.contains(
                "window.Vaadin.featureFlags = window.Vaadin.featureFlags || {};"));
    }

    @Test
    void should_defineAllFeatureFlags() throws ExecutionFailedException {
        taskGenerateFeatureFlags.execute();
        String content = taskGenerateFeatureFlags.getFileContent();

        featureFlags.getFeatures().forEach(feature -> {
            assertFeatureFlagGlobal(content, feature, false);
        });
    }

    @Test
    void should_callFeatureFlagsUpdaterFunction()
            throws ExecutionFailedException {
        taskGenerateFeatureFlags.execute();
        String content = taskGenerateFeatureFlags.getFileContent();
        assertTrue(content.contains(
                "window.Vaadin.featureFlagsUpdaters.forEach(updater => updater(activator))"));
    }

    @Test
    void should_containEmptyExport() throws ExecutionFailedException {
        taskGenerateFeatureFlags.execute();
        String content = taskGenerateFeatureFlags.getFileContent();
        assertTrue(content.contains("export {};"));
    }

    private static void assertFeatureFlagGlobal(String content, Feature feature,
            boolean enabled) {
        assertTrue(content
                .contains(String.format("window.Vaadin.featureFlags.%s = %s",
                        feature.getId(), enabled)));
    }
}
