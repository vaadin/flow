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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;

/**
 * Something that runs on the client when a {@link Trigger} fires.
 * <p>
 * Actions are the unit of behaviour you attach to a trigger: copy text to the
 * clipboard, download a file, scroll an element into view, and so on. Pair an
 * Action with a {@link Trigger} (typically via
 * {@code trigger.triggers(action)}) and the framework wires the action to run
 * whenever the trigger fires.
 * <p>
 * An Action usually consumes one or more {@linkplain Input inputs} that supply
 * its values — a literal, the current value of a DOM property, an event-scoped
 * expression — so the data the action acts on is read on the client at fire
 * time rather than captured on the server. A trigger family may expose its
 * event state as one or more {@code Input}s, typically as
 * {@code public static final} fields (for example
 * {@link MouseEventTrigger.EventData#screenX}); other inputs read state
 * independent of any trigger (for example {@link PropertyInput}).
 * <p>
 * <em>For Action implementors:</em> override {@link #toJs(Trigger)} to produce
 * the JavaScript that the trigger handler invokes; reference inputs through
 * {@link Input#toJs(Trigger)} so the same value-supplier abstractions work with
 * every action.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public abstract class Action implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Action.class);

    /**
     * Builds the {@link JsFunction} that runs this action when the surrounding
     * trigger fires. The returned function takes one runtime argument named
     * {@code event} (declared by the framework when it composes the trigger
     * handler); subclasses do not declare argument names themselves.
     * <p>
     * The body is one statement. To embed a value produced on the client,
     * capture an {@link Input}'s {@link Input#toJs(Trigger) JsFunction} as a
     * capture and invoke it inside the body as {@code $N(event)}.
     *
     * @param trigger
     *            the surrounding trigger this render is for, not {@code null}
     * @return the action's JS function, not {@code null}
     */
    protected abstract JsFunction toJs(Trigger trigger);

    /**
     * Logs a warning if {@code target} is not visible. Intended for actions
     * that capture another component's element as a JsFunction reference: an
     * invisible component is not sent to the client, the captured element
     * reference resolves to {@code null} when the install JS runs, and the
     * action fails at fire time. JsFunction captures are bound by value at
     * install time, so restoring visibility later does not recover the binding.
     * <p>
     * Call this after the target is known to be attached (typically inside a
     * {@code runWhenAttached} callback) so the check sees the visibility state
     * at the time the install JS would be sent to the client.
     *
     * @param target
     *            the component whose visibility to check, not {@code null}
     * @param actionDescription
     *            human-readable name of the action being wired, used in the
     *            warning message (e.g. {@code "Fullscreen.enter()"}), not
     *            {@code null}
     */
    protected static void warnIfNotVisible(Component target,
            String actionDescription) {
        if (!ComponentUtil.isEffectivelyVisible(target)) {
            LOGGER.warn(
                    "Target component {} is not visible; {} will not work. "
                            + "Make the component visible before wiring the "
                            + "trigger.",
                    target.getClass().getName(), actionDescription);
        }
    }

    /**
     * Renders the boilerplate shared by every "apply X, then undo X" action:
     * stashes the original value on the first fire of a cycle, coalesces rapid
     * re-fires (clearing the previous revert timer instead of re-snapshotting
     * the already-modified state), and reverts the stashed value when the timer
     * expires.
     * <p>
     * Subclasses provide three payload-specific sub-functions:
     * <ul>
     * <li>{@code snapshot} — no arguments, returns the value to stash; only
     * called on the first fire of a cycle.</li>
     * <li>{@code apply} — one argument named {@code event}, performs the
     * temporary change; called on every fire.</li>
     * <li>{@code revert} — one argument named {@code original}, restores the
     * stashed value; called once per cycle when the timer expires.</li>
     * </ul>
     *
     * The per-element stash is keyed by {@code stashKey}, so two temporary
     * modifications on the same element with different keys (e.g. different
     * property names) do not interfere; two with the same key (e.g. two
     * triggers both flashing the same property) share an original and a single
     * revert deadline.
     *
     * @param stashElement
     *            element to hang the stash off — typically the modified target,
     *            not {@code null}
     * @param stashKey
     *            identifier of what is being modified (e.g. property name);
     *            separates unrelated temporary modifications on the same
     *            element, not {@code null}
     * @param snapshot
     *            reads the current value to stash, no arguments, not
     *            {@code null}
     * @param apply
     *            performs the change, declares one argument named
     *            {@code event}, not {@code null}
     * @param revert
     *            restores the stashed value, declares one argument named
     *            {@code original}, not {@code null}
     * @param timeoutMillis
     *            delay before reverting, in milliseconds; must be non-negative
     * @return a {@link JsFunction} with one runtime argument named
     *         {@code event}, suitable for returning from an action's
     *         {@link #toJs(Trigger)}, not {@code null}
     */
    protected static JsFunction applyTemporarily(Element stashElement,
            String stashKey, JsFunction snapshot, JsFunction apply,
            JsFunction revert, long timeoutMillis) {
        // Delegate to the TS module: keeps non-trivial client-side plumbing
        // (per-element stash, timer coalescing, revert) in TriggerActions.ts
        // where it can be evolved without per-action churn, while the Java
        // side stays a one-line capture of the action's payload functions.
        return JsFunction
                .of("""
                        window.Vaadin.Flow.triggerActions.applyTemporarily(\
                        $0, $1, { snapshot: $2, apply: $3, revert: $4, timeoutMs: $5 }, event)""",
                        stashElement, stashKey, snapshot, apply, revert,
                        timeoutMillis)
                .withArguments("event");
    }

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
         * @param trigger
         *            the surrounding trigger this render is for, not
         *            {@code null}
         * @return the input's JS function, not {@code null}
         */
        protected abstract JsFunction toJs(Trigger trigger);
    }
}
