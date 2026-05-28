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

    /**
     * Creates a new trigger bound to the given host component's root element.
     *
     * @param host
     *            the component whose root element the trigger fires on, not
     *            {@code null}
     */
    protected Trigger(Component host) {
        this.host = Objects.requireNonNull(host).getElement();
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
        for (Action action : actions) {
            Objects.requireNonNull(action, "Action must not be null");
            registrations.add(Objects.requireNonNull(install(action.toJs(this)),
                    "install must return a Registration"));
        }
    }

    /**
     * Installs the given rendered action as a client-side listener and returns
     * the {@link Registration} that detaches it. Called once per action passed
     * to {@link #triggers(Action...)}.
     * <p>
     * Implementations typically call
     * {@link Element#addJsInitializer(JsFunction) getHost().addJsInitializer}
     * with a {@link JsFunction} whose body hands the action to whatever client
     * API the trigger wraps. Use
     * {@link JsFunction#withParameter(String, Object)} to expose the action and
     * any literal install values under readable names:
     *
     * <pre>{@code
     * return getHost().addJsInitializer(JsFunction
     *         .of("this.addEventListener(eventName, action);"
     *                 + "return () => this.removeEventListener(eventName, action);")
     *         .withParameter("action", action)
     *         .withParameter("eventName", eventName));
     * }</pre>
     *
     * The expression runs with {@code this} bound to the host element. Named
     * captures from {@link JsFunction#withParameter(String, Object)} are
     * exposed under the given names; positional captures from
     * {@link JsFunction#of(String, Object...)} are exposed as {@code $0},
     * {@code $1}, ….
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
