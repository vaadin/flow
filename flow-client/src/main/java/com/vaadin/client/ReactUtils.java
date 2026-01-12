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

import java.util.function.Supplier;

import elemental.dom.Element;

/**
 * Utils class, intended to ease working with React component related code on
 * the client side.
 *
 * @author Vaadin Ltd
 * @since 24.5.
 */
public final class ReactUtils {

    /**
     * Add a callback to the react component that is called when the component
     * initialization is ready for binding flow.
     *
     * @param element
     *            react component element
     * @param name
     *            name of container to bind to
     * @param runnable
     *            callback function runnable
     */
    public static native void addReadyCallback(Element element, String name,
            Runnable runnable)
    /*-{
            if(element.addReadyCallback){
                element.addReadyCallback(name,
                    $entry(runnable.@java.lang.Runnable::run(*).bind(runnable))
                );
            }
    }-*/;

    /**
     * Check if the react element is initialized and functional.
     *
     * @param elementLookup
     *            react element lookup supplier
     * @return {@code true} if Flow binding can already be done
     */
    public static boolean isInitialized(Supplier<Element> elementLookup) {
        return elementLookup.get() != null;
    }
}
