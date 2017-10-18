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
package com.vaadin.ui.layout;

import com.vaadin.ui.Component;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;

/**
 * VerticalLayout is a component container, which shows the subcomponents in the
 * order of their addition (vertically). A vertical layout is by default 100%
 * wide.
 */
@Tag("vaadin-vertical-layout")
@HtmlImport("frontend://bower_components/vaadin-ordered-layout/vaadin-vertical-layout.html")
public class VerticalLayout extends FlexLayout {

    /**
     * Default constructor. Creates an empty layout, with 100% width, without
     * spacing, with items aligned as {@link Alignment#STRETCH}.
     */
    public VerticalLayout() {
        getStyle().set("flexDirection", "column").set("width", "100%");
        setDefaultHorizontalComponentAlignment(Alignment.START);
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
     * Sets a horizontal alignment for individual components inside the layout.
     * This individual alignment for the component overrides any alignment set
     * at the {@link #setDefaultHorizontalComponentAlignment(Alignment)}.
     * <p>
     * The default alignment for individual components is
     * {@link Alignment#AUTO}.
     * <p>
     * It's the same as the {@link #setAlignSelf(Alignment, Component...)}
     * method
     *
     * @see #setAlignSelf(Alignment, Component...)
     *
     * @param alignment
     *            the individual alignment for the children components. Setting
     *            <code>null</code> will reset the alignment to its default
     * @param componentsToAlign
     *            The components to which the individual alignment should be set
     */
    public void setHorizontalComponentAlignment(Alignment alignment,
            Component... componentsToAlign) {
        setAlignSelf(alignment, componentsToAlign);
    }

    /**
     * Gets the individual horizontal alignment of a given component.
     * <p>
     * The default alignment for individual components is
     * {@link Alignment#AUTO}.
     * <p>
     * It's the same as the {@link #getAlignSelf(Component)} method
     *
     * @see #getAlignSelf(Component)
     *
     * @param component
     *            The component which individual layout should be read
     * @return the alignment of the component, never <code>null</code>
     */
    public Alignment getHorizontalComponentAlignment(Component component) {
        return getAlignSelf(component);
    }

    /**
     * Sets the default horizontal alignment to be used by all components
     * without individual alignments inside the layout. Individual components
     * can be aligned by using the
     * {@link #setHorizontalComponentAlignment(Alignment, Component...)} method.
     * <p>
     * The default alignment is {@link Alignment#START}.
     * <p>
     * It's the same as the {@link #setAlignItems(Alignment)} method.
     *
     * @see #setAlignItems(Alignment)
     *
     * @param alignment
     *            the alignment to apply to the components. Setting
     *            <code>null</code> will reset the alignment to its default
     */
    public void setDefaultHorizontalComponentAlignment(Alignment alignment) {
        setAlignItems(alignment);
    }

    /**
     * Gets the default horizontal alignment used by all components without
     * individual alignments inside the layout.
     * <p>
     * The default alignment is {@link Alignment#START}.
     * <p>
     * It's the same as the {@link #getAlignItems()} method.
     *
     * @see VerticalLayout#getAlignItems()
     *
     *
     * @return the general alignment used by the layout, never <code>null</code>
     */
    public Alignment getDefaultHorizontalComponentAlignment() {
        return getAlignItems();
    }

    /**
     * Sets the default horizontal alignment to be used by all components
     * without individual alignments inside the layout. Individual components
     * can be aligned by using the
     * {@link #setAlignSelf(Alignment, Component...)} method.
     * <p>
     * It effectively sets the {@code "alignItems"} style value.
     * <p>
     * The default alignment is {@link Alignment#START}.
     *
     * @param alignment
     *            the alignment to apply to the components. Setting
     *            <code>null</code> will reset the alignment to its default
     */
    @Override
    public void setAlignItems(Alignment alignment) {
        if (alignment == null) {
            getStyle().set(ALIGN_ITEMS_CSS_PROPERTY,
                    Alignment.START.getFlexValue());
        } else {
            getStyle().set(ALIGN_ITEMS_CSS_PROPERTY, alignment.getFlexValue());
        }
    }

    /**
     * Gets the default horizontal alignment used by all components without
     * individual alignments inside the layout.
     * <p>
     * The default alignment is {@link Alignment#START}.
     *
     * @return the general alignment used by the layout, never <code>null</code>
     */
    @Override
    public Alignment getAlignItems() {
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
