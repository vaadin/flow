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
package com.vaadin.flow.server.frontend;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import elemental.json.Json;
import elemental.json.JsonObject;

public class VersionsJsonFilterTest {

    private static final JsonObject TEST_VERSION_JSON;

    static {
        TEST_VERSION_JSON = Json.parse("""
                {
                    "@vaadin/vaadin-progress-bar": "1.1.2",
                    "@vaadin/vaadin-upload":  "4.2.2",
                    "@polymer/iron-list":  "2.0.19",
                    "enforced": "1.5.0",
                    "platform": "foo"
                  }

                """);
    }

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
    public void missingVaadinDependencies_allDependenciesShouldBeUserHandled()
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

    @Test
    public void testGetFilteredVersions_whenErrorHappens_versionOriginParameterIsUsedInErrorLogs()
            throws IOException {
        String pkgJson = IOUtils.toString(
                Objects.requireNonNull(
                        getClass().getClassLoader().getResourceAsStream(
                                "versions/no_vaadin_package.json")),
                StandardCharsets.UTF_8);
        JsonObject packageJson = Json.parse(pkgJson);
        VersionsJsonFilter filter = new VersionsJsonFilter(packageJson,
                NodeUpdater.DEPENDENCIES);
        String versionOrigin = "dummy-origin.json";

        Logger logger = Mockito.spy(Logger.class);
        try (MockedStatic<LoggerFactory> loggerFactoryMocked = Mockito
                .mockStatic(LoggerFactory.class)) {
            loggerFactoryMocked
                    .when(() -> LoggerFactory.getLogger(FrontendVersion.class))
                    .thenReturn(logger);

            JsonObject sourceJsonMocked = getMockedJsonObject();

            Mockito.when(sourceJsonMocked.getString(Mockito.anyString()))
                    .thenThrow(new ClassCastException());
            filter.getFilteredVersions(sourceJsonMocked, versionOrigin);
            Mockito.verify(logger, Mockito.times(1)).warn(
                    "Ignoring error while parsing frontend dependency version for package '{}' in '{}'",
                    "test", versionOrigin);

            sourceJsonMocked = getMockedJsonObject();

            String nfeMessage = "NFE MSG";
            Mockito.when(sourceJsonMocked.getString(Mockito.anyString()))
                    .thenThrow(new NumberFormatException(nfeMessage));
            filter.getFilteredVersions(sourceJsonMocked, versionOrigin);
            Mockito.verify(logger, Mockito.times(1)).warn(
                    "Ignoring error while parsing frontend dependency version in {}: {}",
                    versionOrigin, nfeMessage);
        }
    }

    private JsonObject getMockedJsonObject() {
        JsonObject jsonObject = Mockito.mock(JsonObject.class);
        Mockito.when(jsonObject.keys()).thenReturn(new String[] { "test" });
        Mockito.when(jsonObject.hasKey("test")).thenReturn(true);
        return jsonObject;
    }

    private void assertMissingVaadinDependencies_allDependenciesSholdBeUserHandled(
            String depKey) throws IOException {
        String pkgJson = IOUtils.toString(
                Objects.requireNonNull(
                        getClass().getClassLoader().getResourceAsStream(
                                "versions/no_vaadin_package.json")),
                StandardCharsets.UTF_8);

        VersionsJsonFilter filter = new VersionsJsonFilter(Json.parse(pkgJson),
                depKey);
        JsonObject filteredJson = filter.getFilteredVersions(TEST_VERSION_JSON,
                "versions/versions.json");
        Assert.assertTrue(filteredJson.hasKey("@vaadin/vaadin-progress-bar"));
        Assert.assertTrue(filteredJson.hasKey("@vaadin/vaadin-upload"));
        Assert.assertTrue(filteredJson.hasKey("@polymer/iron-list"));

        Assert.assertEquals("1.1.2",
                filteredJson.getString("@vaadin/vaadin-progress-bar"));
    }

    private void assertFilterPlatformVersions_multipleUserChanged_correctlyIgnored(
            String depKey) throws IOException {
        String versions = IOUtils.toString(
                Objects.requireNonNull(getClass().getClassLoader()
                        .getResourceAsStream("versions/user_versions.json")),
                StandardCharsets.UTF_8);
        String pkgJson = IOUtils.toString(
                Objects.requireNonNull(getClass().getClassLoader()
                        .getResourceAsStream("versions/user_package.json")),
                StandardCharsets.UTF_8);

        VersionsJsonFilter filter = new VersionsJsonFilter(Json.parse(pkgJson),
                depKey);
        JsonObject filteredJson = filter.getFilteredVersions(
                Json.parse(versions), "versions/user_versions.json");
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
        String pkgJson = IOUtils.toString(
                Objects.requireNonNull(getClass().getClassLoader()
                        .getResourceAsStream("versions/package.json")),
                StandardCharsets.UTF_8);

        VersionsJsonFilter filter = new VersionsJsonFilter(Json.parse(pkgJson),
                depKey);
        JsonObject filteredJson = filter.getFilteredVersions(TEST_VERSION_JSON,
                "versions/versions.json");
        Assert.assertTrue(filteredJson.hasKey("@vaadin/vaadin-progress-bar"));
        Assert.assertTrue(filteredJson.hasKey("@vaadin/vaadin-upload"));
        Assert.assertTrue(filteredJson.hasKey("@polymer/iron-list"));

        Assert.assertEquals(
                "'progress-bar' should be the same in package and versions",
                "1.1.2", filteredJson.getString("@vaadin/vaadin-progress-bar"));
        Assert.assertEquals(
                "'upload' should be the same in package and versions", "4.2.2",
                filteredJson.getString("@vaadin/vaadin-upload"));
        Assert.assertEquals(
                "'enforced' version should come from platform (upgrade)",
                "1.5.0", filteredJson.getString("enforced"));
        Assert.assertEquals(
                "'iron-list' version should come from platform (downgrade)",
                "2.0.19", filteredJson.getString("@polymer/iron-list"));
    }
}
