/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import com.vaadin.experimental.Feature;
import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.MockVaadinContext;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.File;

import static com.vaadin.flow.server.frontend.FrontendUtils.FRONTEND;

public class TaskGenerateFeatureFlagsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private TaskGenerateFeatureFlags taskGenerateFeatureFlags;
    private FeatureFlags featureFlags;

    @Before
    public void setUp() throws Exception {
        VaadinContext context = new MockVaadinContext();
        ApplicationConfiguration configuration = Mockito
                .mock(ApplicationConfiguration.class);
        context.setAttribute(ApplicationConfiguration.class, configuration);

        File frontendFolder = temporaryFolder.newFolder(FRONTEND);
        featureFlags = FeatureFlags.get(context);
        taskGenerateFeatureFlags = new TaskGenerateFeatureFlags(frontendFolder,
                featureFlags);
    }

    @Test
    public void should_disableTypeChecksForGlobals()
            throws ExecutionFailedException {
        taskGenerateFeatureFlags.execute();
        String content = taskGenerateFeatureFlags.getFileContent();
        Assert.assertTrue(content.startsWith("// @ts-nocheck"));
    }

    @Test
    public void should_setupFeatureFlagsGlobal()
            throws ExecutionFailedException {
        taskGenerateFeatureFlags.execute();
        String content = taskGenerateFeatureFlags.getFileContent();
        Assert.assertTrue(
                content.contains("window.Vaadin = window.Vaadin || {};"));
        Assert.assertTrue(content.contains(
                "window.Vaadin.featureFlags = window.Vaadin.featureFlags || {};"));
    }

    @Test
    public void should_defineAllFeatureFlags() throws ExecutionFailedException {
        taskGenerateFeatureFlags.execute();
        String content = taskGenerateFeatureFlags.getFileContent();

        featureFlags.getFeatures().forEach(feature -> {
            assertFeatureFlagGlobal(content, feature, false);
        });
    }

    @Test
    public void should_defineCorrectEnabledValue()
            throws ExecutionFailedException {
        // Enable example feature
        featureFlags.getFeatures().stream()
                .filter(feature -> feature.equals(FeatureFlags.EXAMPLE))
                .forEach(feature -> feature.setEnabled(true));

        taskGenerateFeatureFlags.execute();
        String content = taskGenerateFeatureFlags.getFileContent();

        assertFeatureFlagGlobal(content, FeatureFlags.EXAMPLE, true);
    }

    @Test
    public void should_containEmptyExport() throws ExecutionFailedException {
        taskGenerateFeatureFlags.execute();
        String content = taskGenerateFeatureFlags.getFileContent();
        Assert.assertTrue(content.contains("export {};"));
    }

    private static void assertFeatureFlagGlobal(String content, Feature feature,
            boolean enabled) {
        Assert.assertTrue(content
                .contains(String.format("window.Vaadin.featureFlags.%s = %s",
                        feature.getId(), enabled)));
    }
}
