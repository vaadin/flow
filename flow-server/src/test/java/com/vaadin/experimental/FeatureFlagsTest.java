/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.experimental;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class FeatureFlagsTest {

    @Before
    public void before() {
        CurrentInstance.clearAll();
        FeatureFlags.loadProperties(null); // Reset all features to false
    }

    @After
    public void after() {
        CurrentInstance.clearAll();
    }

    @Test
    public void propertiesLoaded() throws IOException {
        Assert.assertFalse(FeatureFlags.isEnabled(FeatureFlags.EXAMPLE));

        File folder = createTempFeatureFlagsFile(
                "com.vaadin.experimental.exampleFeatureFlag=true\n");

        mockResourcesLocation(folder);

        // This is done automatically but from a static block which we cannot
        // mock
        FeatureFlags.loadProperties();

        Assert.assertTrue(FeatureFlags.isEnabled(FeatureFlags.EXAMPLE));
    }

    private void mockResourcesLocation(File folder) {
        VaadinService service = Mockito.mock(VaadinService.class);
        VaadinService.setCurrent(service);
        VaadinContext vaadinContext = Mockito.mock(VaadinContext.class);
        Mockito.when(service.getContext()).thenReturn(vaadinContext);

        ApplicationConfiguration applicationConfiguration = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(vaadinContext.getAttribute(
                Mockito.eq(ApplicationConfiguration.class), Mockito.any()))
                .thenReturn(applicationConfiguration);
        Mockito.when(applicationConfiguration.getStringProperty(
                Constants.JAVA_RESOURCE_FOLDER_TOKEN, "src/main/resources"))
                .thenReturn(folder.getAbsolutePath());

    }

    @Test
    public void setPropertiesLocation() throws Exception {
        // Set location and ensure flags are loaded from there
        File folder = createTempFeatureFlagsFile(
                "com.vaadin.experimental.exampleFeatureFlag=true\n");
        Assert.assertFalse(FeatureFlags.isEnabled(FeatureFlags.EXAMPLE));
        FeatureFlags.setPropertiesLocation(folder);
        Assert.assertTrue(FeatureFlags.isEnabled(FeatureFlags.EXAMPLE));
    }

    private File createTempFeatureFlagsFile(String data) throws IOException {
        File folder = Files.createTempDirectory("featureflags-test-folder")
                .toFile();
        folder.deleteOnExit();

        FileUtils.write(new File(folder, FeatureFlags.PROPERTIES_FILENAME),
                data, StandardCharsets.UTF_8);
        return folder;
    }

    @Test
    public void enableDisableFeature() throws IOException {
        File folder = createTempFeatureFlagsFile(
                "com.vaadin.experimental.exampleFeatureFlag=false\n");
        mockResourcesLocation(folder);
        FeatureFlags.loadProperties();
        Assert.assertFalse(FeatureFlags.isEnabled(FeatureFlags.EXAMPLE));
        FeatureFlags.setEnabled(FeatureFlags.EXAMPLE.getId(), true);
        Assert.assertTrue(FeatureFlags.isEnabled(FeatureFlags.EXAMPLE));
        Assert.assertEquals(
                "# Example feature. Will be removed once the first real feature flag is added\ncom.vaadin.experimental.exampleFeatureFlag=true\n",
                FileUtils.readFileToString(
                        new File(folder, FeatureFlags.PROPERTIES_FILENAME),
                        StandardCharsets.UTF_8));

        FeatureFlags.setEnabled(FeatureFlags.EXAMPLE.getId(), false);
        Assert.assertFalse(FeatureFlags.isEnabled(FeatureFlags.EXAMPLE));
        Assert.assertEquals("",
                FileUtils.readFileToString(
                        new File(folder, FeatureFlags.PROPERTIES_FILENAME),
                        StandardCharsets.UTF_8));
    }

}
