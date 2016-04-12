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

import java.util.Objects;

import com.vaadin.annotations.Tag;
import com.vaadin.hummingbird.html.event.ChangeEvent;
import com.vaadin.hummingbird.html.event.ChangeNotifier;

/**
 * Component representing an <code>&lt;input&gt;</code> element.
 *
 * @since
 * @author Vaadin Ltd
 */
@Tag("input")
public class Input extends HtmlComponent implements ChangeNotifier {

    /**
     * Creates a new input without any specific type.
     */
    public Input() {
        getElement().synchronizeProperty("value", "change");
    }

    /**
     * Creates a new input with the defined type.
     *
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/input">
     *      Overview of supported type values</a>
     *
     * @param type
     *            the type of the input
     */
    public Input(String type) {
        this();
        setType(type);
    }

    /**
     * Sets the placeholder text that is shown if the input is empty.
     *
     * @param placeholder
     *            the placeholder text to set, or <code>null</code> to remove
     *            the placeholder
     */
    public void setPlaceholder(String placeholder) {
        setAttribute("placeholder", placeholder);
    }

    /**
     * Gets the placeholder text.
     *
     * @see #setPlaceholder(String)
     *
     * @return the placeholder, or <code>null</code> if there is no placeholder
     */
    public String getPlaceholder() {
        return getAttribute("placeholder");
    }

    /**
     * Gets the value of this component. For textual input components, the value
     * is the text displayed in the component.
     *
     * @return the the value, or <code>null</code> if no value is set
     */
    public String getValue() {
        return getElement().getProperty("value");
    }

    /**
     * Sets the value of this component. For textual input components, the value
     * is the text displayed in the component.
     * <p>
     * This methods fires a {@link ChangeEvent} if the value is changed.
     *
     * @param value
     *            the value to set, or <code>null</code> to remove the value
     */
    public void setValue(String value) {
        String oldValue = getValue();
        getElement().setProperty("value", value);

        if (!Objects.equals(value, oldValue)) {
            fireEvent(new ChangeEvent(this, false));
        }
    }

    /**
     * Sets the type of this input.
     *
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/input">
     *      Overview of supported type values</a>
     *
     * @param type
     *            the type, or <code>null</code> use the default type
     */
    public void setType(String type) {
        setAttribute("type", type);
    }

    /**
     * Gets the type of this input.
     *
     * @return the input type, or <code>null</code> if no input type is defined.
     */
    public String getType() {
        return getAttribute("type");
    }
}
