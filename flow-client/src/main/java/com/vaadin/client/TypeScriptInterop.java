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
package com.vaadin.client;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Test class to validate GWT can call TypeScript code.
 * This proves the migration approach works.
 */
public class TypeScriptInterop {

    /**
     * Test calling TypeScript ArrayUtil.isEmpty from GWT.
     *
     * @param array the JavaScript array to check
     * @return true if the array is empty
     */
    public static native boolean isArrayEmpty(JavaScriptObject array)
    /*-{
        if ($wnd.Vaadin && $wnd.Vaadin.TypeScript && $wnd.Vaadin.TypeScript.ArrayUtil) {
            return $wnd.Vaadin.TypeScript.ArrayUtil.isEmpty(array);
        }
        // Fallback if TypeScript not loaded
        return !array || array.length === 0;
    }-*/;

    /**
     * Test calling TypeScript ArrayUtil.clear from GWT.
     *
     * @param array the JavaScript array to clear
     */
    public static native void clearArray(JavaScriptObject array)
    /*-{
        if ($wnd.Vaadin && $wnd.Vaadin.TypeScript && $wnd.Vaadin.TypeScript.ArrayUtil) {
            $wnd.Vaadin.TypeScript.ArrayUtil.clear(array);
        } else {
            // Fallback if TypeScript not loaded
            array.length = 0;
        }
    }-*/;

    /**
     * Check if TypeScript interop is available.
     *
     * @return true if TypeScript code is loaded and available
     */
    public static native boolean isTypeScriptAvailable()
    /*-{
        return !!($wnd.Vaadin && $wnd.Vaadin.TypeScript && $wnd.Vaadin.TypeScript.ArrayUtil);
    }-*/;
}
