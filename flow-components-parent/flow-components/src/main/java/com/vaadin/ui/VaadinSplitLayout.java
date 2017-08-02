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

import com.vaadin.flow.html.Div;
import com.vaadin.generated.vaadin.split.layout.GeneratedVaadinSplitLayout;

/**
 * Server-side component for the {@code <vaadin-split-layout>} element.
 * 
 * @author Vaadin Ltd
 */
public class VaadinSplitLayout
        extends GeneratedVaadinSplitLayout<VaadinSplitLayout> {

    private Component primaryComponent;
    private Component secondaryComponent;

    /**
     * Default constructor.
     */
    public VaadinSplitLayout() {
    }

    /**
     *
     * @param primaryComponent
     * @param secondaryComponent
     */
    public VaadinSplitLayout(Component primaryComponent,
            Component secondaryComponent) {
        addToPrimary(primaryComponent);
        addToSecondary(secondaryComponent);
    }

    /**
     *
     * @param primaryComponent
     * @param secondaryComponent
     * @param initialSplitterPosition
     */
    public VaadinSplitLayout(Component primaryComponent,
            Component secondaryComponent, double initialSplitterPosition) {
        this(primaryComponent, secondaryComponent);
        double primaryWidth = Math.min(Math.max(initialSplitterPosition, 0),
                100);
        double secondaryWidth = 100 - primaryWidth;
        primaryComponent.getElement().getStyle().set("width",
                primaryWidth + "%");
        secondaryComponent.getElement().getStyle().set("width",
                secondaryWidth + "%");
    }

    /**
     * 
     * <b>Note:</b> Calling this method with multiple arguments will wrap the
     * components in a {@code <div>} element.
     */
    @Override
    public VaadinSplitLayout addToPrimary(Component... components) {
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
     *
     * <b>Note:</b> Calling this method with multiple arguments will wrap the
     * components in a {@code <div>} element.
     */
    @Override
    public VaadinSplitLayout addToSecondary(Component... components) {
        if (components.length == 1) {
            secondaryComponent = components[0];
        } else {
            Div container = new Div();
            container.add(components);
            secondaryComponent = container;
        }
        return setComponents();
    }

    private VaadinSplitLayout setComponents() {
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
}
