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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class RequestParametersTest {

    @Test
    public void emptyParameters() {
        RequestParameters emptyParams = RequestParameters.empty();

        assertEquals(Collections.emptyMap(), emptyParams.getParameters());
    }

    @Test
    public void simpleParameters() {
        Map<String, String> inputParameters = new HashMap<>();
        inputParameters.put("one", "1");
        inputParameters.put("two", "2");
        inputParameters.put("three", "3");

        RequestParameters simpleParams = RequestParameters
                .simple(inputParameters);

        Map<String, List<String>> expectedFullParams = new HashMap<>();
        expectedFullParams.put("one", Collections.singletonList("1"));
        expectedFullParams.put("two", Collections.singletonList("2"));
        expectedFullParams.put("three", Collections.singletonList("3"));
        assertEquals(expectedFullParams, simpleParams.getParameters());
    }

    @Test
    public void complexParameters() {
        Map<String, String[]> inputParameters = new HashMap<>();
        inputParameters.put("one", new String[] { "1", "11" });
        inputParameters.put("two", new String[] { "2", "22" });
        inputParameters.put("three", new String[] { "3" });

        RequestParameters fullParams = RequestParameters.full(inputParameters);

        Map<String, List<String>> expectedFullParams = new HashMap<>();
        expectedFullParams.put("one", Arrays.asList("1", "11"));
        expectedFullParams.put("two", Arrays.asList("2", "22"));
        expectedFullParams.put("three", Collections.singletonList("3"));
        assertEquals(expectedFullParams, fullParams.getParameters());
    }
}
