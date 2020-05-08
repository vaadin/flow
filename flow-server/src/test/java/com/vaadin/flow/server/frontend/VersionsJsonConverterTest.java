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
        // @formatter:off
        String json = "{\"core\": {"+
                            "\"flow\": { "
                            + " \"javaVersion\": \"3.0.0.alpha17\""
                            + "}, "
                        + " \"vaadin-progress-bar\": { "
                            + " \"npmName\": \"@vaadin/vaadin-progress-bar\", "
                            + "\"jsVersion\": \"1.1.2\" "
                          + "},"
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
                    +"\"platform\": \"foo\""
                + "}";
        // @formatter:on

        VersionsJsonConverter convert = new VersionsJsonConverter(
                Json.parse(json));
        JsonObject convertedJson = convert.getConvertedJson();
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
}
