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
package com.vaadin.flow.spring;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

public class RootMappedConditionTest {

    @Test
    public void lookupProperty() {
        assertUrlMapping("url-mapping");
        assertUrlMapping("urlmapping");
        assertUrlMapping("urlMapping");
        assertUrlMapping("URL-mapping");
        assertUrlMapping("URLMAPPING");
    }

    private void assertUrlMapping(String key) {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("vaadin." + key, "abc");
        Assert.assertEquals("abc",
                RootMappedCondition.getUrlMapping(environment));
    }
}
