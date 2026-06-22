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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.shared.Registration;

/**
 * Something that fires on the client when some condition is met — a DOM event,
 * a signal change, an observer firing, a timer elapsing, an idle timeout, a
 * {@code BroadcastChannel} message, a media-query match, … — and, when it does,
 * runs one or more {@link Action actions}.
 * <p>
 * Actions run synchronously inside the browser's handler when the trigger
 * fires; for triggers that originate from a user gesture (click, keypress, …)
 * this preserves the gesture context for downstream actions, letting them
 * invoke browser APIs that require it (clipboard, fullscreen, file picker,
 * share, …). Note that most such APIs are themselves asynchronous: the action
 * is dispatched synchronously, but any server-observable effect — for example,
 * a callback reporting whether {@code navigator.clipboard.writeText} resolved —
 * may reach the server arbitrarily later than the gesture itself, after one or
 * more event-loop turns.
 * <p>
 * A trigger only does something once it is armed with an action: every trigger
 * is expected to have an action committed to it — via
 * {@link #triggers(Action...)} or its deferred
 * {@link #triggers(Component, SerializableSupplier)} overload — during the same
 * server visit that creates it. A trigger left unarmed is treated as a
 * programming error and reported with an {@link IllegalStateException} when the
 * client response is built.
 * <p>
 * Each {@link Action} passed to {@link #triggers(Action...)} produces one
 * {@link Element#addJsInitializer addJsInitializer} registration on the host
 * element via {@link #install(JsFunction)} — so a call with N actions yields N
 * registrations, all detached by {@link #remove()}. Subclasses implement
 * {@code install} to wire the rendered {@link JsFunction} to whatever client
 * API the trigger wraps (typically passing it to {@code addJsInitializer}
 * alongside whatever literal values the install expression needs).
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public abstract class Trigger implements Serializable {

    private final Element host;
    private final List<Registration> registrations = new ArrayList<>();

    // Whether an action has been committed to this trigger, whether wired
    // straight away by triggers(Action...) or scheduled by the deferred
    // triggers(Component, SerializableSupplier) overload. The unarmed-trigger
    // check reads this rather than the registration list so a legitimately
    // deferred wiring is not mistaken for a forgotten one.
    private boolean armed;

    /**
     * Creates a new trigger bound to the given host component's root element.
     *
     * @param host
     *            the component whose root element the trigger fires on, not
     *            {@code null}
     */
    protected Trigger(Component host) {
        this.host = Objects.requireNonNull(host).getElement();
        verifyArmedBeforeClientResponse();
    }

    /**
     * Schedules a check, run once the host is attached and just before the
     * client response is built, that fails if no action was committed to this
     * trigger via {@link #triggers(Action...)} or its deferred
     * {@link #triggers(Component, SerializableSupplier)} overload.
     * <p>
     * A trigger with no action does nothing on the client, so an unarmed
     * trigger is almost always a bug — typically a fluent binding whose
     * terminal action call was forgotten (for example
     * {@code Clipboard.onClick(button)} with no following
     * {@code writeText(…)}). The check is deferred to
     * {@code beforeClientResponse} rather than run from the constructor because
     * the action is committed in a separate call right after construction;
     * deferring lets that call happen first. The check looks at whether an
     * action was <em>committed</em>, not whether it has been wired yet, so the
     * deferred {@code triggers} overload — which records the action now but
     * wires it only once a target component attaches, possibly in a later round
     * trip — passes from the moment it is called.
     */
    private void verifyArmedBeforeClientResponse() {
        StateNode node = host.getNode();
        node.runWhenAttached(ui -> ui.getInternals().getStateTree()
                .beforeClientResponse(node, context -> {
                    if (!armed) {
                        throw new IllegalStateException("A "
                                + getClass().getSimpleName()
                                + " was created but no action was assigned to "
                                + "it, so it does nothing. Assign at least one "
                                + "action to the trigger when you create it — "
                                + "for example "
                                + "Clipboard.onClick(button).writeText(field) "
                                + "rather than Clipboard.onClick(button) on its "
                                + "own.");
                    }
                }));
    }

    /**
     * The host element this trigger fires on.
     *
     * @return the host element, never {@code null}
     */
    public final Element getHost() {
        return host;
    }

    /**
     * Wires the given actions to this trigger. They run in the order given the
     * next time this trigger fires. Each call adds another wiring; the existing
     * ones are kept.
     *
     * @param actions
     *            the actions to run, not {@code null} or empty
     */
    public final void triggers(Action... actions) {
        Objects.requireNonNull(actions);
        if (actions.length == 0) {
            throw new IllegalArgumentException(
                    "At least one action is required");
        }
        armed = true;
        for (Action action : actions) {
            Objects.requireNonNull(action, "Action must not be null");
            registrations.add(Objects.requireNonNull(install(action.toJs(this)),
                    "install must return a Registration"));
        }
    }

    /**
     * Deferred variant of {@link #triggers(Action...)}: builds the action via
     * {@code action} and wires it once {@code attachTarget} is attached to a UI
     * — immediately if it already is. Use this overload when the action can
     * only be built after some component is attached (for example because the
     * action needs the UI's wrapper element, or wants to inspect the target's
     * visibility as it will be at install time), so the {@link Action} cannot
     * be constructed up front and handed to {@link #triggers(Action...)}.
     * <p>
     * The trigger counts as armed from the moment this method is called, so the
     * unarmed-trigger check is satisfied straight away even though the actual
     * wiring waits for the attach — exactly what distinguishes a legitimately
     * deferred binding from a forgotten one.
     *
     * @param attachTarget
     *            the component whose attach gates the wiring, not {@code null}
     * @param action
     *            supplies the action to wire once {@code attachTarget} is
     *            attached, not {@code null}
     */
    public final void triggers(Component attachTarget,
            SerializableSupplier<Action> action) {
        Objects.requireNonNull(attachTarget, "attachTarget must not be null");
        Objects.requireNonNull(action, "action must not be null");
        // Record intent now so the deferred wiring is not flagged as forgotten;
        // the actual triggers(...) call (which also sets armed) runs at attach.
        armed = true;
        attachTarget.getElement().getNode()
                .runWhenAttached(ui -> triggers(action.get()));
    }

    /**
     * Installs the given rendered action as a client-side listener and returns
     * the {@link Registration} that detaches it. Called once per action passed
     * to {@link #triggers(Action...)}.
     * <p>
     * Implementations typically call
     * {@link Element#addJsInitializer(String, Object...)
     * getHost().addJsInitializer} with an install expression that hands the
     * action {@link JsFunction} to whatever client API the trigger wraps:
     *
     * <pre>{@code
     * return getHost().addJsInitializer(
     *         "this.addEventListener($1, $0);"
     *                 + "return () => this.removeEventListener($1, $0);",
     *         action, eventName);
     * }</pre>
     *
     * The expression runs with {@code this} bound to the host element. The
     * captures are made available as {@code $0}, {@code $1}, … in the order
     * passed.
     *
     * @param action
     *            the rendered action {@link JsFunction}; takes one runtime
     *            argument named {@code event} (the trigger's event payload),
     *            not {@code null}
     * @return the registration whose {@link Registration#remove()} detaches the
     *         listener, not {@code null}
     */
    protected abstract Registration install(JsFunction action);

    /**
     * Removes this trigger and all wirings created from it. The corresponding
     * client-side listeners are detached as part of the next synchronisation.
     */
    public final void remove() {
        registrations.forEach(Registration::remove);
        registrations.clear();
    }
}
