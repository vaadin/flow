/*
 * Copyright 2000-2017 Vaadin Ltd.
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

package com.vaadin.hummingbird.router;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class RequestParametersTest {

    @Test
    public void emptyParameters() {
        RequestParameters emptyParams = RequestParameters.empty();

        assertEquals(Collections.emptyMap(), emptyParams.getFullParameterMap());
        assertEquals(Collections.emptyMap(),
                emptyParams.getSimpleParameterMap());
    }

    @Test
    public void simpleParameters() {
        Map<String, String> inputParameters = new HashMap<>();
        inputParameters.put("one", "1");
        inputParameters.put("two", "2");
        inputParameters.put("three", "3");

        RequestParameters simpleParams = RequestParameters
                .simple(inputParameters);

        assertEquals(inputParameters, simpleParams.getSimpleParameterMap());
        Map<String, String[]> expectedFullParams = new HashMap<>();
        expectedFullParams.put("one", new String[] { "1" });
        expectedFullParams.put("two", new String[] { "2" });
        expectedFullParams.put("three", new String[] { "3" });
        assertMapsWithArraysEqual(expectedFullParams,
                simpleParams.getFullParameterMap());
    }

    @Test
    public void complexParameters() {
        Map<String, String[]> inputParameters = new HashMap<>();
        inputParameters.put("one", new String[] { "1", "11" });
        inputParameters.put("two", new String[] { "2", "22" });
        inputParameters.put("three", new String[] { "3" });

        RequestParameters fullParams = RequestParameters.full(inputParameters);

        assertMapsWithArraysEqual(inputParameters,
                fullParams.getFullParameterMap());
        Map<String, String> expectedSimpleParams = new HashMap<>();
        expectedSimpleParams.put("one", "1");
        expectedSimpleParams.put("two", "2");
        expectedSimpleParams.put("three", "3");

        assertEquals(expectedSimpleParams, fullParams.getSimpleParameterMap());
    }

    private <T, K> void assertMapsWithArraysEqual(Map<T, K[]> expected,
            Map<T, K[]> actual) {
        assertEquals(expected.size(), actual.size());
        assertEquals(expected.keySet(), actual.keySet());
        expected.forEach((key, expectedArray) -> assertArrayEquals(
                expectedArray, actual.get(key)));
    }
}
