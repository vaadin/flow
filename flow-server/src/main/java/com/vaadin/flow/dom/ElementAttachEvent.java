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

import java.util.EventObject;

/**
 * Event fired after an Element has been attached to the UI.
 * <p>
 * When a hierarchy of elements is being attached, this event is fired
 * child-first.
 *
 * @since 1.0
 */
public class ElementAttachEvent extends EventObject {

    /**
     * Creates a new attach event with the given element as source.
     *
     * @param source
     *            the element that was attached
     */
    public ElementAttachEvent(Element source) {
        super(source);
    }

    @Override
    public Element getSource() {
        return (Element) super.getSource();
    }
}
