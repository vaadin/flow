/*
 * Copyright 2000-2023 Vaadin Ltd.
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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import elemental.json.Json;
import elemental.json.JsonObject;

public class PlatformVersionTest {

    private static String VAADIN_VERSION = "1.2.3";
    ClassLoader classLoader;
    File vaadinCore;

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Before
    public void init() throws IOException {
        classLoader = Mockito.mock(ClassLoader.class);
        File vaadinFolder = temporary.newFolder("com", "vaadin",
                "vaadin-core-internal", VAADIN_VERSION);
        vaadinCore = new File(vaadinFolder,
                Constants.VAADIN_CORE_VERSIONS_JSON);
        JsonObject content = Json.createObject();
        content.put("platform", VAADIN_VERSION);
        FileUtils.write(vaadinCore, content.toJson());
    }

    @Test
    public void getVaadinVersion_singleJson_correctVersionIsReturned()
            throws IOException {
        assertContents(VAADIN_VERSION, vaadinCore.toURL());
    }

    @Test
    public void getVaadinVersion_multipleJson_correctVersionIsReturned()
            throws IOException {
        String HILLA_VERSION = "3.2.1";
        File hillaFolder = temporary.newFolder("dev", "hilla", "hilla",
                HILLA_VERSION);
        File hillaCore = new File(hillaFolder,
                Constants.VAADIN_CORE_VERSIONS_JSON);
        JsonObject content = Json.createObject();
        content.put("platform", HILLA_VERSION);
        FileUtils.write(hillaCore, content.toJson());
        assertContents(VAADIN_VERSION, vaadinCore.toURL(), hillaCore.toURL());
    }

    private void assertContents(String version, URL... urls)
            throws IOException {
        Mockito.when(
                classLoader.getResources(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(Collections.enumeration(Arrays.asList(urls)));
        try (MockedStatic<Platform> platform = Mockito
                .mockStatic(Platform.class)) {
            platform.when(Platform::getClassloader).thenReturn(classLoader);
            platform.when(Platform::getVaadinVersion).thenCallRealMethod();
            platform.when(
                    () -> Platform.loadVersionForUrl(Mockito.any(URL.class)))
                    .thenCallRealMethod();
            Optional<String> vaadinVersion = Platform.getVaadinVersion();
            Assert.assertTrue(vaadinVersion.isPresent());
            Assert.assertEquals(version, vaadinVersion.get());
        }
    }
}
