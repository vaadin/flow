/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.testnpmonlyfeatures.nobuildmojo;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;

import com.vaadin.flow.component.trigger.internal.TriggerJs;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.server.Constants;

/**
 * Checks the stats of the pre-built bundle that a production build of
 * test-prod-bundle wrote.
 */
public class PrebuiltBundleStatsTest {

    @Test
    public void listsTheFunctionIdsOfTheBundle() throws Exception {
        Set<String> ids;
        try (InputStream stats = getClass().getClassLoader()
                .getResourceAsStream(
                        Constants.PROD_BUNDLE_JAR_PATH + "config/stats.json")) {
            Assert.assertNotNull("The pre-built bundle should have stats.json",
                    stats);
            JsonNode functionIds = JacksonUtils
                    .readTree(new String(stats.readAllBytes(),
                            StandardCharsets.UTF_8))
                    .get("jsDefinitionFunctionIds");
            Assert.assertTrue(
                    "The stats should list the functions of the bundle",
                    functionIds instanceof ArrayNode);
            ids = JacksonUtils.<JsonNode> stream((ArrayNode) functionIds)
                    .map(JsonNode::asString).collect(Collectors.toSet());
        }

        // A definition of Flow, which this application has too
        String flowId = JsCall.functionId(TriggerJs.class
                .getMethod("callChannel", ReturnChannelRegistration.class));
        Assert.assertTrue("The stats should list the function " + flowId,
                ids.contains(flowId));
        // PrebuiltOnlyJs of test-prod-bundle, which this application does not
        // have. NoAppBundleIT checks that the application still uses the
        // bundle, so a bundle with more functions than needed is accepted.
        String prebuiltOnlyId = JsCall.functionId("window.prebuiltOnly = $0", 1,
                false);
        Assert.assertTrue(
                "The stats should list the function " + prebuiltOnlyId,
                ids.contains(prebuiltOnlyId));
    }
}
