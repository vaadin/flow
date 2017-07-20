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
package com.vaadin.ui;

import com.vaadin.flow.event.ComponentEventListener;
import com.vaadin.flow.html.Image;
import com.vaadin.generated.vaadin.button.GeneratedVaadinButton;

/**
 * Server-side component for the <code>vaadin-button</code> element.
 */
public class VaadinButton extends GeneratedVaadinButton<VaadinButton> {

    /**
     * Default constructor. Creates an empty button.
     */
    public VaadinButton() {
    }

    /**
     * Creates a button with a text inside.
     * 
     * @param text
     *            the text inside the button
     * @see #setText(String)
     */
    public VaadinButton(String text) {
        super(text);
    }

    /**
     * Creates a button with a text and a listener for click events.
     * 
     * @param text
     *            the text inside the button
     * @param clickListener
     *            the event listener for click events
     * @see #setText(String)
     * @see #addClickListener(ComponentEventListener)
     */
    public VaadinButton(String text,
            ComponentEventListener<ClickEvent<VaadinButton>> clickListener) {
        super(text);
        addClickListener(clickListener);
    }

    /**
     * Creates a button with an image inside.
     * 
     * @param image
     *            the image inside the button
     * @see #add(Component...)
     */
    public VaadinButton(Image image) {
        add(image);
    }

    /**
     * Creates a button with an image and a listener for click events.
     * 
     * @param image
     *            the image inside the button
     * @param clickListener
     *            the event listener for click events
     * @see #add(Component...)
     * @see #addClickListener(ComponentEventListener)
     */
    public VaadinButton(Image image,
            ComponentEventListener<ClickEvent<VaadinButton>> clickListener) {
        add(image);
        addClickListener(clickListener);
    }

    /**
     * Executes a click on this button at the client-side. Calling this method
     * behaves exactly the same as if the user would have clicked on the button.
     */
    public void click() {
        getElement().callFunction("click");
    }

}
