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
package com.vaadin.client.flow.util;

import com.vaadin.client.ClientEngineTestBase;
import com.vaadin.client.flow.collection.JsCollections;

import elemental.client.Browser;
import jsinterop.annotations.JsFunction;

public class GwtNativeFunctionTest extends ClientEngineTestBase {

    @JsFunction
    @FunctionalInterface
    private interface Adder {
        int add(int a, int b);
    }

    public void testAsJsFunction() {
        Adder adder = NativeFunction.create("a", "b", "return a + b");

        int result = adder.add(3, 4);

        assertEquals(7, result);
    }

    public void testApply() {
        NativeFunction adder = new NativeFunction("param1", "param2",
                "return this + param1 + param2");

        Object result = adder.apply(Browser.getDocument(),
                JsCollections.array(" myString ", Double.valueOf(42)));

        assertEquals("[object HTMLDocument] myString 42", result);
    }

    public void testCall() {
        NativeFunction adder = new NativeFunction("param1", "param2",
                "return this + param1 + param2");

        Object result = adder.call(Browser.getDocument(), " myString ",
                Double.valueOf(42));

        assertEquals("[object HTMLDocument] myString 42", result);
    }
}
