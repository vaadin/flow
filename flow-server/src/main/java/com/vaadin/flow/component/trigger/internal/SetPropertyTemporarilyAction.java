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

import java.time.Duration;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;

/**
 * Assigns a value to a JavaScript property on a target element when the bound
 * trigger fires, then reverts the property to its previous value after a
 * configurable timeout. Pure client-side — no server round-trip.
 * <p>
 * The "previous" value is read on the client at fire time, immediately before
 * the new value is assigned, and the reversion is scheduled via
 * {@code setTimeout}. Each firing captures its own previous value and schedules
 * its own reversion; overlapping firings within the timeout window are not
 * coalesced.
 * <p>
 * The default timeout is {@value #DEFAULT_TIMEOUT_MS} ms
 * ({@link #DEFAULT_TIMEOUT}). Passing {@link Duration#ZERO} is allowed and
 * queues the reversion onto the next event-loop turn — the new value is briefly
 * visible before reverting, which is a legitimate "flash" / "pulse" pattern.
 * <p>
 * Symmetric with {@link SetPropertyAction}, which applies the value
 * permanently. The value to assign can be either a literal (constant,
 * serialised at build time) or an {@link Action.Input} that produces the value
 * on the client when the trigger fires.
 * <p>
 * Common idioms:
 * <ul>
 * <li>Flash a button into a "copied" state for one second:
 * {@code new SetPropertyTemporarilyAction(button, "innerText", "Copied!")}
 * <li>Mark an input invalid for half a second:
 * {@code new SetPropertyTemporarilyAction(input, "invalid", true,
 * Duration.ofMillis(500))}
 * </ul>
 *
 * Server-side state is not updated by this action; the changes (both the
 * temporary assignment and the reversion) live in the browser until the next
 * sync from the client (if any).
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @param <T>
 *            the runtime type of the value to assign
 */
public class SetPropertyTemporarilyAction<T> extends Action {

    private static final long DEFAULT_TIMEOUT_MS = 1000L;

    /**
     * Default timeout used by constructors that don't take an explicit
     * {@link Duration}: one second.
     */
    public static final Duration DEFAULT_TIMEOUT = Duration
            .ofMillis(DEFAULT_TIMEOUT_MS);

    /**
     * Singleton input that yields a JS {@code null}. Used by the null-accepting
     * convenience constructor so {@link LiteralInput} can stay non-null.
     */
    private static final Action.Input<Object> NULL_LITERAL = new Action.Input<>() {
        @Override
        protected JsFunction toJs(Trigger trigger) {
            return JsFunction.of("return null");
        }
    };

    @SuppressWarnings("unchecked")
    private static <T> Action.Input<T> nullLiteral() {
        return (Action.Input<T>) NULL_LITERAL;
    }

    private final Element target;
    private final String propertyName;
    private final Action.Input<? extends T> source;
    private final long timeoutMillis;

    /**
     * Creates an action that temporarily assigns the given literal value to the
     * given JS property on {@code target} when the trigger fires, reverting
     * after {@link #DEFAULT_TIMEOUT}.
     *
     * @param target
     *            the component whose root element to modify, not {@code null}
     * @param propertyName
     *            the JS property name, not {@code null}
     * @param value
     *            the value to assign temporarily — {@code String},
     *            {@code Boolean}, {@code Number}, or any Jackson-serialisable
     *            object; may be {@code null} to emit a JS {@code null}
     */
    public SetPropertyTemporarilyAction(Component target, String propertyName,
            @Nullable T value) {
        this(target, propertyName, value, DEFAULT_TIMEOUT);
    }

    /**
     * Creates an action that temporarily assigns the given literal value to the
     * given JS property on {@code target} when the trigger fires, reverting
     * after {@code timeout}.
     *
     * @param target
     *            the component whose root element to modify, not {@code null}
     * @param propertyName
     *            the JS property name, not {@code null}
     * @param value
     *            the value to assign temporarily — {@code String},
     *            {@code Boolean}, {@code Number}, or any Jackson-serialisable
     *            object; may be {@code null} to emit a JS {@code null}
     * @param timeout
     *            how long to keep the value before reverting, not {@code null}
     *            and not negative; {@link Duration#ZERO} is allowed
     */
    public SetPropertyTemporarilyAction(Component target, String propertyName,
            @Nullable T value, Duration timeout) {
        this(target, propertyName,
                value == null ? nullLiteral() : new LiteralInput<>(value),
                timeout);
    }

    /**
     * Creates an action that temporarily assigns the value produced by
     * {@code source} to the given JS property on {@code target} when the
     * trigger fires, reverting after {@link #DEFAULT_TIMEOUT}.
     *
     * @param target
     *            the component whose root element to modify, not {@code null}
     * @param propertyName
     *            the JS property name, not {@code null}
     * @param source
     *            input that produces the value to assign, not {@code null}
     */
    public SetPropertyTemporarilyAction(Component target, String propertyName,
            Action.Input<? extends T> source) {
        this(target, propertyName, source, DEFAULT_TIMEOUT);
    }

    /**
     * Creates an action that temporarily assigns the value produced by
     * {@code source} to the given JS property on {@code target} when the
     * trigger fires, reverting after {@code timeout}.
     *
     * @param target
     *            the component whose root element to modify, not {@code null}
     * @param propertyName
     *            the JS property name, not {@code null}
     * @param source
     *            input that produces the value to assign, not {@code null}
     * @param timeout
     *            how long to keep the value before reverting, not {@code null}
     *            and not negative; {@link Duration#ZERO} is allowed
     */
    public SetPropertyTemporarilyAction(Component target, String propertyName,
            Action.Input<? extends T> source, Duration timeout) {
        this.target = Objects.requireNonNull(target).getElement();
        this.propertyName = Objects.requireNonNull(propertyName);
        this.source = Objects.requireNonNull(source);
        Objects.requireNonNull(timeout, "timeout must not be null");
        if (timeout.isNegative()) {
            throw new IllegalArgumentException(
                    "timeout must not be negative: " + timeout);
        }
        this.timeoutMillis = timeout.toMillis();
    }

    @Override
    protected JsFunction toJs(Trigger trigger) {
        // $0 = target element (captured), $1 = property name (Jackson-quoted
        // on the client), $2 = source JsFunction (invoked with event so
        // handler-scoped inputs work), $3 = timeout in ms (Jackson-encoded
        // number — never string-concatenated into the body).
        // The IIFE keeps the action a single statement (the base class
        // convention) while reading the previous value at fire time and
        // scheduling the reversion.
        return JsFunction.of("""
                (() => { const prev = $0[$1]; $0[$1] = $2(event); \
                setTimeout(() => { $0[$1] = prev; }, $3); })()""", target,
                propertyName, source.toJs(trigger), timeoutMillis)
                .withArguments("event");
    }
}
