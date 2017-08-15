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
package com.vaadin.ui;

/**
 * VerticalLayout is a component container, which shows the subcomponents in the
 * order of their addition (vertically). A vertical layout is by default 100%
 * wide.
 */
public class VerticalLayout extends FlexLayout {

    /**
     * Default constructor. Creates an empty layout, with 100% width, without
     * spacing, with items aligned as {@link Alignment#STRETCH}.
     */
    public VerticalLayout() {
        getStyle().set("flexDirection", "column").set("width", "100%");
    }

    /**
     * Convenience constructor to create a layout with the children already
     * inside it.
     * 
     * @param children
     *            the items to add to this layout
     * @see #add(Component...)
     */
    public VerticalLayout(Component... children) {
        this();
        add(children);
    }
}
