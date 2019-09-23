/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import java.util.Set;

import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.flow.shared.Registration;

/**
 * A registration for configuring or removing a DOM event listener added to an
 * element.
 *
 * @see Element#addEventListener(String, DomEventListener)
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface DomListenerRegistration extends Registration {
    /**
     * Add a JavaScript expression for extracting event data. When an event is
     * fired in the browser, the expression is evaluated and its value is sent
     * back to the server. The expression is evaluated in a context where
     * <code>element</code> refers to this element and <code>event</code> refers
     * to the fired event. If multiple expressions are defined for the same
     * event, their order of execution is undefined.
     * <p>
     * The result of the evaluation is available in
     * {@link DomEvent#getEventData()} with the expression as the key in the
     * JSON object. An expression might be e.g.
     *
     * <ul>
     * <li><code>element.value</code> the get the value of an input element for
     * a change event.
     * <li><code>event.button === 0</code> to get true for click events
     * triggered by the primary mouse button.
     * </ul>
     *
     * @param eventData
     *            definition for data that should be passed back to the server
     *            together with the event, not <code>null</code>
     * @return this registration, for chaining
     */
    DomListenerRegistration addEventData(String eventData);

    /**
     * Sets a JavaScript expression that is used for filtering events to this
     * listener. When an event is fired in the browser, the expression is
     * evaluated and an event is sent to the server only if the expression value
     * is <code>true</code>-ish according to JavaScript type coercion rules. The
     * expression is evaluated in a context where <code>element</code> refers to
     * this element and <code>event</code> refers to the fired event.
     * <p>
     * An expression might be e.g. <code>event.button === 0</code> to only
     * forward events triggered by the primary mouse button.
     * <p>
     * Any previous filter for this registration is discarded.
     *
     * @param filter
     *            the JavaScript filter expression, or <code>null</code> to
     *            clear the filter
     * @return this registration, for chaining
     */
    DomListenerRegistration setFilter(String filter);

    /**
     * Gets the currently set filter expression.
     *
     * @see #setFilter(String)
     *
     * @return the current filter expression, or <code>null</code> if no filter
     *         is in use
     */
    String getFilter();

    /**
     * Configure whether this listener will be called even in cases when the
     * element is disabled.
     * <p>
     * When used in combination with {@link #synchronizeProperty(String)}, the
     * most permissive update mode for the same property will be effective. This
     * means that there might be unexpected property updates for a disabled
     * component if multiple parties independently configure different aspects
     * for the same component. This is based on the assumption that if a
     * property is explicitly safe to update for disabled components in one
     * context, then the nature of that property is probably such that it's also
     * safe to update in other contexts.
     *
     * @param disabledUpdateMode
     *            controls RPC communication from the client side to the server
     *            side when the element is disabled, not {@code null}
     *
     * @return this registration, for chaining
     */
    DomListenerRegistration setDisabledUpdateMode(
            DisabledUpdateMode disabledUpdateMode);

    /**
     * Configures the debouncing phases for which this listener should be
     * triggered. Debouncing is disabled and the set phases are ignored if
     * <code>timeout</code> is set to 0.
     * <p>
     * This methods overrides the settings previously set through
     * {@link #debounce(int)}, {@link #throttle(int)} or
     * {@link #debounce(int, DebouncePhase, DebouncePhase...)}.
     *
     * @see DebouncePhase
     *
     * @param timeout
     *            the debounce timeout in milliseconds, or 0 to disable
     *            debouncing
     * @param firstPhase
     *            the first phase to use
     * @param rest
     *            any remaining phases to use
     * @return this registration, for chaining
     */
    // (first, ... rest) looks slightly weird, but is needed for EnumSet.of
    DomListenerRegistration debounce(int timeout, DebouncePhase firstPhase,
            DebouncePhase... rest);

    /**
     * Configures this listener to be notified only when at least
     * <code>timeout</code> milliseconds has passed since the last time the
     * event was triggered. This is useful for cases such as text input where
     * it's only relevant to know the result once the user stops typing.
     * <p>
     * Debouncing is disabled if the <code>timeout</code> is set to 0.
     * <p>
     * This methods overrides the settings previously set through
     * {@link #debounce(int)}, {@link #throttle(int)} or
     * {@link #debounce(int, DebouncePhase, DebouncePhase...)}.
     *
     * @param timeout
     *            the debounce timeout in milliseconds, or 0 to disable
     *            debouncing
     * @return this registration, for chaining
     */
    default DomListenerRegistration debounce(int timeout) {
        return debounce(timeout, DebouncePhase.TRAILING);
    }

    /**
     * Configures this listener to not be notified more often than
     * <code>period</code> milliseconds.
     * <p>
     * Throttling is disabled if the <code>period</code> is set to 0.
     * <p>
     * This methods overrides the settings previously set through
     * {@link #debounce(int)}, {@link #throttle(int)} or
     * {@link #debounce(int, DebouncePhase, DebouncePhase...)}.
     *
     * @param period
     *            the minimum period between listener invocations, or 0 to
     *            disable throttling
     * @return this registration, for chaining
     */
    default DomListenerRegistration throttle(int period) {
        return debounce(period, DebouncePhase.LEADING,
                DebouncePhase.INTERMEDIATE);
    }

    /**
     * Gets the debounce timeout that is configured by debounce or throttle.
     *
     * @see #debounce(int, DebouncePhase, DebouncePhase...)
     * @see #debounce(int)
     * @see #throttle(int)
     *
     * @return timeout in milliseconds,
     *         or <code>0</code> if debouncing is disabled
     */
    default int getDebounceTimeout() {
        /*
         * Dummy backwards compatibility implementation to keep old custom code
         * compiling.
         */
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the debouncing phases for which this listener should be triggered.
     *
     * @see #debounce(int, DebouncePhase, DebouncePhase...)
     * @see #debounce(int)
     * @see #throttle(int)
     *
     * @return debounce phases
     */
    default Set<DebouncePhase> getDebouncePhases() {
        /*
         * Dummy backwards compatibility implementation to keep old custom code
         * compiling.
         */
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the event type that the listener is registered for.
     *
     * @return DOM event type of the listener
     */
    default String getEventType() {
        /*
         * Dummy backwards compatibility implementation to keep old custom code
         * compiling.
         */
        throw new UnsupportedOperationException();
    }

    /**
     * Adds a handler that will be run when this registration is removed.
     *
     * @param unregisterHandler
     *            the handler to run when the registration is removed, not
     *            <code>null</code>
     * @return this registration, for chaining
     *
     * @since 1.3
     */
    default DomListenerRegistration onUnregister(
            SerializableRunnable unregisterHandler) {
        /*
         * Dummy backwards compatibility implementation to keep old custom code
         * compiling, even though custom implementations won't work with the new
         * addPropertyListener overload that uses this method.
         */
        throw new UnsupportedOperationException();
    }

    /**
     * Marks that the DOM event of this registration should trigger
     * synchronization for the given property.
     *
     * @return this registration, for chaining
     *
     * @param propertyName
     *            the name of the property to synchronize, not <code>null</code>
     *            or <code>""</code>
     * @return this registration, for chaining
     */
    default DomListenerRegistration synchronizeProperty(String propertyName) {
        if (propertyName == null || propertyName.isEmpty()) {
            throw new IllegalArgumentException("Property name must be given");
        }
        return addEventData(
                JsonConstants.SYNCHRONIZE_PROPERTY_TOKEN + propertyName);
    }
}
