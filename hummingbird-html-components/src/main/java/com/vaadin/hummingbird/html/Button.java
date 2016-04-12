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
package com.vaadin.hummingbird.html;

import java.util.function.Consumer;

import com.vaadin.annotations.Tag;
import com.vaadin.hummingbird.html.event.ClickEvent;
import com.vaadin.hummingbird.html.event.ClickNotifier;

/**
 * Component representing a <code>&lt;button&gt;</code> element.
 *
 * @since
 * @author Vaadin Ltd
 */
@Tag("button")
public class Button extends HtmlComponentWithContent implements ClickNotifier {
    /**
     * Creates a new empty button.
     */
    public Button() {
        super();
    }

    /**
     * Creates a button with the given text.
     *
     * @param text
     *            the button text
     */
    public Button(String text) {
        setText(text);
    }

    /**
     * Creates a button with the given text and click listener.
     * 
     * @param text
     *            the button text
     * @param clickListener
     *            the click listener
     */
    public Button(String text, Consumer<ClickEvent> clickListener) {
        this(text);
        addClickListener(clickListener);
    }

}
