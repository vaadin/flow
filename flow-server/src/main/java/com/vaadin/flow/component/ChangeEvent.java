/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.component;

/**
 * Event fired when the value of a component has changed. The new value can be
 * retrieved from the component that fired the event.
 * <p>
 * This event is specifically intended to the used for the <code>change</code>
 * event in the DOM API.
 *
 * @author Vaadin Ltd
 */
@DomEvent("change")
public class ChangeEvent extends ComponentEvent<HtmlComponent> {
    /**
     * Creates a new change event.
     *
     * @param source
     *            the component that fired the event
     * @param fromClient
     *            <code>true</code> if the event was originally fired on the
     *            client, <code>false</code> if the event originates from
     *            server-side logic
     */
    public ChangeEvent(HtmlComponent source, boolean fromClient) {
        super(source, fromClient);
    }
}
