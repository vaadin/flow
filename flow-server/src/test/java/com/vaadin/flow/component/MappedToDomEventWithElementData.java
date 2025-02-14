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

package com.vaadin.flow.component;

import com.vaadin.flow.dom.Element;

@DomEvent("dom-event")
public class MappedToDomEventWithElementData extends ComponentEvent<Component> {

    private final Element element;

    /**
     * Creates a new event using the given source and indicator whether the
     * event originated from the client side or the server side.
     *
     * @param source
     *            the source component
     * @param fromClient
     *            <code>true</code> if the event originated from the client
     */
    public MappedToDomEventWithElementData(Component source, boolean fromClient,
            @EventData("element") Element element) {
        super(source, fromClient);
        this.element = element;
    }

    public Element getElement() {
        return element;
    }
}
