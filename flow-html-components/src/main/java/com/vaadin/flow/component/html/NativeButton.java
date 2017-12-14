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
package com.vaadin.flow.component.html;

import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlContainer;
import com.vaadin.ui.event.ComponentEventListener;
import com.vaadin.ui.event.ClickEvent;
import com.vaadin.ui.event.ClickNotifier;

/**
 * Component representing a <code>&lt;button&gt;</code> element.
 *
 * @author Vaadin Ltd
 */
@Tag(Tag.BUTTON)
public class NativeButton extends HtmlContainer implements ClickNotifier {
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
            ComponentEventListener<ClickEvent> clickListener) {
        this(text);
        addClickListener(clickListener);
    }

}
