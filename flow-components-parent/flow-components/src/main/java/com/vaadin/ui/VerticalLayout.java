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
        setDefaultComponentAlignment(Alignment.START);
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

    /**
     * Sets the default alignment to be used by all components without
     * individual alignments inside the layout. Individual components can be
     * aligned by using the
     * {@link #setComponentAlignment(Alignment, Component...)} method.
     * <p>
     * The default alignment is {@link Alignment#START}.
     *
     * @param alignment
     *            the alignment to apply to the components. Setting
     *            <code>null</code> will reset the alignment to its default
     */
    @Override
    public void setDefaultComponentAlignment(Alignment alignment) {
        if (alignment == null) {
            getStyle().set(ALIGN_ITEMS_CSS_PROPERTY,
                    Alignment.START.getFlexValue());
        } else {
            getStyle().set(ALIGN_ITEMS_CSS_PROPERTY, alignment.getFlexValue());
        }
    }

    /**
     * Gets the default alignment used by all components without individual
     * alignments inside the layout.
     * <p>
     * The default alignment is {@link Alignment#START}.
     *
     * @return the general alignment used by the layout, never <code>null</code>
     */
    @Override
    public Alignment getDefaultComponentAlignment() {
        return Alignment.toAlignment(getStyle().get(ALIGN_ITEMS_CSS_PROPERTY),
                Alignment.START);
    }

    /**
     * Expands the given components.
     * <p>
     * It effectively sets {@code 1} as a flex grow property value for each
     * component.
     *
     * @param componentsToExpand
     *            components to expand
     */
    public void expand(Component... componentsToExpand) {
        setFlexGrow(1.0d, componentsToExpand);
    }
}
