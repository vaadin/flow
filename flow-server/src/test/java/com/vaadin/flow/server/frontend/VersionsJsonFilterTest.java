/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import elemental.json.Json;
import elemental.json.JsonObject;

public class VersionsJsonFilterTest {

    @Test
    public void filterPlatformVersions_dependencies() throws IOException {
        assertFilterPlatformVersions(NodeUpdater.DEPENDENCIES);
    }

    @Test
    public void filterPlatformVersions_devDependencies() throws IOException {
        assertFilterPlatformVersions(NodeUpdater.DEV_DEPENDENCIES);
    }

    @Test
    public void filterPlatformDependenciesVersions_multipleUserChanged_correctlyIgnored()
            throws IOException {
        assertFilterPlatformVersions_multipleUserChanged_correctlyIgnored(
                NodeUpdater.DEPENDENCIES);
    }

    @Test
    public void filterPlatformDevDependenciesVersions_multipleUserChanged_correctlyIgnored()
            throws IOException {
        assertFilterPlatformVersions_multipleUserChanged_correctlyIgnored(
                NodeUpdater.DEV_DEPENDENCIES);
    }

    @Test
    public void missingVaadinDependencies_allDependenciesSholdBeUserHandled()
            throws IOException {
        assertMissingVaadinDependencies_allDependenciesSholdBeUserHandled(
                NodeUpdater.DEPENDENCIES);
    }

    @Test
    public void missingVaadinDevDependencies_allDependenciesSholdBeUserHandled()
            throws IOException {
        assertMissingVaadinDependencies_allDependenciesSholdBeUserHandled(
                NodeUpdater.DEV_DEPENDENCIES);
    }

    private void assertMissingVaadinDependencies_allDependenciesSholdBeUserHandled(
            String depKey) throws IOException {
        String versions = IOUtils
                .toString(
                        getClass().getClassLoader()
                                .getResourceAsStream("versions/versions.json"),
                        StandardCharsets.UTF_8);
        String pkgJson = IOUtils.toString(
                getClass().getClassLoader()
                        .getResourceAsStream("versions/no_vaadin_package.json"),
                StandardCharsets.UTF_8);

        VersionsJsonFilter filter = new VersionsJsonFilter(Json.parse(pkgJson),
                depKey);
        JsonObject filteredJson = filter
                .getFilteredVersions(Json.parse(versions));
        Assert.assertTrue(filteredJson.hasKey("@vaadin/vaadin-progress-bar"));
        Assert.assertTrue(filteredJson.hasKey("@vaadin/vaadin-upload"));
        Assert.assertTrue(filteredJson.hasKey("@polymer/iron-list"));

        Assert.assertEquals("1.1.2",
                filteredJson.getString("@vaadin/vaadin-progress-bar"));
    }

    private void assertFilterPlatformVersions_multipleUserChanged_correctlyIgnored(
            String depKey) throws IOException {
        String versions = IOUtils
                .toString(
                        getClass().getClassLoader().getResourceAsStream(
                                "versions/user_versions.json"),
                        StandardCharsets.UTF_8);
        String pkgJson = IOUtils
                .toString(
                        getClass().getClassLoader().getResourceAsStream(
                                "versions/user_package.json"),
                        StandardCharsets.UTF_8);

        VersionsJsonFilter filter = new VersionsJsonFilter(Json.parse(pkgJson),
                depKey);
        JsonObject filteredJson = filter
                .getFilteredVersions(Json.parse(versions));
        List<String> expectedKeys = Arrays.asList("@vaadin/vaadin-notification",
                "@vaadin/vaadin-overlay", "@vaadin/vaadin-select",
                "@vaadin/vaadin-split-layout", "@vaadin/vaadin-tabs");

        for (String key : expectedKeys) {
            Assert.assertTrue(
                    String.format("Key '%s' was expected, but not found", key),
                    filteredJson.hasKey(key));
        }

        List<String> droppedKeys = Arrays.asList("flow", "core", "platform");
        for (String key : droppedKeys) {
            Assert.assertFalse(
                    String.format("User managed key '%s' was found.", key),
                    filteredJson.hasKey(key));
        }

        Map<String, String> expectedValues = new HashMap<>();
        expectedValues.put("@vaadin/vaadin-notification", "1.4.0");
        expectedValues.put("@vaadin/vaadin-overlay", "3.2.19");
        expectedValues.put("@vaadin/vaadin-select", "2.1.7");
        expectedValues.put("@vaadin/vaadin-split-layout", "4.1.1");
        expectedValues.put("@vaadin/vaadin-tabs", "3.0.5");

        for (Map.Entry<String, String> entry : expectedValues.entrySet()) {
            Assert.assertEquals(
                    String.format("Got wrong version for '%s'", entry.getKey()),
                    entry.getValue(), filteredJson.getString(entry.getKey()));
        }
    }

    private void assertFilterPlatformVersions(String depKey)
            throws IOException {
        String versions = IOUtils
                .toString(
                        getClass().getClassLoader()
                                .getResourceAsStream("versions/versions.json"),
                        StandardCharsets.UTF_8);
        String pkgJson = IOUtils
                .toString(
                        getClass().getClassLoader()
                                .getResourceAsStream("versions/package.json"),
                        StandardCharsets.UTF_8);

        VersionsJsonFilter filter = new VersionsJsonFilter(Json.parse(pkgJson),
                depKey);
        JsonObject filteredJson = filter
                .getFilteredVersions(Json.parse(versions));
        Assert.assertTrue(filteredJson.hasKey("@vaadin/vaadin-progress-bar"));
        Assert.assertTrue(filteredJson.hasKey("@vaadin/vaadin-upload"));
        Assert.assertTrue(filteredJson.hasKey("@polymer/iron-list"));

        Assert.assertEquals("1.1.2",
                filteredJson.getString("@vaadin/vaadin-progress-bar"));
        Assert.assertEquals("4.2.2",
                filteredJson.getString("@vaadin/vaadin-upload"));
        Assert.assertEquals("2.0.19",
                filteredJson.getString("@polymer/iron-list"));
    }
}
