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
package com.vaadin.client.flow.util;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

import com.vaadin.client.flow.collection.JsArray;

/**
 * JsInterop wrapper for interacting with the JavaScript Function type.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, name = "Function", namespace = JsPackage.GLOBAL)
@SuppressWarnings("unusable-by-js")
public class NativeFunction {
    /**
     * Creates a new function with the given parameters and implementation.
     *
     * @param paramsAndCode
     *            parameter names followed by the code of the function
     */
    public NativeFunction(String... paramsAndCode) {
        /*
         * The GWT compiler will replace this JsInterop constructor with a JS
         * invocation of new Function(<arguments>)
         */
    }

    /**
     * Invokes this function with a given <code>this</code> and arguments
     * provided as an array.
     *
     * @param thisArg
     *            the value of <code>this</code>
     * @param arguments
     *            an array of arguments
     * @return the return value of the invocation
     */
    public native Object apply(Object thisArg, JsArray<?> arguments);

    /**
     * Invokes this function with a given <code>this</code> and arguments
     * provides as varargs.
     *
     * @param thisArg
     *            the value of <code>this</code>
     * @param arguments
     *            the arguments to invoke this function with
     * @return the return value of the invocation
     */
    public native Object call(Object thisArg, Object... arguments);

    /**
     * Creates a new function with the given parameters and implementation. The
     * return value of this method is intended to be used as a type annotated
     * with {@link JsFunction @JsFunction}.
     *
     * @param paramsAndCode
     *            parameter names followed by the code of the function
     * @param <T>
     *            the function type
     * @return the native function
     */
    @SuppressWarnings("unchecked")
    @JsOverlay
    public static <T> T create(String... paramsAndCode) {
        return (T) new NativeFunction(paramsAndCode);
    }
}
