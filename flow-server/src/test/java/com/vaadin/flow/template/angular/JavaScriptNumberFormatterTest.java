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
package com.vaadin.flow.template.angular;

import static org.junit.Assert.assertEquals;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.junit.Test;

public class JavaScriptNumberFormatterTest {
    ScriptEngine nashhorn = new ScriptEngineManager()
            .getEngineByName("nashorn");

    @Test
    public void doubles() throws ScriptException {
        // These values cannot be sent in the model, but they might still show
        // up as a result of evaluating JS
        assertDouble(Double.NaN, "NaN");
        assertDouble(Double.POSITIVE_INFINITY, "Infinity");
        assertDouble(Double.NEGATIVE_INFINITY, "-Infinity");

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
        assertEquals(jsFormatted, javaFormatted);
    }
}
