/*
 * Copyright 2000-2019 Vaadin Ltd.
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
package com.vaadin.client.communication;

import java.util.function.Supplier;

import org.junit.Assert;

import com.vaadin.client.flow.nodefeature.MapProperty;

public abstract class AbstractConfigurationTest {

    protected abstract MapProperty getProperty(String key);

    protected void testString(String key, Supplier<String> getter) {
        String value1 = "foo";
        String value2 = "bar";

        getProperty(key).setValue(value1);
        Assert.assertEquals(value1, getter.get());

        getProperty(key).setValue(value2);
        Assert.assertEquals(value2, getter.get());
    }

    protected void testInt(String key, Supplier<Integer> getter) {
        Integer value1 = 1234;
        Integer value2 = 1;

        // Numbers are always passed as doubles from the server
        getProperty(key).setValue((double) value1);
        Assert.assertEquals(value1, getter.get());

        // Numbers are always passed as doubles from the server
        getProperty(key).setValue((double) value2);
        Assert.assertEquals(value2, getter.get());
    }

    protected void testBoolean(String key, Supplier<Boolean> getter) {
        Boolean value1 = Boolean.TRUE;
        Boolean value2 = Boolean.FALSE;

        getProperty(key).setValue(value1);
        Assert.assertEquals(value1, getter.get());

        getProperty(key).setValue(value2);
        Assert.assertEquals(value2, getter.get());
    }

}
