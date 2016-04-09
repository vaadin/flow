/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.dom;

import java.util.EventObject;

/**
 * Event fired after an element is detached from the UI.
 */
public class DetachEvent extends EventObject {

    /**
     * Creates a new detach event with the given element as source.
     *
     * @param source
     *            the element that was detached
     */
    public DetachEvent(Element source) {
        super(source);
    }

    /**
     * Gets the element that was detached from the UI.
     *
     * @return the detached element
     */
    public Element getElement() {
        return (Element) getSource();
    }
}
