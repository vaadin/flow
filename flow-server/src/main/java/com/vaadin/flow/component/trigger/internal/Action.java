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

import com.vaadin.flow.dom.JsFunction;

/**
 * Something that runs on the client when a {@link Trigger} fires.
 * <p>
 * Each Action {@linkplain #render renders} into a {@link JsFunction}: a body of
 * JavaScript plus a list of captures (element references, value literals,
 * nested {@link JsFunction}s) referenced positionally as {@code $0},
 * {@code $1}, … The framework composes the per-Action functions into the
 * trigger's handler — Actions do not concatenate JS source with anything
 * outside their own body.
 * <p>
 * The rendered function takes one runtime argument, {@code event}, which is the
 * DOM event (or other trigger payload) that fired the trigger. Inputs that need
 * it ({@link HandlerInput}) read it; Inputs that don't simply ignore it.
 * <p>
 * Actions are intentionally one-shot: each {@link #render} produces a single
 * statement worth of JS. Multi-statement logic belongs in a
 * {@code window.Vaadin.Flow.*} helper in {@code flow-client} that the Action
 * calls into (see {@code Clipboard.ts}, {@code Download.ts}).
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public abstract class Action implements Serializable {

    /**
     * Builds the {@link JsFunction} that runs this action when the surrounding
     * trigger fires. The returned function takes one runtime argument named
     * {@code event} (declared by the framework when it composes the trigger
     * handler); subclasses do not declare argument names themselves.
     * <p>
     * The body is one statement. To embed a value produced on the client,
     * capture an {@link Input}'s {@link Input#toJs(JsBuilder) JsFunction} as a
     * capture and invoke it inside the body as {@code $N(event)}.
     *
     * @param builder
     *            render-time context exposing the surrounding {@link Trigger};
     *            not {@code null}
     * @return the action's JS function, not {@code null}
     */
    protected abstract JsFunction render(JsBuilder builder);

    /**
     * A value an {@link Action} consumes at fire time. Renders into a
     * {@link JsFunction} that, when called, evaluates to the input's value — a
     * literal, the current value of a DOM property, an event-scoped expression,
     * anything the producer chooses.
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
         * Builds the {@link JsFunction} that yields this input's value when
         * called. The function may take {@code event} as a runtime argument
         * (declared by the subclass via
         * {@link JsFunction#withArguments(String...)}); inputs that don't need
         * {@code event} simply omit the declaration and ignore the argument the
         * caller passes.
         *
         * @param builder
         *            render-time context exposing the surrounding
         *            {@link Trigger}; not {@code null}
         * @return the input's JS function, not {@code null}
         */
        protected abstract JsFunction toJs(JsBuilder builder);
    }
}
