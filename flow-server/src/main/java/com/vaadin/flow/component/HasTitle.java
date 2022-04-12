/*
 * Copyright 2000-2022 Vaadin Ltd.
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

import java.util.Optional;

/**
 * A title, better known as a tool-tip is a textual hint that is displayed by
 * browsers when the mouse pointer is hovering over it; note that this implies
 * that mobile browsers will not display this.
 *
 * Also, for accessibility reasons, relying on the title attribute is currently
 * discouraged as many user agents do not expose the attribute in an accessible
 * manner as required by this specification (e.g., requiring a pointing device
 * such as a mouse to cause a tooltip to appear, which excludes keyboard-only
 * users and touch-only users, such as anyone with a modern phone or tablet).
 *
 * Any HTML element may have the 'title' attribute specified on it, see
 * https://html.spec.whatwg.org/multipage/dom.html#global-attributes .
 */
public interface HasTitle extends HasElement {
    /**
     * Sets the title of this component. Browsers typically use the title to
     * show a tooltip when hovering an element or any descendant without a title
     * value of its own.
     *
     * @param title
     *            the title value to set, or <code>""</code> to remove any
     *            previously set title
     */
    default void setTitle(String title) {
        final PropertyDescriptor<String, Optional<String>> titleDescriptor = PropertyDescriptors
                .optionalAttributeWithDefault("title", "");
        titleDescriptor.set(getElement(), title);
    }

    /**
     * Gets the title of this component.
     *
     * @see #setTitle(String)
     *
     * @return an optional title, or an empty optional if no title has been set
     *
     */
    default Optional<String> getTitle() {
        final PropertyDescriptor<String, Optional<String>> titleDescriptor = PropertyDescriptors
                .optionalAttributeWithDefault("title", "");
        return titleDescriptor.get(getElement());
    }
}
