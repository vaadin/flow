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
package com.vaadin.hummingbird.template;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.junit.Assert;
import org.junit.Test;

public class JavaScriptNumberFormatterTest {
    ScriptEngine nashhorn = new ScriptEngineManager()
            .getEngineByName("nashorn");

    @Test
    public void doubles() throws ScriptException {
        // JSON does not support NaN or infinity
        assertDouble(Double.NaN, "");
        assertDouble(Double.POSITIVE_INFINITY, "");
        assertDouble(Double.NEGATIVE_INFINITY, "");

        assertDouble(Double.MIN_VALUE);
        assertDouble(Double.MAX_VALUE);
        assertDouble(0.0);
        assertDouble(0.00);
        assertDouble(0.5);
        assertDouble(1);
        assertDouble(100);

        // JS converts to scientific format later than Java
        // Can be fixed if needed
        // 999999999999999900000.0
        // 1000000000000000000000.0
        // -100000000000000000000.0
        // -1000000000000000000000.0
    }

    private void assertDouble(double d) throws ScriptException {
        String jsFormatted = (String) nashhorn.eval("\"\"+" + d);
        assertDouble(d, jsFormatted);
    }

    private void assertDouble(double d, String jsFormatted)
            throws ScriptException {
        String javaFormatted = JavaScriptNumberFormatter.toString(d);
        Assert.assertEquals(jsFormatted, javaFormatted);
    }
}
