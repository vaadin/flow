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
package com.vaadin.ui;

import java.util.EventObject;

/**
 * Superclass of all component originated events.
 *
 * <p>
 * Events are the basis of all user interaction handling in Vaadin. To handle
 * events, you provide a listener object that receives the events of the
 * particular event type.
 * </p>
 *
 * <pre>
 * Button button = new Button(&quot;Click Me!&quot;);
 * button.addListener(new Button.ClickListener() {
 *     public void buttonClick(ClickEvent event) {
 *         getWindow().showNotification(&quot;Thank You!&quot;);
 *     }
 * });
 * layout.addComponent(button);
 * </pre>
 *
 * <p>
 * Notice that while each of the event types have their corresponding listener
 * types; the listener interfaces are not required to inherit the
 * {@code ComponentEventListener} interface.
 * </p>
 *
 * @see ComponentEventListener
 */
public abstract class ComponentEvent extends EventObject {

    /**
     * Constructs a new event with the specified source component.
     *
     * @param source
     *            the source component of the event
     */
    public ComponentEvent(Component source) {
        super(source);
    }

    /**
     * Gets the component where the event occurred.
     *
     * @return the source component of the event
     */
    public Component getComponent() {
        return (Component) getSource();
    }

}