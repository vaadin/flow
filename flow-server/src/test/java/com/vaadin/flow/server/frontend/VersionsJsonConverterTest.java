/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import elemental.json.Json;
import elemental.json.JsonObject;

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
                  + "\"vaadin-core\": {\n" +
                    "    \"jsVersion\": \"21.0.0.alpha1\",\n" + // broken for npm
                    "    \"npmName\": \""+VAADIN_CORE_NPM_PACKAGE+"\"\n" +
                    "},\n"
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
        Assert.assertFalse(convertedJson.hasKey(VAADIN_CORE_NPM_PACKAGE));
        Assert.assertFalse(convertedJson.hasKey("platform"));

        Assert.assertEquals("1.1.2",
                convertedJson.getString("@vaadin/vaadin-progress-bar"));
        Assert.assertEquals("4.2.2",
                convertedJson.getString("@vaadin/vaadin-upload"));
        Assert.assertEquals("3.0.2",
                convertedJson.getString("@polymer/iron-list"));
    }
}
