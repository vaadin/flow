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
package com.vaadin.ui;

import com.vaadin.hummingbird.dom.Element;

/**
 * A component which encapsulates the given text in a text node.
 *
 * @author Vaadin
 * @since
 */
public class Text implements Component {

    private Element element;

    /**
     * Creates an instance using the given text.
     *
     * @param text
     *            the text to show
     */
    public Text(String text) {
        setElement(Element.createText(text));
    }

    private void setElement(Element element) {
        assert this.element == null : "Element has already been set";
        assert element != null : "Element can not be null";

        this.element = element;
        this.element.attachComponent(this);
    }

    /**
     * Sets the text of the component.
     *
     * @param text
     *            the text of the component
     */
    public void setText(String text) {
        getElement().setTextContent(text);
    }

    /**
     * Gets the text of the component.
     *
     * @return the text of the component
     */
    public String getText() {
        return getElement().getOwnTextContent();
    }

    @Override
    public Element getElement() {
        return element;
    }

}
