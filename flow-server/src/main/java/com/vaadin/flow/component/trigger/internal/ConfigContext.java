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

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.Argument;
import com.vaadin.flow.dom.Element;

/**
 * Passed into {@code buildClientConfig} so trigger/action/argument subclasses
 * can ship their JSON config and reference other arguments/elements by stable
 * id without needing direct access to the host's {@link TriggerSupport}.
 * <p>
 * Values written via {@link #put(String, Object)} accept any JSON-serialisable
 * Java type (String, Boolean, Number, {@code null}, {@code Map}, {@code List},
 * or any Jackson-serialisable POJO); the framework converts them to JSON
 * internally so extension authors never need to touch Jackson types.
 * <p>
 * For internal use only.
 */
public interface ConfigContext extends Serializable {

    /**
     * Writes a key/value entry into the current config object.
     *
     * @param key
     *            the key, not {@code null}
     * @param value
     *            the value — any JSON-serialisable Java type, or {@code null}
     * @return this context, for chaining
     */
    ConfigContext put(String key, @Nullable Object value);

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
}
