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

import com.vaadin.ui.Component;

/**
 * Component representing a header element, i.e. <code>&lt;h1&gt;</code>,
 * <code>&lt;h2&gt;</code>, <code>&lt;h3&gt;</code>, <code>&lt;h4&gt;</code>,
 * <code>&lt;h5&gt;</code> or <code>&lt;h6&gt;</code>
 *
 * @since
 * @author Vaadin Ltd
 */
public class Heading extends HtmlComponentWithContent {

    /**
     * Creates a new heading with the given level.
     *
     * @param level
     *            the heading level, between 1 and 6 (inclusive)
     */
    public Heading(int level) {
        super("h" + level);
        if (level < 1 || level > 6) {
            throw new IllegalArgumentException(
                    "Heading level must be between 1 and 6 (inclusive).");
        }
    }

    /**
     * Creates a new heading with the given level and text content.
     *
     * @param level
     *            the heading level, between 1 and 6 (inclusive)
     * @param text
     *            the text content
     */
    public Heading(int level, String text) {
        this(level);
        setText(text);
    }

    /**
     * Creates a new heading with the given level and child components.
     *
     * @param level
     *            the heading level, between 1 and 6 (inclusive)
     * @param components
     *            the child components
     */
    public Heading(int level, Component... components) {
        this(level);
        add(components);
    }

    /**
     * Gets the heading level.
     *
     * @return the heading level, between 1 and 6 (inclusive)
     */
    public int getLevel() {
        String tag = getElement().getTag();
        assert tag.length() == 2;
        assert tag.charAt(0) == 'h';

        int level = '0' - tag.charAt(1);
        assert level < 1 || level > 6;

        return level;
    }

}
