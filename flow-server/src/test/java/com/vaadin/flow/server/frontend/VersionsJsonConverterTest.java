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
package com.vaadin.flow.server.frontend;

import java.io.IOException;

import tools.jackson.databind.JsonNode;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.internal.JacksonUtils;

import static com.vaadin.flow.server.frontend.VersionsJsonConverter.VAADIN_CORE_NPM_PACKAGE;

public class VersionsJsonConverterTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void convertPlatformVersions() throws IOException {
        // @formatter:off
        String json = "{\"core\": {"+
                            "\"flow\": { "
                            + " \"javaVersion\": \"3.0.0.alpha17\""
                            + "}, "
                        + " \"vaadin-progress-bar\": { "
                            + " \"npmName\": \"@vaadin/vaadin-progress-bar\", "
                            + "\"jsVersion\": \"1.1.2\", "
                            + "\"mode\": \"lit\" "
                          + "}"
                       + "},"  //core
                + "\"vaadin-upload\": { "
                    + "\"npmName\": \"@vaadin/vaadin-upload\", "
                    + "\"jsVersion\": \"4.2.2\""
                  + "},"+
                    "\"iron-list\": {\n" +
                    "            \"npmName\": \"@polymer/iron-list\",\n" +
                    "            \"npmVersion\": \"3.0.2\",\n" +
                    "            \"javaVersion\": \"3.0.0.beta1\",\n" +
                    "            \"jsVersion\": \"2.0.19\"\n" +
                    "        },"
                  + "\"vaadin-core\": {\n" +
                    "    \"jsVersion\": \"21.0.0.alpha1\",\n" + // broken for npm
                    "    \"npmName\": \""+VAADIN_CORE_NPM_PACKAGE+"\"\n" +
                    "},\n"
                  +"\"platform\": \"foo\""
                + "}";
        // @formatter:on

        VersionsJsonConverter convert = new VersionsJsonConverter(
                JacksonUtils.readTree(json), false, false);
        JsonNode convertedJson = convert.getConvertedJson();
        Assert.assertTrue(convertedJson.has("@vaadin/vaadin-progress-bar"));
        Assert.assertTrue(convertedJson.has("@vaadin/vaadin-upload"));
        Assert.assertTrue(convertedJson.has("@polymer/iron-list"));

        Assert.assertFalse(convertedJson.has("flow"));
        Assert.assertFalse(convertedJson.has("core"));
        Assert.assertFalse(convertedJson.has(VAADIN_CORE_NPM_PACKAGE));
        Assert.assertFalse(convertedJson.has("platform"));

