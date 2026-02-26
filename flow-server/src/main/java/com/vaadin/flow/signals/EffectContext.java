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
package com.vaadin.flow.signals;

import java.io.Serializable;

/**
 * Provides context information about why a signal effect is running. This
 * allows effect callbacks to distinguish between the initial execution, updates
 * triggered by user requests, and updates triggered by background changes (such
 * as server push or other users modifying shared signals).
 * <p>
 * Typical usage:
 *
 * <pre>
 * Signal.effect(this, ctx -&gt; {
 *     span.getElement().setText("$" + price.get());
 *     if (ctx.isBackgroundChange()) {
 *         span.getElement().flashClass("highlight");
 *     }
 * });
 * </pre>
 */
public class EffectContext implements Serializable {

    private final boolean initialRun;
    private final boolean backgroundChange;

    /**
     * Creates a new effect context.
     *
     * @param initialRun
     *            whether this is the first execution of the effect
     * @param backgroundChange
     *            whether the invalidation that triggered this execution
     *            happened outside a user request context
     */
    public EffectContext(boolean initialRun, boolean backgroundChange) {
        this.initialRun = initialRun;
        this.backgroundChange = backgroundChange;
    }

    /**
     * Returns whether this is the very first execution of the effect. The
     * initial run happens when the effect is first created (or when a bound
     * component is first attached).
     *
     * @return {@code true} if this is the first execution, {@code false}
     *         otherwise
     */
    public boolean isInitialRun() {
        return initialRun;
    }

    /**
     * Returns whether this execution was triggered by a background change
     * rather than by a user request or the initial render. A background change
     * occurs when a signal is modified outside of a user request context, for
     * example from a background thread using
     * {@link com.vaadin.flow.component.UI#access(com.vaadin.flow.server.Command)
     * UI.access()}, from server push, or from another user modifying a shared
     * signal.
     * <p>
     * This is {@code false} during the initial run (even if there is no active
     * request) and {@code false} during a normal user request. It is
     * {@code true} only when the effect re-runs due to a signal change that
     * happened outside any user request.
     * <p>
     * Note: async user actions (user click leading to async work, then
     * {@code ui.access(() -> signal.set(...))}) are classified as background
     * changes because by the time the async result arrives, there is no active
     * request.
     *
     * @return {@code true} if triggered by a background change, {@code false}
     *         otherwise
     */
    public boolean isBackgroundChange() {
        return !initialRun && backgroundChange;
    }
}
