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
package com.vaadin.flow.component.html;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;button&gt;</code> element.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Tag(Tag.BUTTON)
public class NativeButton extends HtmlContainer
        implements ClickNotifier<NativeButton>, Focusable<NativeButton> {
    /**
     * Creates a new empty button.
     */
    public NativeButton() {
        super();
    }

    /**
     * Creates a button with the given text.
     *
     * @param text
     *            the button text
     */
    public NativeButton(String text) {
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
    public NativeButton(String text,
            ComponentEventListener<ClickEvent<NativeButton>> clickListener) {
        this(text);
        addClickListener(clickListener);
    }

}
