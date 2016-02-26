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
package com.vaadin.hummingbird.namespace;

import org.junit.Assert;

import com.vaadin.hummingbird.util.JsonUtil;
import com.vaadin.tests.util.MockUI;

import elemental.json.Json;
import elemental.json.JsonObject;

public class DependencyListNamspaceTest {

    public void testAddDependencies() {
        MockUI ui = new MockUI();
        DependencyListNamespace namespace = ui.getFrameworkData().getStateTree()
                .getRootNode().getNamespace(DependencyListNamespace.class);

        Assert.assertEquals(0, namespace.size());

        ui.getPage().addStyleSheet("styleSheetUrl");

        JsonObject expectedStyleSheetJson = Json.createObject();
        expectedStyleSheetJson.put(DependencyListNamespace.KEY_TYPE,
                DependencyListNamespace.TYPE_STYLESHEET);
        expectedStyleSheetJson.put(DependencyListNamespace.KEY_URL,
                "styleSheetUrl");

        Assert.assertEquals(1, namespace.size());
        Assert.assertTrue(
                JsonUtil.jsonEquals(expectedStyleSheetJson, namespace.get(0)));

        ui.getPage().addJavaScript("jsUrl");

        JsonObject expectedJsJson = Json.createObject();
        expectedJsJson.put(DependencyListNamespace.KEY_TYPE,
                DependencyListNamespace.TYPE_JAVASCRIPT);
        expectedJsJson.put(DependencyListNamespace.KEY_URL, "jsUrl");

        Assert.assertEquals(2, namespace.size());
        Assert.assertTrue(
                JsonUtil.jsonEquals(expectedStyleSheetJson, namespace.get(0)));
        Assert.assertTrue(
                JsonUtil.jsonEquals(expectedJsJson, namespace.get(1)));
    }
}
