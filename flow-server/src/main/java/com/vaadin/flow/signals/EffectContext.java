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
 * triggered by the effect owner's requests, and updates triggered by background
 * changes (such as a background thread or another user modifying a shared
 * signal).
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
     * Returns whether this execution was triggered by a signal change that
     * happened outside the effect owner's request context. A background change
     * occurs when a signal is modified from a background thread or by another
     * user modifying a shared signal.
     * <p>
     * This is {@code false} during the initial run (even if there is no active
     * request) and {@code false} when the signal was changed during a request
     * from the user who owns the effect. It is {@code true} when the signal
     * change originated from outside any request context for this user.
     * <p>
     * <strong>Note:</strong> This flag is a hint and may not be fully accurate
     * when multiple signal changes from different sources are batched into a
     * single effect invocation. The flag is guaranteed to not be set if none of
     * the changes are from a background context and guaranteed to be set if all
     * changes are from a background context. When changes come from mixed
     * sources, the state of the flag is arbitrary.
     *
     * @return {@code true} if triggered by a background change, {@code false}
     *         otherwise
     */
    public boolean isBackgroundChange() {
        return !initialRun && backgroundChange;
    }
}
