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
package com.vaadin.flow.component.trigger;

import java.io.Serializable;

/**
 * Something that fires on the client when some condition is met — a DOM event,
 * a signal change, an observer firing, a timer elapsing, an idle timeout, a
 * {@code BroadcastChannel} message, a media-query match, … — and, when it does,
 * runs one or more {@link Action actions}.
 * <p>
 * A trigger is bound to a host component. It is created with one of the
 * built-in subclasses or with an add-on subclass of {@link AbstractTrigger},
 * and then wired to actions via {@link #triggers}:
 *
 * <pre>{@code
 * Trigger click = new DomEventTrigger(button, "click");
 * click.triggers(new ClipboardCopyAction(
 *         new PropertyArgument<>(textField, "value", String.class)));
 * }</pre>
 *
 * Calling {@code triggers} more than once is additive — every subsequent call
 * adds another binding to the same trigger.
 * <p>
 * Triggers and actions run client-side without a server round-trip. An action
 * may still cause one if it has a server-observable effect (e.g. reporting back
 * whether a browser API call succeeded).
 * <p>
 * Actions run synchronously when the trigger fires; for triggers that originate
 * from a user gesture (click, keypress, …) this preserves the gesture context
 * for downstream actions, letting them invoke browser APIs that require it
 * (clipboard, fullscreen, file picker, share, …).
 */
public interface Trigger extends Serializable {

    /**
     * Wires the given actions to this trigger. They run in the order given the
     * next time this trigger fires.
     *
     * @param actions
     *            the actions to run, not {@code null}
     * @return this trigger, for chaining
     */
    Trigger triggers(Action... actions);

    /**
     * Removes this trigger and all bindings created from it. The corresponding
     * client-side handlers are detached as part of the next synchronisation.
     */
    void remove();
}
