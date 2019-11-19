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

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;pre&gt;</code> element.
 *
 * @author Vaadin Ltd
 */
@Tag(Tag.PRE)
public class Pre extends HtmlContainer
        implements ClickNotifier<Pre> {

    /**
     * Creates a new empty preformatted text block.
     */
    public Pre() {
        super();
    }

    /**
     * Creates a new preformatted text block with the given child components.
     *
     * @param components
     *            the child components
     */
    public Pre(Component... components) {
        super(components);
    }

    /**
     * Creates a new paragraph with the given text.
     *
     * @param text
     *            the text
     */
    public Pre(String text) {
        super();
        setText(text);
    }
}
