/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
