/*
 * Copyright 2000-2018 Vaadin Ltd.
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
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import elemental.json.Json;
import elemental.json.JsonObject;

public class VersionsJsonConverterTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void convertPlatformVersions() throws IOException {
        String versions = IOUtils.toString(getClass().getClassLoader()
                        .getResourceAsStream("versions/versions.json"),
                StandardCharsets.UTF_8);
        String pkgJson = IOUtils.toString(getClass().getClassLoader()
                        .getResourceAsStream("versions/package.json"),
                StandardCharsets.UTF_8);

        VersionsJsonConverter convert = new VersionsJsonConverter(
                Json.parse(versions), Json.parse(pkgJson));
        JsonObject convertedJson = convert.getManagedVersions();
        Assert.assertTrue(convertedJson.hasKey("@vaadin/vaadin-progress-bar"));
        Assert.assertTrue(convertedJson.hasKey("@vaadin/vaadin-upload"));
        Assert.assertTrue(convertedJson.hasKey("@polymer/iron-list"));

        Assert.assertFalse(convertedJson.hasKey("flow"));
        Assert.assertFalse(convertedJson.hasKey("core"));
        Assert.assertFalse(convertedJson.hasKey("platform"));

        Assert.assertEquals("1.1.2",
                convertedJson.getString("@vaadin/vaadin-progress-bar"));
        Assert.assertEquals("4.2.2",
                convertedJson.getString("@vaadin/vaadin-upload"));
        Assert.assertEquals("3.0.2",
                convertedJson.getString("@polymer/iron-list"));
    }

    @Test
    public void convertPlatformVersions_multipleUserChanged_correctlyIgnored() throws IOException {
        String versions = IOUtils.toString(getClass().getClassLoader()
                        .getResourceAsStream("versions/user_versions.json"),
                StandardCharsets.UTF_8);
        String pkgJson = IOUtils.toString(getClass().getClassLoader()
                        .getResourceAsStream("versions/user_package.json"),
                StandardCharsets.UTF_8);

        VersionsJsonConverter convert = new VersionsJsonConverter(
                Json.parse(versions), Json.parse(pkgJson));
        JsonObject convertedJson = convert.getManagedVersions();
        List<String> expectedKeys = Arrays.asList("@vaadin/vaadin-notification",
                "@vaadin/vaadin-overlay",
                "@vaadin/vaadin-select",
                "@vaadin/vaadin-split-layout",
                "@vaadin/vaadin-tabs");

        for(String key : expectedKeys) {
            Assert.assertTrue(String.format("Key '%s' was expected, but not found", key), convertedJson.hasKey(key));
        }

        List<String> droppedKeys = Arrays.asList("flow",
                "core",
                "platform",
                "@vaadin/vaadin-ordered-layout",
                "@vaadin/vaadin-progress-bar",
                "@vaadin/vaadin-radio-button",
                "@vaadin/vaadin-confirm-dialog"
                );
        for(String key : droppedKeys) {
            Assert.assertFalse(String.format("User managed key '%s' was found.", key), convertedJson.hasKey(key));
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
                    entry.getValue(), convertedJson.getString(entry.getKey()));
        }
    }

    @Test
    public void missingVaadinDependencies_allDependenciesSholdBeUserHandled() throws IOException {
        String versions = IOUtils.toString(getClass().getClassLoader()
                        .getResourceAsStream("versions/versions.json"),
                StandardCharsets.UTF_8);
        String pkgJson = IOUtils.toString(getClass().getClassLoader()
                        .getResourceAsStream("versions/no_vaadin_package.json"),
                StandardCharsets.UTF_8);

        VersionsJsonConverter convert = new VersionsJsonConverter(
                Json.parse(versions), Json.parse(pkgJson));
        JsonObject convertedJson = convert.getManagedVersions();
        Assert.assertTrue(convertedJson.hasKey("@vaadin/vaadin-progress-bar"));
        Assert.assertFalse(convertedJson.hasKey("@vaadin/vaadin-upload"));
        Assert.assertFalse(convertedJson.hasKey("@polymer/iron-list"));

        Assert.assertFalse(convertedJson.hasKey("flow"));
        Assert.assertFalse(convertedJson.hasKey("core"));
        Assert.assertFalse(convertedJson.hasKey("platform"));

        Assert.assertEquals("1.1.2",
                convertedJson.getString("@vaadin/vaadin-progress-bar"));
    }
}
