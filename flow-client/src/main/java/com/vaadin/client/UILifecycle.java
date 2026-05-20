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
package com.vaadin.client;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

/**
 * Manages the lifecycle of a UI. Pure {@code @JsType(isNative=true)} binding to
 * the TypeScript implementation at
 * {@code src/main/frontend/internal/client/UILifecycle.ts}. The Java
 * {@link UIState} enum is retained for type-safe consumer code; the JS side
 * stores the matching string and {@link #getState()} / {@link #setState} adapt
 * between the two on each call.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client", name = "UILifecycle")
public class UILifecycle {

    /** State of the UI. */
    public enum UIState {
        INITIALIZING, RUNNING, TERMINATED;
    }

    /** JS handler-registration shape returned by {@link #addHandler}. */
    @JsType(isNative = true)
    public interface HandlerRegistration {
        void removeHandler();
    }

    /** Native event shape passed to {@link StateChangeHandler}. */
    @JsType(isNative = true)
    public interface StateChangeEvent {
        UILifecycle getUiLifecycle();
    }

    /** Listener for UI state changes. */
    @FunctionalInterface
    @JsFunction
    @SuppressWarnings("unusable-by-js")
    public interface StateChangeHandler {
        void onUIStateChanged(StateChangeEvent event);
    }

    public UILifecycle() {
        // Defined by the TS class constructor.
    }

    @JsMethod(name = "getStateName")
    native String getStateNameImpl();

    @JsMethod(name = "setStateName")
    native void setStateNameImpl(String state);

    public native boolean isRunning();

    public native boolean isTerminated();

    public native HandlerRegistration addHandler(StateChangeHandler handler);

    /** Gets the current UI state. */
    @JsOverlay
    public final UIState getState() {
        return UIState.valueOf(getStateNameImpl());
    }

    /** Sets the UI state. Only forward transitions are allowed. */
    @JsOverlay
    public final void setState(UIState state) {
        setStateNameImpl(state.name());
    }
}