        Assert.assertEquals("1.1.2",
                convertedJson.get("@vaadin/vaadin-progress-bar").textValue());
        Assert.assertEquals("4.2.2",
                convertedJson.get("@vaadin/vaadin-upload").textValue());
        Assert.assertEquals("3.0.2",
                convertedJson.get("@polymer/iron-list").textValue());
    }

    @Test
    public void reactRouterInUse_reactComponentsAreAdded() {
        String json = """
                {
                  "core": {
                    "flow": {
                      "javaVersion": "3.0.0.alpha17"
                    },
                    "vaadin-progress-bar": {
                      "npmName": "@vaadin/vaadin-progress-bar",
                      "jsVersion": "1.1.2"
                    }
                  },
                  "vaadin-upload": {
                    "npmName": "@vaadin/vaadin-upload",
                    "jsVersion": "4.2.2"
                  },
                  "iron-list": {
                    "npmName": "@polymer/iron-list",
                    "npmVersion": "3.0.2",
                    "javaVersion": "3.0.0.beta1",
                    "jsVersion": "2.0.19"
                  },
                  "vaadin-core": {
                      "jsVersion": "21.0.0.alpha1",
                      "npmName": "%s"
                  },
                  "react": {
                    "react-components": {
                      "jsVersion": "24.4.0-alpha7",
                      "npmName": "@vaadin/react-components"
                    }
                  },
                  "react-pro": {
                    "react-components-pro": {
                      "jsVersion": "24.4.0-alpha7",
                      "npmName": "@vaadin/react-components-pro"
                    }
                  },
                  "platform": "foo"
                }
                """.formatted(VAADIN_CORE_NPM_PACKAGE);

        VersionsJsonConverter convert = new VersionsJsonConverter(
                JacksonUtils.readTree(json), true, true);
        JsonNode convertedJson = convert.getConvertedJson();
        Assert.assertTrue(convertedJson.has("@vaadin/vaadin-progress-bar"));
        Assert.assertTrue(convertedJson.has("@vaadin/vaadin-upload"));
        Assert.assertTrue(convertedJson.has("@polymer/iron-list"));
        Assert.assertTrue(convertedJson.has("@vaadin/react-components-pro"));
        Assert.assertTrue(convertedJson.has("@vaadin/react-components"));

        Assert.assertFalse(convertedJson.has("flow"));
        Assert.assertFalse(convertedJson.has("core"));
        Assert.assertFalse(convertedJson.has(VAADIN_CORE_NPM_PACKAGE));
        Assert.assertFalse(convertedJson.has("platform"));
        Assert.assertFalse(convertedJson.has("react"));
        Assert.assertFalse(convertedJson.has("react-pro"));
        Assert.assertFalse(convertedJson.has("react-components"));
        Assert.assertFalse(convertedJson.has("react-components-pro"));

        Assert.assertEquals("1.1.2",
                convertedJson.get("@vaadin/vaadin-progress-bar").textValue());
        Assert.assertEquals("4.2.2",
                convertedJson.get("@vaadin/vaadin-upload").textValue());
        Assert.assertEquals("3.0.2",
                convertedJson.get("@polymer/iron-list").textValue());
        Assert.assertEquals("24.4.0-alpha7",
                convertedJson.get("@vaadin/react-components").textValue());
        Assert.assertEquals("24.4.0-alpha7",
                convertedJson.get("@vaadin/react-components-pro").textValue());
    }

    @Test
    public void reactRouterNotUsed_reactComponentsIgnored() {
        String json = """
                {
                  "core": {
                    "flow": {
                      "javaVersion": "3.0.0.alpha17"
                    },
                    "vaadin-progress-bar": {
                      "npmName": "@vaadin/vaadin-progress-bar",
                      "jsVersion": "1.1.2"
                    }
                  },
                  "vaadin-upload": {
                    "npmName": "@vaadin/vaadin-upload",
                    "jsVersion": "4.2.2"
                  },
                  "iron-list": {
                    "npmName": "@polymer/iron-list",
                    "npmVersion": "3.0.2",
                    "javaVersion": "3.0.0.beta1",
                    "jsVersion": "2.0.19"
                  },
                  "vaadin-core": {
                      "jsVersion": "21.0.0.alpha1",
                      "npmName": "%s"
                  },
                  "react": {
                    "react-components": {
                      "jsVersion": "24.4.0-alpha7",
                      "npmName": "@vaadin/react-components",
                      "mode": "react"
                    }
                  },
                  "react-pro": {
                    "react-components-pro": {
                      "jsVersion": "24.4.0-alpha7",
                      "npmName": "@vaadin/react-components-pro",
                      "mode": "react"
                    }
                  },
                  "platform": "foo"
                }
                """.formatted(VAADIN_CORE_NPM_PACKAGE);

        VersionsJsonConverter convert = new VersionsJsonConverter(
                JacksonUtils.readTree(json), false, true);
        JsonNode convertedJson = convert.getConvertedJson();
        Assert.assertTrue(convertedJson.has("@vaadin/vaadin-progress-bar"));
        Assert.assertTrue(convertedJson.has("@vaadin/vaadin-upload"));
        Assert.assertTrue(convertedJson.has("@polymer/iron-list"));

        Assert.assertFalse(convertedJson.has("flow"));
        Assert.assertFalse(convertedJson.has("core"));
        Assert.assertFalse(convertedJson.has(VAADIN_CORE_NPM_PACKAGE));
        Assert.assertFalse(convertedJson.has("platform"));
        Assert.assertFalse(convertedJson.has("@vaadin/react-components-pro"));
        Assert.assertFalse(convertedJson.has("@vaadin/react-components"));
        Assert.assertFalse(convertedJson.has("react"));
        Assert.assertFalse(convertedJson.has("react-pro"));
        Assert.assertFalse(convertedJson.has("react-components"));
        Assert.assertFalse(convertedJson.has("react-components-pro"));

        Assert.assertEquals("1.1.2",
                convertedJson.get("@vaadin/vaadin-progress-bar").textValue());
        Assert.assertEquals("4.2.2",
                convertedJson.get("@vaadin/vaadin-upload").textValue());
        Assert.assertEquals("3.0.2",
                convertedJson.get("@polymer/iron-list").textValue());
    }

    @Test
    public void reactRouterUsed_noVaadinRouterAdded() {
        String json = """
                {
                  "core": {
                    "flow": {
                      "javaVersion": "3.0.0.alpha17"
                    }
                  },
                  "vaadin-router": {
                    "npmName": "@vaadin/router",
                    "jsVersion": "2.0.0"
                  },
                  "react": {
                    "react-components": {
                      "jsVersion": "24.4.0-alpha7",
                      "npmName": "@vaadin/react-components",
                      "mode": "react"
                    }
                  },
                  "platform": "foo"
                }
                """.formatted(VAADIN_CORE_NPM_PACKAGE);

        VersionsJsonConverter convert = new VersionsJsonConverter(
                JacksonUtils.readTree(json), true, false);
        JsonNode convertedJson = convert.getConvertedJson();

        Assert.assertFalse(
                "Found @vaadin/router even though it should not be in use.",
                convertedJson.has("@vaadin/router"));
        Assert.assertTrue("Missing react-components",
                convertedJson.has("@vaadin/react-components"));
    }

    @Test
    public void testModeProperty() {
        String json = """
                {
                  "core": {
                    "flow": {
                      "javaVersion": "3.0.0.alpha17"
                    },
                    "vaadin-progress-bar": {
                      "npmName": "@vaadin/vaadin-progress-bar",
                      "jsVersion": "1.1.2",
                      "mode": "lit"
                    }
                  },
                  "vaadin-upload": {
                    "npmName": "@vaadin/vaadin-upload",
                    "jsVersion": "4.2.2",
                    "mode": ""
                  },
                  "iron-list": {
                    "npmName": "@polymer/iron-list",
                    "npmVersion": "3.0.2",
                    "javaVersion": "3.0.0.beta1",
                    "jsVersion": "2.0.19",
                    "mode": "all"
                  },
                  "vaadin-core": {
                      "jsVersion": "21.0.0.alpha1",
                      "npmName": "%s"
                  },
                  "react": {
                    "react-components": {
                      "jsVersion": "24.4.0-alpha7",
                      "npmName": "@vaadin/react-components",
                      "mode": "react"
                    }
                  },
                  "react-pro": {
                    "react-components-pro": {
                      "jsVersion": "24.4.0-alpha7",
                      "npmName": "@vaadin/react-components-pro",
                      "mode": "react"
                    }
                  },
                  "platform": "foo"
                }
                """.formatted(VAADIN_CORE_NPM_PACKAGE);

        // react enabled
        VersionsJsonConverter convert = new VersionsJsonConverter(
                JacksonUtils.readTree(json), true, false);
        JsonNode convertedJson = convert.getConvertedJson();
        Assert.assertFalse(convertedJson.has("@vaadin/vaadin-progress-bar"));
        Assert.assertTrue(convertedJson.has("@vaadin/vaadin-upload"));
        Assert.assertTrue(convertedJson.has("@polymer/iron-list"));
        Assert.assertTrue(convertedJson.has("@vaadin/react-components-pro"));
        Assert.assertTrue(convertedJson.has("@vaadin/react-components"));

        Assert.assertFalse(convertedJson.has("flow"));
        Assert.assertFalse(convertedJson.has("core"));
        Assert.assertFalse(convertedJson.has(VAADIN_CORE_NPM_PACKAGE));
        Assert.assertFalse(convertedJson.has("platform"));
        Assert.assertFalse(convertedJson.has("react"));
        Assert.assertFalse(convertedJson.has("react-pro"));
        Assert.assertFalse(convertedJson.has("react-components"));
        Assert.assertFalse(convertedJson.has("react-components-pro"));

        // react enabled, exclude web components
        convert = new VersionsJsonConverter(JacksonUtils.readTree(json), true,
                true);
        convertedJson = convert.getConvertedJson();
        Assert.assertFalse(convertedJson.has("@vaadin/vaadin-progress-bar"));
        Assert.assertTrue(convertedJson.has("@vaadin/vaadin-upload"));
        Assert.assertTrue(convertedJson.has("@polymer/iron-list"));
        Assert.assertFalse(convertedJson.has("@vaadin/react-components-pro"));
        Assert.assertFalse(convertedJson.has("@vaadin/react-components"));

        Assert.assertFalse(convertedJson.has("flow"));
        Assert.assertFalse(convertedJson.has("core"));
        Assert.assertFalse(convertedJson.has(VAADIN_CORE_NPM_PACKAGE));
        Assert.assertFalse(convertedJson.has("platform"));
        Assert.assertFalse(convertedJson.has("react"));
        Assert.assertFalse(convertedJson.has("react-pro"));
        Assert.assertFalse(convertedJson.has("react-components"));
        Assert.assertFalse(convertedJson.has("react-components-pro"));

        // react disabled
        convert = new VersionsJsonConverter(JacksonUtils.readTree(json), false,
                false);
        convertedJson = convert.getConvertedJson();
        Assert.assertTrue(convertedJson.has("@vaadin/vaadin-progress-bar"));
        Assert.assertTrue(convertedJson.has("@vaadin/vaadin-upload"));
        Assert.assertTrue(convertedJson.has("@polymer/iron-list"));
        Assert.assertFalse(convertedJson.has("@vaadin/react-components-pro"));
        Assert.assertFalse(convertedJson.has("@vaadin/react-components"));

        Assert.assertFalse(convertedJson.has("flow"));
        Assert.assertFalse(convertedJson.has("core"));
        Assert.assertFalse(convertedJson.has(VAADIN_CORE_NPM_PACKAGE));
        Assert.assertFalse(convertedJson.has("platform"));
        Assert.assertFalse(convertedJson.has("react"));
        Assert.assertFalse(convertedJson.has("react-pro"));
        Assert.assertFalse(convertedJson.has("react-components"));

        // react disabled, exclude web components
        convert = new VersionsJsonConverter(JacksonUtils.readTree(json), false,
                true);
        convertedJson = convert.getConvertedJson();
        Assert.assertFalse(convertedJson.has("@vaadin/vaadin-progress-bar"));
        Assert.assertTrue(convertedJson.has("@vaadin/vaadin-upload"));
        Assert.assertTrue(convertedJson.has("@polymer/iron-list"));
        Assert.assertFalse(convertedJson.has("@vaadin/react-components-pro"));
        Assert.assertFalse(convertedJson.has("@vaadin/react-components"));

        Assert.assertFalse(convertedJson.has("flow"));
        Assert.assertFalse(convertedJson.has("core"));
        Assert.assertFalse(convertedJson.has(VAADIN_CORE_NPM_PACKAGE));
        Assert.assertFalse(convertedJson.has("platform"));
        Assert.assertFalse(convertedJson.has("react"));
        Assert.assertFalse(convertedJson.has("react-pro"));
        Assert.assertFalse(convertedJson.has("react-components"));
        Assert.assertFalse(convertedJson.has("react-components-pro"));
    }

    @Test
    public void testExclusionsArrayProperty() {
        String json = """
                {
                  "core": {
                    "flow": {
                      "javaVersion": "3.0.0.alpha17"
                    },
                    "vaadin-progress-bar": {
                      "npmName": "@vaadin/vaadin-progress-bar",
                      "jsVersion": "1.1.2",
                      "mode": "lit"
                    }
                  },
                  "vaadin-upload": {
                    "npmName": "@vaadin/vaadin-upload",
                    "jsVersion": "4.2.2",
                    "mode": ""
                  },
                  "iron-list": {
                    "npmName": "@polymer/iron-list",
                    "npmVersion": "3.0.2",
                    "javaVersion": "3.0.0.beta1",
                    "jsVersion": "2.0.19",
                    "mode": "all"
                  },
                  "vaadin-core": {
                      "jsVersion": "21.0.0.alpha1",
                      "npmName": "%s"
                  },
                  "react": {
                    "react-components": {
                      "jsVersion": "24.4.0-alpha7",
                      "npmName": "@vaadin/react-components",
                      "mode": "react",
                      "exclusions": [
                        "@vaadin/vaadin-progress-bar",
                        "@vaadin/vaadin-upload",
                        "@polymer/iron-list",
                        "@vaadin/react-components-pro"
                        ]
                    }
                  },
                  "react-pro": {
                    "react-components-pro": {
                      "jsVersion": "24.4.0-alpha7",
                      "npmName": "@vaadin/react-components-pro",
                      "mode": "react"
                    }
                  },
                  "platform": "foo"
                }
                """.formatted(VAADIN_CORE_NPM_PACKAGE);

        // react enabled
        VersionsJsonConverter convert = new VersionsJsonConverter(
                JacksonUtils.readTree(json), true, false);
        JsonNode convertedJson = convert.getConvertedJson();
        Assert.assertFalse(convertedJson.has("@vaadin/vaadin-progress-bar"));
        Assert.assertFalse(convertedJson.has("@vaadin/vaadin-upload"));
        Assert.assertFalse(convertedJson.has("@polymer/iron-list"));
        Assert.assertFalse(convertedJson.has("@vaadin/react-components-pro"));
        Assert.assertTrue(convertedJson.has("@vaadin/react-components"));

        Assert.assertFalse(convertedJson.has("flow"));
        Assert.assertFalse(convertedJson.has("core"));
        Assert.assertFalse(convertedJson.has(VAADIN_CORE_NPM_PACKAGE));
        Assert.assertFalse(convertedJson.has("platform"));
        Assert.assertFalse(convertedJson.has("react"));
        Assert.assertFalse(convertedJson.has("react-pro"));
        Assert.assertFalse(convertedJson.has("react-components"));
        Assert.assertFalse(convertedJson.has("react-components-pro"));

        // react disabled
        convert = new VersionsJsonConverter(JacksonUtils.readTree(json), false,
                false);
        convertedJson = convert.getConvertedJson();
        Assert.assertTrue(convertedJson.has("@vaadin/vaadin-progress-bar"));
        Assert.assertTrue(convertedJson.has("@vaadin/vaadin-upload"));
        Assert.assertTrue(convertedJson.has("@polymer/iron-list"));
        Assert.assertFalse(convertedJson.has("@vaadin/react-components-pro"));
        Assert.assertFalse(convertedJson.has("@vaadin/react-components"));

        Assert.assertFalse(convertedJson.has("flow"));
        Assert.assertFalse(convertedJson.has("core"));
        Assert.assertFalse(convertedJson.has(VAADIN_CORE_NPM_PACKAGE));
        Assert.assertFalse(convertedJson.has("platform"));
        Assert.assertFalse(convertedJson.has("react"));
        Assert.assertFalse(convertedJson.has("react-pro"));
        Assert.assertFalse(convertedJson.has("react-components"));
        Assert.assertFalse(convertedJson.has("react-components-pro"));
    }
}
