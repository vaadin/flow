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
package com.vaadin.ui.splitlayout;

import java.util.Optional;

import com.vaadin.flow.dom.ElementConstants;
import com.vaadin.ui.Component;
import com.vaadin.ui.common.HasSize;
import com.vaadin.ui.html.Div;

/**
 * Server-side component for the {@code <vaadin-split-layout>} element.
 * 
 * @author Vaadin Ltd
 */
public class SplitLayout extends GeneratedVaadinSplitLayout<SplitLayout>
        implements HasSize {

    private Component primaryComponent;
    private Component secondaryComponent;

    /**
     * Constructs an empty VaadinSplitLayout.
     */
    public SplitLayout() {
    }

    /**
     * Constructs a VaadinSplitLayout with the given initial components to set
     * to the primary and secondary splits.
     * 
     * @param primaryComponent
     *            the component set to the primary split
     * @param secondaryComponent
     *            the component set to the secondary split
     */
    public SplitLayout(Component primaryComponent,
            Component secondaryComponent) {
        addToPrimary(primaryComponent);
        addToSecondary(secondaryComponent);
    }

    /**
     * Sets the given components to the primary split of this layout, i.e. the
     * left split if in horizontal mode and the top split if in vertical mode.
     * <p>
     * <b>Note:</b> Calling this method with multiple arguments will wrap the
     * components inside a {@code <div>} element.
     * <p>
     * <b>Note:</b> Removing the primary component through the component API
     * will move the secondary component to the primary split, causing this
     * layout to desync with the server. This is a known issue.
     * 
     * @see #setVertical(boolean)
     * @return this instance, for method chaining
     */
    @Override
    public SplitLayout addToPrimary(Component... components) {
        if (components.length == 1) {
            primaryComponent = components[0];
        } else {
            Div container = new Div();
            container.add(components);
            primaryComponent = container;
        }
        return setComponents();
    }

    /**
     * Get the component currently set to the primary split.
     * 
     * @return the primary component, may be null
     */
    public Component getPrimaryComponent() {
        return primaryComponent;
    }

    /**
     * Sets the given components to the secondary split of this layout, i.e. the
     * right split if in horizontal mode and the bottom split if in vertical
     * mode.
     * <p>
     * <b>Note:</b> Calling this method with multiple arguments will wrap the
     * components inside a {@code <div>} element.
     * 
     * @see #setVertical(boolean)
     * @return this instance, for method chaining
     */
    @Override
    public SplitLayout addToSecondary(Component... components) {
        if (components.length == 1) {
            secondaryComponent = components[0];
        } else {
            Div container = new Div();
            container.add(components);
            secondaryComponent = container;
        }
        return setComponents();
    }

    /**
     * Get the component currently set to the secondary split.
     * 
     * @return the primary component, may be null
     */
    public Component getSecondaryComponent() {
        return secondaryComponent;
    }

    /**
     * Sets the relative position of the splitter in percentages. The given
     * value is used to set how much space is given to the primary component
     * relative to the secondary component. In horizontal mode this is the width
     * of the component and in vertical mode this is the height. The given value
     * will automatically be clamped to the range [0, 100].
     * 
     * @param position
     *            the relative position of the splitter, in percentages
     * @return this instance, for method chaining
     */
    public SplitLayout setSplitterPosition(double position) {
        double primary = Math.min(Math.max(position, 0), 100);
        double secondary = 100 - primary;
        String styleName;
        if (isVertical()) {
            styleName = ElementConstants.STYLE_HEIGHT;
        } else {
            styleName = ElementConstants.STYLE_WIDTH;
        }
        setPrimaryStyle(styleName, primary + "%");
        setSecondaryStyle(styleName, secondary + "%");
        return get();
    }

    /**
     * Set a style to the component in the primary split.
     * 
     * @param styleName
     *            name of the style to set
     * @param value
     *            the value to set
     * @return this instance, for method chaining
     */
    public SplitLayout setPrimaryStyle(String styleName, String value) {
        return setInnerComponentStyle(primaryComponent, styleName, value);
    }

    /**
     * Set a style to the component in the secondary split.
     * 
     * @param styleName
     *            name of the style to set
     * @param value
     *            the value to set
     * @return this instance, for method chaining
     */
    public SplitLayout setSecondaryStyle(String styleName, String value) {
        return setInnerComponentStyle(secondaryComponent, styleName, value);
    }

    private SplitLayout setComponents() {
        removeAll();
        if (primaryComponent == null) {
            super.addToPrimary(new Div());
        } else {
            super.addToPrimary(primaryComponent);
        }
        if (secondaryComponent == null) {
            super.addToSecondary(new Div());
        } else {
            super.addToSecondary(secondaryComponent);
        }
        return get();
    }

    private SplitLayout setInnerComponentStyle(Component innerComponent,
            String styleName, String value) {
        Optional.ofNullable(innerComponent).ifPresent(component -> component
                .getElement().getStyle().set(styleName, value));
        return get();
    }
}
