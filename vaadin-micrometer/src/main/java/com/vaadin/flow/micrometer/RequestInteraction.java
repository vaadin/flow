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
package com.vaadin.flow.micrometer;

/**
 * Thread-local relay that lets binders which observe a specific kind of
 * server-side activity (poll, navigation) tell the {@link RequestMetricsBinder}
 * what the in-flight UIDL request actually did.
 * <p>
 * UIDL processing is synchronous on the request thread, so a value set by a
 * poll listener or navigation listener during request handling is visible to
 * the interceptor's {@code requestEnd} on the same thread. The interceptor
 * clears the slot at {@code requestStart} and consumes it at
 * {@code requestEnd}.
 */
final class RequestInteraction {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private RequestInteraction() {
    }

    /**
     * Records what the current request did. Last writer wins; a request that
     * both navigates and polls is rare and either label is acceptable.
     */
    static void mark(String kind) {
        CURRENT.set(kind);
    }

    /**
     * Returns and clears the interaction kind for the current thread, or
     * {@code null} if none was marked.
     */
    static String take() {
        String value = CURRENT.get();
        CURRENT.remove();
        return value;
    }

    /**
     * Clears any value left over from a previous request on this (pooled)
     * thread.
     */
    static void clear() {
        CURRENT.remove();
    }
}
