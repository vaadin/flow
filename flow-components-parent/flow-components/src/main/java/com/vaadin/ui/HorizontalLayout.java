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
 * HorizontalLayout is a component container, which shows the subcomponents in
 * the order of their addition (horizontally). A horizontal layout is doesn't
 * have a predefined size - its size is defined by the components inside it.
 */
public class HorizontalLayout extends FlexLayout {

    /**
     * Default constructor. Creates an empty layout, without spacing and without
     * a predefined width. The default alignment is {@link Alignment#BASELINE}.
     */
    public HorizontalLayout() {
        getStyle().set("display", "inline-flex").set("flexDirection", "row");
        setDefaultComponentAlignment(Alignment.BASELINE);
    }

    /**
     * Convenience constructor to create a layout with the children already
     * inside it.
     * 
     * @param children
     *            the items to add to this layout
     * @see #add(Component...)
     */
    public HorizontalLayout(Component... children) {
        this();
        add(children);
    }

    /**
     * Sets the default alignment to be used by all components without
     * individual alignments inside the layout. Individual components can be
     * aligned by using the
     * {@link #setComponentAlignment(Alignment, Component...)} method.
     * <p>
     * The default alignment is {@link Alignment#BASELINE}.
     * 
     * @param alignment
     *            the alignment to apply to the components. Setting
     *            <code>null</code> will reset the alignment to its default
     */
    public void setDefaultComponentAlignment(Alignment alignment) {
        super.setDefaultComponentAlignment(alignment);
    }

    /**
     * Gets the default alignment used by all components without individual
     * alignments inside the layout.
     * <p>
     * The default alignment is {@link Alignment#BASELINE}.
     * 
     * @return the general alignment used by the layout, never <code>null</code>
     */
    public Alignment getDefaultComponentAlignment() {
        return super.getDefaultComponentAlignment();
    }

    @Override
    protected Alignment getPredefinedDefaultAlignment() {
        return Alignment.BASELINE;
    }

}
