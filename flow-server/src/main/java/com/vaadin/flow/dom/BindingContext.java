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
package com.vaadin.flow.dom;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.signals.EffectContext;

/**
 * Provides rich context information about a signal binding update. Extends
 * {@link EffectContext} with binding-specific information such as old and new
 * values, target element, and nearest component.
 * <p>
 * Typical usage:
 *
 * <pre>
 * span.bindText(signal).onChange(ctx -&gt; {
 *     if (ctx.isBackgroundChange()) {
 *         ctx.getElement().flashClass("highlight");
 *     }
 * });
 * </pre>
 *
 * @param <T>
 *            the type of the bound signal value
 */
public class BindingContext<T extends @Nullable Object> extends EffectContext {

    private final T oldValue;
    private final T newValue;
    private final Element element;

    /**
     * Creates a new binding context.
     *
     * @param initialRun
     *            whether this is the first execution of the binding effect
     * @param oldValue
     *            the previous value (same as newValue on initial run)
     * @param newValue
     *            the current value
     * @param element
     *            the target element of the binding
     */
    public BindingContext(boolean initialRun, T oldValue, T newValue,
            Element element) {
        super(initialRun);
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.element = element;
    }

    /**
     * Returns the previous value before this update. On the initial run, this
     * is the same as {@link #getNewValue()}.
     *
     * @return the old value
     */
    public T getOldValue() {
        return oldValue;
    }

    /**
     * Returns the current value after this update.
     *
     * @return the new value
     */
    public T getNewValue() {
        return newValue;
    }

    /**
     * Returns the target element that the signal is bound to.
     *
     * @return the target element, not {@code null}
     */
    public Element getElement() {
        return element;
    }

    /**
     * Returns the nearest component for the target element. If the element
     * itself has an associated component, that component is returned. Otherwise,
     * the element tree is traversed upward to find the first ancestor with an
     * associated component.
     *
     * @return the nearest component, or {@code null} if no component is found
     *         in the element hierarchy
     */
    public @Nullable Component getComponent() {
        return ComponentUtil.findParentComponent(element).orElse(null);
    }
}
