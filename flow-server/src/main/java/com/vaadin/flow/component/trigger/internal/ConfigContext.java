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
import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.Argument;
import com.vaadin.flow.dom.Element;

/**
 * Context passed into {@code buildClientConfig} so trigger/action/argument
 * subclasses can reference other arguments and elements by stable id without
 * needing direct access to the host's {@link TriggerSupport}.
 * <p>
 * For internal use only.
 */
public interface ConfigContext extends Serializable {

    /**
     * Returns a stable id for the given argument, registering it with the
     * host's TriggerSupport if it hasn't been registered yet.
     *
     * @param argument
     *            the argument to reference, not {@code null}
     * @return the id of the argument in the surrounding snapshot
     */
    int registerArgument(Argument<?> argument);

    /**
     * Returns a stable parameter index for the given element. Host element is
     * index {@code 0} ({@code this} in the executeJs invocation); other
     * elements get sequential indices starting at {@code 1}.
     *
     * @param element
     *            the element to reference, not {@code null}
     * @return the parameter index
     */
    int referenceElement(Element element);

    /**
     * Returns a stable parameter index for the given component's root element.
     *
     * @param component
     *            the component to reference, not {@code null}
     * @return the parameter index
     */
    default int referenceElement(Component component) {
        return referenceElement(Objects.requireNonNull(component).getElement());
    }

    /**
     * The host element this snapshot belongs to. Useful for arguments that
     * install element-scoped subscriptions (e.g. {@code SignalArgument} via
     * {@link com.vaadin.flow.dom.ElementEffect#effect}).
     *
     * @return the host element
     */
    Element getHost();

    /**
     * Schedules a fresh client snapshot for the host to be emitted on the next
     * {@code beforeClientResponse} flush. Used by arguments whose value may
     * change between trigger fires (e.g. a {@code SignalArgument}). Idempotent
     * within a request.
     */
    void scheduleSync();
}
