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

import com.vaadin.flow.function.SerializableRunnable;

/**
 * Something that fires on the client and, when it does, runs one or more
 * {@link Action actions} synchronously inside the original DOM event handler.
 * <p>
 * A trigger is bound to a host {@link com.vaadin.flow.dom.Element element}. It
 * is created with one of the built-in subclasses or with an add-on subclass of
 * {@link AbstractTrigger}, and then wired to actions via {@link #triggers}:
 *
 * <pre>{@code
 * Trigger click = new ClickTrigger(button);
 * click.triggers(new ClipboardCopyAction(
 *         new PropertyArgument<>(textField, "value", String.class)));
 * }</pre>
 *
 * Calling {@code triggers} more than once is additive — every subsequent call
 * adds another binding to the same trigger.
 * <p>
 * Triggers and actions run client-side without a server round-trip. Actions may
 * still cause a server round-trip if they have a server-side effect (e.g.
 * updating a server-side property mirror) or if a server callback is attached
 * via {@link #triggers(SerializableRunnable)}.
 */
public interface Trigger extends Serializable {

    /**
     * Wires the given actions to this trigger. They run in the order given,
     * inside the original DOM event handler, the next time this trigger fires.
     *
     * @param actions
     *            the actions to run, not {@code null}
     * @return this trigger, for chaining
     */
    Trigger triggers(Action... actions);

    /**
     * Wires a server-side callback to this trigger. The callback runs after the
     * client-side dispatch of any other bound actions has finished. The
     * callback fires on the UI thread.
     * <p>
     * This is sugar for adding a {@code ServerCallbackAction} that wraps the
     * given runnable.
     *
     * @param serverHandler
     *            the runnable to execute on the server, not {@code null}
     * @return this trigger, for chaining
     */
    Trigger triggers(SerializableRunnable serverHandler);

    /**
     * Removes this trigger and all bindings created from it. The corresponding
     * client-side handlers are detached as part of the next synchronisation.
     */
    void remove();
}
