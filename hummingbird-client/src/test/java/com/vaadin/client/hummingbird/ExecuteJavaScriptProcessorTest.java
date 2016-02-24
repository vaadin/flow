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
package com.vaadin.client.hummingbird;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.client.Registry;
import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.hummingbird.util.JsonUtil;

import elemental.json.Json;
import elemental.json.JsonArray;

public class ExecuteJavaScriptProcessorTest {
    private static class CollectingExecuteJavaScriptProcessor
            extends ExecuteJavaScriptProcessor {
        private final List<String[]> parameterNamesAndCodeList = new ArrayList<>();
        private final List<JsArray<Object>> parametersList = new ArrayList<>();

        private CollectingExecuteJavaScriptProcessor() {
            super(new Registry() {
                {
                    set(StateTree.class, new StateTree(this));
                }
            });
        }

        @Override
        protected void invoke(String[] parameterNamesAndCode,
                JsArray<Object> parameters) {
            parameterNamesAndCodeList.add(parameterNamesAndCode);
            parametersList.add(parameters);
        }
    }

    @Test
    public void testExecute() {
        CollectingExecuteJavaScriptProcessor processor = new CollectingExecuteJavaScriptProcessor();

        JsonArray invocation1 = JsonUtil.createArray(Json.create("script1"));
        JsonArray invocation2 = Stream.of("param1", "param2", "script2")
                .map(Json::create).collect(JsonUtil.asArray());
        JsonArray invocations = JsonUtil.createArray(invocation1, invocation2);

        processor.execute(invocations);

        Assert.assertEquals(2, processor.parameterNamesAndCodeList.size());
        Assert.assertEquals(2, processor.parametersList.size());

        Assert.assertArrayEquals(new String[] { "script1" },
                processor.parameterNamesAndCodeList.get(0));
        Assert.assertEquals(0, processor.parametersList.get(0).length());

        Assert.assertArrayEquals(new String[] { "$0", "$1", "script2" },
                processor.parameterNamesAndCodeList.get(1));
        Assert.assertEquals(2, processor.parametersList.get(1).length());
        Assert.assertEquals("param1", processor.parametersList.get(1).get(0));
        Assert.assertEquals("param2", processor.parametersList.get(1).get(1));

    }
}
