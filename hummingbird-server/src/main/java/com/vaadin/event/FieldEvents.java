/*
 * Copyright 2000-2014 Vaadin Ltd.
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

package com.vaadin.event;

import java.io.Serializable;

import com.vaadin.annotations.EventType;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.shared.communication.FieldRpc.FocusAndBlurServerRpc;
import com.vaadin.ui.Component;
import com.vaadin.ui.Component.Event;
import com.vaadin.ui.Field;
import com.vaadin.ui.TextField;

/**
 * Interface that serves as a wrapper for {@link Field} related events.
 */
public interface FieldEvents {

    /**
     * The interface for adding and removing <code>FocusEvent</code> listeners.
     * By implementing this interface a class explicitly announces that it will
     * generate a <code>FocusEvent</code> when it receives keyboard focus.
     * <p>
     * Note: The general Java convention is not to explicitly declare that a
     * class generates events, but to directly define the
     * <code>addListener</code> and <code>removeListener</code> methods. That
     * way the caller of these methods has no real way of finding out if the
     * class really will send the events, or if it just defines the methods to
     * be able to implement an interface.
     * </p>
     *
     * @since 6.2
     * @see FocusListener
     * @see FocusEvent
     */
    public interface FocusNotifier extends Component, EventSource {
        /**
         * Adds a <code>FocusListener</code> to the Component which gets fired
         * when a <code>Field</code> receives keyboard focus.
         *
         * @param listener
         * @see FocusListener
         * @since 6.2
         */
        default public void addFocusListener(
                EventListener<FocusEvent> listener) {
            getElement().addEventListener(FocusEvent.class, listener,
                    FocusNotifier.this);
        }

        /**
         * Removes a <code>FocusListener</code> from the Component.
         *
         * @param listener
         * @see FocusListener
         * @since 6.2
         */
        default public void removeFocusListener(
                EventListener<FocusEvent> listener) {
            getElement().removeEventListener(FocusEvent.class, listener,
                    FocusNotifier.this);
        }
    }

    /**
     * The interface for adding and removing <code>BlurEvent</code> listeners.
     * By implementing this interface a class explicitly announces that it will
     * generate a <code>BlurEvent</code> when it loses keyboard focus.
     * <p>
     * Note: The general Java convention is not to explicitly declare that a
     * class generates events, but to directly define the
     * <code>addListener</code> and <code>removeListener</code> methods. That
     * way the caller of these methods has no real way of finding out if the
     * class really will send the events, or if it just defines the methods to
     * be able to implement an interface.
     * </p>
     *
     * @since 6.2
     * @see BlurListener
     * @see BlurEvent
     */
    public interface BlurNotifier extends Component, EventSource {
        /**
         * Adds a <code>BlurListener</code> to the Component which gets fired
         * when a <code>Field</code> loses keyboard focus.
         *
         * @param listener
         * @see BlurListener
         * @since 6.2
         */
        default public void addBlurListener(EventListener<BlurEvent> listener) {
            getElement().addEventListener(BlurEvent.class, listener,
                    BlurNotifier.this);
        }

        /**
         * Removes a <code>BlurListener</code> from the Component.
         *
         * @param listener
         * @see BlurListener
         * @since 6.2
         */
        default public void removeBlurListener(
                EventListener<BlurEvent> listener) {
            getElement().removeEventListener(BlurEvent.class, listener,
                    BlurNotifier.this);
        }

    }

    /**
     * <code>FocusEvent</code> class for holding additional event information.
     * Fired when a <code>Field</code> receives keyboard focus.
     *
     * @since 6.2
     */
    @EventType("focus")
    public static class FocusEvent extends Component.Event {

        public FocusEvent(Component source) {
            super(source);
        }
    }

    public interface BlurListener extends EventListener<BlurEvent> {
        @Override
        default void onEvent(BlurEvent event) {
            blur(event);
        }

        public void blur(BlurEvent event);

    }

    public interface FocusListener extends EventListener<FocusEvent> {
        @Override
        default void onEvent(FocusEvent event) {
            focus(event);
        }

        public void focus(FocusEvent event);

    }

    /**
     * <code>BlurEvent</code> class for holding additional event information.
     * Fired when a <code>Field</code> loses keyboard focus.
     *
     * @since 6.2
     */
    @EventType("blur")
    public static class BlurEvent extends Component.Event {

        /**
         * Identifier for event that can be used in {@link EventRouter}
         */
        public BlurEvent(Component source) {
            super(source);
        }
    }

    /**
     * TextChangeEvents are fired when the user is editing the text content of a
     * field. Most commonly text change events are triggered by typing text with
     * keyboard, but e.g. pasting content from clip board to a text field also
     * triggers an event.
     * <p>
     * TextChangeEvents differ from {@link ValueChangeEvent}s so that they are
     * triggered repeatedly while the end user is filling the field.
     * ValueChangeEvents are not fired until the user for example hits enter or
     * focuses another field. Also note the difference that TextChangeEvents are
     * only fired if the change is triggered from the user, while
     * ValueChangeEvents are also fired if the field value is set by the
     * application code.
     * <p>
     * The {@link TextChangeNotifier}s implementation may decide when exactly
     * TextChangeEvents are fired. TextChangeEvents are not necessary fire for
     * example on each key press, but buffered with a small delay. The
     * {@link TextField} component supports different modes for triggering
     * TextChangeEvents.
     *
     * @see TextChangeListener
     * @see TextChangeNotifier
     * @see TextField#setTextChangeEventMode(com.vaadin.ui.TextField.TextChangeEventMode)
     * @since 6.5
     */
    public static class TextChangeEvent extends Component.Event {

        private String text;
        private int cursorPosition;

        public TextChangeEvent(Component source, String text,
                int cursorPosition) {
            super(source);
            this.text = text;
            this.cursorPosition = cursorPosition;
        }

        /**
         * @return the text content of the field after the
         *         {@link TextChangeEvent}
         */
        public String getText() {
            return text;
        }

        /**
         * @return the cursor position during after the {@link TextChangeEvent}
         */
        public int getCursorPosition() {
            return cursorPosition;
        }
    }

    /**
     * A listener for {@link TextChangeEvent}s.
     *
     * @since 6.5
     */
    public interface TextChangeListener extends ComponentEventListener {

        /**
         * This method is called repeatedly while the text is edited by a user.
         *
         * @param event
         *            the event providing details of the text change
         */
        public void textChange(TextChangeEvent event);
    }

    /**
     * An interface implemented by a {@link Field} supporting
     * {@link TextChangeEvent}s. An example a {@link TextField} supports
     * {@link TextChangeListener}s.
     */
    public interface TextChangeNotifier extends Serializable {
        public void addTextChangeListener(TextChangeListener listener);

        public void removeTextChangeListener(TextChangeListener listener);

    }

    public static abstract class FocusAndBlurServerRpcImpl
            implements FocusAndBlurServerRpc {

        private Component component;

        public FocusAndBlurServerRpcImpl(Component component) {
            this.component = component;
        }

        protected abstract void fireEvent(Event event);

        @Override
        public void blur() {
            fireEvent(new BlurEvent(component));
        }

        @Override
        public void focus() {
            fireEvent(new FocusEvent(component));
        }
    }

}
