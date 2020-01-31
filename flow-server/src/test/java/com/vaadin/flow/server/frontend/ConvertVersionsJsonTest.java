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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import elemental.json.Json;
import elemental.json.JsonObject;

public class ConvertVersionsJsonTest {

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
                  + "},"
                    +"\"platform\": \"foo\""
                + "}";
        // @formatter:on

        ConvertVersionsJson convert = new ConvertVersionsJson(Json.parse(json));
        JsonObject conertedJson = convert.convert();
        Assert.assertTrue(conertedJson.hasKey("@vaadin/vaadin-progress-bar"));
        Assert.assertTrue(conertedJson.hasKey("@vaadin/vaadin-upload"));

        Assert.assertFalse(conertedJson.hasKey("flow"));
        Assert.assertFalse(conertedJson.hasKey("core"));
        Assert.assertFalse(conertedJson.hasKey("platform"));

        Assert.assertEquals("1.1.2",
                conertedJson.getString("@vaadin/vaadin-progress-bar"));
        Assert.assertEquals("4.2.2",
                conertedJson.getString("@vaadin/vaadin-upload"));
    }
}
