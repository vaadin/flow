/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.server.communication;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.util.JsonUtil;
import com.vaadin.ui.FrameworkData.JavaScriptInvocation;

import elemental.json.Json;
import elemental.json.JsonArray;

public class UidlWriterTest {
    @Test
    public void testEncodeExecuteJavaScript() {
        Element element = new Element("div");

        List<JavaScriptInvocation> executeJavaScriptList = Arrays.asList(
                new JavaScriptInvocation("$0.focus()", Arrays.asList(element)),
                new JavaScriptInvocation("console.log($0, $1)",
                        Arrays.asList("Lives remaining:", Integer.valueOf(3))));

        JsonArray json = UidlWriter
                .encodeExecuteJavaScriptList(executeJavaScriptList);

        JsonArray expectedJson = JsonUtil.createArray(
                JsonUtil.createArray(
                        // Null since element is not attached
                        Json.createNull(), Json.create("$0.focus()")),
                JsonUtil.createArray(Json.create("Lives remaining:"),
                        Json.create(3), Json.create("console.log($0, $1)")));

        Assert.assertTrue(JsonUtil.jsonEquals(expectedJson, json));
    }
}
