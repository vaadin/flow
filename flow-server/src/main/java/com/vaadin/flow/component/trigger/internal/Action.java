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
package com.vaadin.flow.component.trigger.internal;

import java.io.Serializable;

/**
 * Something that runs on the client when a {@link Trigger} fires. Subclasses
 * append the JS that runs when the trigger fires by overriding
 * {@link #appendStatement(JsBuilder, StringBuilder)}.
 * <p>
 * Actions can take their value either from server-side literals or from an
 * {@link Input} — a deferred value expression evaluated in the trigger's
 * handler scope at fire time. A Trigger may expose its own state as one or more
 * {@code Input}s (for example {@link ClickTrigger#screenX()}); other subclasses
 * of {@code Input} read state independent of any trigger (for example
 * {@link PropertyInput}).
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public abstract class Action implements Serializable {

    /**
     * Appends this action's JS statement to {@code out}. Element references
     * must go through {@link JsBuilder#reference}.
     *
     * @param builder
     *            collects element parameter references, not {@code null}
     * @param out
     *            buffer to append into, not {@code null}
     */
    protected abstract void appendStatement(JsBuilder builder,
            StringBuilder out);

    /**
     * A value an {@link Action} consumes at fire time — a deferred JS
     * expression that yields a value when the surrounding trigger fires.
     * Subclasses produce the JS expression by overriding
     * {@link #appendExpression(JsBuilder, StringBuilder)}.
     * <p>
     * Some inputs are bound to a specific trigger's handler scope (see
     * {@link HandlerInput}) and may only be used in actions wired to that
     * trigger; others are independent of any trigger (see
     * {@link PropertyInput}, {@link LiteralInput}) and may be used freely.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     *
     * @param <T>
     *            the runtime type of the value produced
     */
    public abstract static class Input<T> implements Serializable {

        /**
         * Appends this input's JS expression to {@code out}. Element references
         * must go through {@link JsBuilder#reference}.
         *
         * @param builder
         *            collects element parameter references, not {@code null}
         * @param out
         *            buffer to append into, not {@code null}
         */
        protected abstract void appendExpression(JsBuilder builder,
                StringBuilder out);
    }
}
