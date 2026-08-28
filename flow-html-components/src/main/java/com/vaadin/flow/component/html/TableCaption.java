/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;caption&gt;</code> element — the title of
 * a {@link Table}, announced by assistive technologies as the table's name.
 * <p>
 * A caption holds flow content, so unlike the table itself it does extend
 * {@link HtmlContainer}: anything may go inside it.
 *
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/caption">MDN:
 *      &lt;caption&gt; — The Table Caption element</a>
 * @since 25.3
 */
@Tag(Tag.CAPTION)
public class TableCaption extends HtmlContainer {

    /**
     * Creates a new empty caption.
     */
    public TableCaption() {
        super();
    }

    /**
     * Creates a new caption with the given children.
     *
     * @param components
     *            the children components.
     */
    public TableCaption(Component... components) {
        super(components);
    }

    /**
     * Creates a new caption with the given text.
     *
     * @param text
     *            the caption text.
     */
    public TableCaption(String text) {
        super();
        setText(text);
    }
}
