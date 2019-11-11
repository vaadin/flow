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
package com.vaadin.flow.component;

import com.vaadin.flow.dom.Element;

/**
 * A component which encapsulates the given text in a text node.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class Text extends Component implements HasText {

    /**
     * Creates an instance using the given text.
     *
     * @param text
     *            the text to show, not <code>null</code>
     */
    public Text(String text) {
        super(Element.createText(text));
    }

    /**
     * Sets the text of the component.
     *
     * @param text
     *            the text of the component, not <code>null</code>
     */
    @Override
    public void setText(String text) {
        if (text == null) {
            throw new IllegalArgumentException("The text must not be null");
        }
        getElement().setText(text);
    }

    /**
     * Gets the text of the component.
     *
     * @return the text of the component, not <code>null</code>
     */
    @Override
    public String getText() {
        return getElement().getText();
    }

}
