/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.client.flow.nodefeature;

import java.util.Optional;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

import com.vaadin.client.JsRunnable;
import com.vaadin.client.flow.reactive.ReactiveValue;
import com.vaadin.client.flow.reactive.ReactiveValueChangeListener;

import elemental.events.EventRemover;

/**
 * A property in a node map. Pure {@code @JsType(isNative=true)} binding to the
 * TypeScript implementation at
 * {@code src/main/frontend/internal/client/flow/nodefeature/MapProperty.ts}.
 *
 * <p>
 * The Java-only {@code syncToServer} / {@code getSyncToServerCommand} helpers
 * stay as {@code @JsOverlay} methods because they dispatch into the still-Java
 * {@link StateTree} server-connector chain. {@code getPreviousDomValue()}
 * reconstructs the historical {@code Optional<Object>} return from the TS-side
 * {@code hasPreviousDomValue} / {@code getPreviousDomValueRaw} pair.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.flow.nodefeature", name = "MapProperty")
public class MapProperty implements ReactiveValue {

    // Bound to the `MapProperty.NO_OP` static declared in MapProperty.ts so
    // Java and TS callers share the same JS function reference. Debouncer's
    // runCommands relies on reference equality to detect a no-op sync.
    public static JsRunnable NO_OP;

    public MapProperty(String name, NodeMap map) {
        // Constructor delegates to TS class with forceValueUpdate=false.
    }

    public MapProperty(String name, NodeMap map, boolean forceValueUpdate) {
        // Defined by the TS class constructor.
    }

    public native String getName();

    public native NodeMap getMap();

    public native Object getValue();

    public native boolean hasValue();

    public native boolean hasPropertyValue();

    public native void setValue(Object value);

    public native void removeValue();

    public native EventRemover addChangeListener(
            MapPropertyChangeListener listener);

    @Override
    public native EventRemover addReactiveValueChangeListener(
            ReactiveValueChangeListener reactiveValueChangeListener);

    @JsMethod(name = "getValueOrDefaultNumber")
    public native int getValueOrDefault(int defaultValue);

    @JsMethod(name = "getValueOrDefaultBoolean")
    public native boolean getValueOrDefault(boolean defaultValue);

    @JsMethod(name = "getValueOrDefaultString")
    public native String getValueOrDefault(String defaultValue);

    public native void setPreviousDomValue(Object previousDomValue);

    public native boolean hasPreviousDomValue();

    public native Object getPreviousDomValueRaw();

    public native void clearPreviousDomValue();

    public native boolean isServerUpdate();

    public native void markServerUpdate(boolean value);

    public native void doSetValue(Object value);

    @JsOverlay
    public final Optional<Object> getPreviousDomValue() {
        if (!hasPreviousDomValue()) {
            return Optional.empty();
        }
        return Optional.ofNullable(getPreviousDomValueRaw());
    }

    /** Sets the value and synchronizes it to the server. */
    public native void syncToServer(Object newValue);

    /** Returns the deferred send command for syncing the new value. */
    public native JsRunnable getSyncToServerCommand(Object newValue);
}
