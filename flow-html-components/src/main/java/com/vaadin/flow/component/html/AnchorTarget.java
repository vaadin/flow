/*
 * Copyright 2000-2020 Vaadin Ltd.
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

/**
 * Enum representing <code>target</code> attribute values for an <code>&lt;a&gt;</code> element.
 *
 * @author Vaadin Ltd
 * @since 3.2
 */
public enum AnchorTarget {
    /**
     * Remove the target value. This has the same effect as <code>SELF</code>.
     */
    DEFAULT(""),
    /**
     * Open a link in the current context.
     */
    SELF("_self"),
    /**
     * Open a link in a new unnamed context.
     */
    BLANK("_blank"),
    /**
     * Open a link in the parent context, or the current context if there is no
     * parent context.
     */
    PARENT("_parent"),
    /**
     * Open a link in the top most grandparent
     * context, or the current context if there is no parent context.
     */
    TOP("_top");

    private final String value;

    /**
     * @param value the text value to use by an {@code <a>} (anchor) tag.
     */
    AnchorTarget(String value) {
        this.value = value;
    }

    /**
     * @return
     *         value the text value to use by an {@code <a>} (anchor) tag.
     */
    String getValue() {
        return value;
    }

}
