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

package com.vaadin.flow.component.webcomponent;

import java.io.Serializable;

/**
 * {@code EventOptions} is used to configure the custom event published by an
 * exported web component.
 *
 * @author Vaadin Ltd.
 * @since 2.0
 */
public class EventOptions implements Serializable {
    private boolean bubbles = false;
    private boolean cancelable = false;
    private boolean composed = false;

    /**
     * Create {@code EventOptions} used to configure the custom event published
     * by an exported web component.
     *
     * @see #EventOptions(boolean, boolean, boolean) for all properties
     * @see com.vaadin.flow.component.WebComponentExporter for exporting web
     *         components
     */
    public EventOptions() {
    }

    /**
     * Create {@code EventOptions} used to configure the custom event published
     * by an exported web component.
     *
     * @param bubbles
     *         A Boolean indicating whether the event bubbles up through the DOM
     *         or not.
     * @see #EventOptions(boolean, boolean, boolean) for all properties
     * @see com.vaadin.flow.component.WebComponentExporter for exporting web
     *         components
     */
    public EventOptions(boolean bubbles) {
        this.bubbles = bubbles;
    }

    /**
     * Create {@code EventOptions} used to configure the custom event published
     * by an exported web component.
     *
     * @param bubbles
     *         indicates whether the event bubbles up through the DOM or not.
     * @param cancelable
     *         indicates whether the event is cancelable.
     * @param composed
     *         indicates whether or not the event can bubble across the boundary
     *         between the shadow DOM and the regular DOM.
     * @see com.vaadin.flow.component.WebComponentExporter for exporting web
     *         components
     */
    public EventOptions(boolean bubbles, boolean cancelable,
                        boolean composed) {
        this.bubbles = bubbles;
        this.cancelable = cancelable;
        this.composed = composed;
    }

    /**
     * Can the event bubbles up through the DOM or not.
     *
     * @return bubbles
     */
    public boolean isBubbles() {
        return bubbles;
    }

    /**
     * Is the event is cancelable.
     *
     * @return cancellable
     */
    public boolean isCancelable() {
        return cancelable;
    }

    /**
     * Can the event bubble across the boundary between the shadow DOM and the
     * regular DOM.
     *
     * @return composed
     */
    public boolean isComposed() {
        return composed;
    }
}
