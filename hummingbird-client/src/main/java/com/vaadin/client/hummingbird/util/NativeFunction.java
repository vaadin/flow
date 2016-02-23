package com.vaadin.client.hummingbird.util;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * Helper for dealing with JavaScript functions.
 *
 * @since
 * @author Vaadin Ltd
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
    private NativeFunction(String... paramsAndCode) {
        // Compiler will "implement" this
    }

    /**
     * Creates a new function with the given parameters and implementation. The
     * return value of this method is intended to be used as a type annotated
     * with {@link JsFunction @JsFunction}.
     *
     * @param paramsAndCode
     *            parameter names followed by the code of the function
     * @return the native function
     */
    @SuppressWarnings("unchecked")
    @JsOverlay
    public static <T> T create(String... paramsAndCode) {
        return (T) new NativeFunction(paramsAndCode);
    }
}
