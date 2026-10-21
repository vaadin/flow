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

import com.vaadin.flow.component.trigger.Action;
import com.vaadin.flow.component.trigger.Trigger;
import com.vaadin.flow.dom.JsFunction;

/**
 * A value an {@link Action} consumes at fire time. Renders into a
 * {@link JsFunction} that, when called, evaluates to the input's value — a
 * literal, the current value of a DOM property, an event-scoped expression,
 * anything the producer chooses.
 * <p>
 * Some inputs are bound to a specific trigger's handler scope (see
 * {@link MouseEventData}) and may only be used in actions wired to that
 * trigger; others are independent of any trigger (see {@link PropertyInput},
 * {@link LiteralInput}) and may be used freely.
 * <p>
 * For internal use only. May be renamed or removed in any release.
 *
 * @param <T>
 *            the runtime type of the value produced
 */
public abstract class ActionInput<T> implements Serializable {

    /**
     * Builds the {@link JsFunction} that yields this input's value when called.
     * The function may take {@code event} as a runtime argument (declared by
     * the subclass via {@link JsFunction#withArguments(String...)}); inputs
     * that don't need {@code event} simply omit the declaration and ignore the
     * argument the caller passes.
     * <p>
     * Public so that {@link Action} implementations in any package can consume
     * inputs they receive as parameters, not only inputs they created
     * themselves.
     *
     * @param trigger
     *            the surrounding trigger this render is for, not {@code null}
     * @return the input's JS function, not {@code null}
     */
    public abstract JsFunction toJs(Trigger trigger);
}
