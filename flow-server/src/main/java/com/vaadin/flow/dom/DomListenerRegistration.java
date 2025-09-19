/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import java.util.Objects;
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
     * @see #addEventDataElement(String) for mapping an element
     * @see #mapEventTargetElement() to map the {@code event.target} to an
     *      element
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
     * <code>timeout</code> is set to 0 (the default).
     * <p>
     * This methods overrides the settings previously set through
     * {@link #debounce(int)}, {@link #throttle(int)} or
     * {@link #debounce(int, DebouncePhase, DebouncePhase...)}.
     * <p>
     * The tested and supported combinations of phases are:
     *
     * <dl>
     * <dt>DebouncePhase.TRAILING</dt>
     * <dd>The server side is notified once after the event hasn't been
     * triggered within the timeout. For example only the last keydown event is
     * fired if a person continuously types without pauses longer than the
     * timeout. This is the most commonly used mode. There is
     * {@link #debounce(int)} shorthand for this mode.</dd>
     * <dt>DebouncePhase.LEADING</dt>
     * <dd>In this case you only get notified of this event once right away on
     * the server side. Other events of the same type will be ignored until the
     * timeout has passed.</dd>
     * <dt>DebouncePhase.LEADING + DebouncePhase.TRAILING</dt>
     * <dd>This works like with the basic DebouncePhase.TRAILING, but in
     * addition the first event of the burst gets reported rightaway. This is
     * good if your want normal debouncing, but you are also interested to know
     * if the user input has started.</dd>
     * <dt>DebouncePhase.TIMEOUT</dt>
     * <dd>In this case the listener is triggered only after the given timeout.
     * If there are multiple events during that period, only the last one is
     * reported.</dd>
     * <dt>DebouncePhase.LEADING + DebouncePhase.TIMEOUT</dt>
     * <dd>In this case the listener is triggered when an event burst starts,
     * but afterwards only after the given timeout has passed. There is
     * {@link #throttle(int)} shorthand for this mode.</dd>
     * </dl>
     * <p>
     * In case another event in the UI triggers a "server roundtrip" from the
     * client-side, events possibly queued by the debouncer are fired before
     * that. Thus, events may occur before than expected. Also in the otherway
     * around, due to the client-server nature of the web apps, events may
     * arrive bit late. Do not expect debouncing to be perfectly deterministic!
     * <p>
     * Also note that due to the client-side implementation, de-bounce settings
     * are global for keys formed of "element-to-event-type-to-timeout".
     * Behavior is unspecified if you configure multiple debouncing rules for
     * the same event on the same element.
     *
     * @see DebouncePhase
     *
     * @param timeout
     *            the debounce timeout in milliseconds, or 0 to disable
     *            debouncing
     * @param firstPhase
     *            the first phase to use. If you are interested about the first
     *            event in the burst, you should give DebouncePhase.TRAILING as
     *            a parameter here. Otherwise either DebouncePhase.TRAILING or
     *            DebouncePhase.INTERMEDIATE.
     * @param rest
     *            any remaining phases to use. In practice, only either
     *            DebouncePhase.TRAILING and DebouncePhase.INTERMEDIATE should
     *            be given here, if DebouncePhase.LEADING is given as a
     *            firstPhase.
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
     * This is a shorthand for
     * {@link #debounce(int, DebouncePhase, DebouncePhase...)} with only
     * DebouncePhase.TRAILING specified. See
     * {@link #debounce(int, DebouncePhase, DebouncePhase...)} for complete
     * documentation!
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
     * This is a shorthand for
     * {@link #debounce(int, DebouncePhase, DebouncePhase...)} with
     * DebouncePhase.LEADING and DebouncePhase.INTERMEDIATE specified. See
     * {@link #debounce(int, DebouncePhase, DebouncePhase...)} for complete
     * documentation!
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
     * @return timeout in milliseconds, or <code>0</code> if debouncing is
     *         disabled
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

    /**
     * Marks that the DOM event should map the {@code event.target} to the
     * closest corresponding {@link Element} on the server side, to be returned
     * by {@link DomEvent#getEventTarget()}.
     *
     * @return this registration, for chaining
     * @since 9.0
     */
    default DomListenerRegistration mapEventTargetElement() {
        return addEventData(JsonConstants.MAP_STATE_NODE_EVENT_DATA);
    }

    /**
     * Add a JavaScript expression for extracting an element as event data. When
     * an event is fired in the browser, the expression is evaluated and if it
     * returns an element from DOM, the server side element or closest parent
     * element is sent to server side. The expression is evaluated in a context
     * where <code>element</code> refers to this element and <code>event</code>
     * refers to the fired event. If multiple expressions are defined for the
     * same event, their order of execution is undefined.
     * <p>
     * The result of the evaluation is available in
     * {@link DomEvent#getEventDataElement(String)} with the expression as the
     * key in the JSON object.
     * <p>
     * In case you want to get the {@code event.target} element to the server
     * side, use the {@link #mapEventTargetElement()} method to get it mapped
     * and the {@link DomEvent#getEventTarget()} to fetch the element.
     *
     * @param eventData
     *            definition for element that should be passed back to the
     *            server together with the event, not <code>null</code>
     * @return this registration, for chaining
     * @since 9.0
     * @see #mapEventTargetElement() to map the {@code event.target} to an
     *      element
     */
    default DomListenerRegistration addEventDataElement(String eventData) {
        Objects.requireNonNull(eventData);
        // optimizing this case as it is quite trivial
        if (Objects.equals(eventData, "event.target")) {
            return mapEventTargetElement();
        } else {
            return addEventData(
                    JsonConstants.MAP_STATE_NODE_EVENT_DATA + eventData);
        }
    }

    /**
     * Stops propagation of the event to upper level DOM elements.
     *
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/API/Event/stopPropagation">MDN
     *      docs for related JS DOM API</a>
     *
     * @return the DomListenerRegistration for further configuration
     */
    default public DomListenerRegistration stopPropagation() {
        addEventData("event.stopPropagation()");
        return this;
    };

    /**
     * Tries to prevent the default behavior of the event in the browser, such
     * as shortcut action on key press or context menu on "right click". This
     * might not be possible for some events.
     * <p>
     * When used with {@link #setFilter(String)}, preventDefault will only be
     * called when the filter condition is met. For example, if you set a filter
     * for specific keys, preventDefault will only apply to those keys.
     *
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/API/Event/preventDefault">MDN
     *      docs for related JS DOM API</a>
     *
     * @return the DomListenerRegistration for further configuration
     */
    default public DomListenerRegistration preventDefault() {
        addEventData("event.preventDefault()");
        return this;
    }

    /**
     * Configures the event listener to bypass the server side security checks
     * for modality. Handle with care! Can be ok when transferring data from
     * "non-ui" component events through the Element API, like e.g. geolocation
     * events.
     *
     * @return the DomListenerRegistration for further configuration
     */
    public DomListenerRegistration allowInert();

}
