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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.trigger.internal.ActionInput;
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
 * Creating actions and wiring them to triggers is public API. Passing data from
 * a trigger or from the page into an action, and implementing a custom action,
 * is not yet: the input types and the methods that render an action to
 * JavaScript are for internal use only and may be changed or removed in any
 * release. Within the framework, implementors override {@link #toJs(Trigger)}
 * to produce the JavaScript that the trigger handler invokes.
 *
 * @since 25.4
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
     * capture an {@link ActionInput}'s {@link ActionInput#toJs(Trigger)
     * JsFunction} as a capture and invoke it inside the body as
     * {@code $N(event)}.
     * <p>
     * For internal use only. May be changed or removed in any release.
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
     * <p>
     * For internal use only. May be changed or removed in any release.
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
}
