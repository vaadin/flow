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
package com.vaadin.flow.component;

/**
 * Base class for a {@link Component} that represents a single built-in HTML
 * element that can contain child components or text.
 * <p>
 * This base class is meant for elements whose HTML content model accepts any
 * kind of content, such as <code>&lt;div&gt;</code>, <code>&lt;span&gt;</code>
 * or <code>&lt;section&gt;</code>. It implements {@link HasComponents}, so any
 * {@link Component} can be added as a child and the API cannot prevent a child
 * that the element isn't allowed to contain.
 * <p>
 * Elements with a restricted content model are a poor fit. Such a component
 * should instead extend {@link HtmlComponent} and expose only an API that
 * matches what the element actually accepts. The shape of that API depends on
 * the element:
 * <ul>
 * <li>When each part of the content is different, a dedicated API for each part
 * is the clearest option. A <code>&lt;table&gt;</code>, for example, accepts an
 * optional <code>&lt;caption&gt;</code>, then <code>&lt;colgroup&gt;</code>,
 * <code>&lt;thead&gt;</code>, <code>&lt;tbody&gt;</code> and
 * <code>&lt;tfoot&gt;</code> elements in a specific order, so a table component
 * is better served by separate methods for the caption, the header, the bodies
 * and the footer than by a generic add-a-child method.</li>
 * <li>When all children are of the same type, such as the
 * <code>&lt;li&gt;</code> children of a <code>&lt;ul&gt;</code>,
 * {@link HasComponentsOfType} gives the same add and remove methods as
 * {@link HasComponents} while rejecting unrelated components already at compile
 * time.</li>
 * </ul>
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class HtmlContainer extends HtmlComponent
        implements HasComponents, HasText {
    /**
     * Creates an empty component with the element determined by the {@link Tag}
     * annotation of a sub class.
     */
    protected HtmlContainer() {
        super();
    }

    /**
     * Creates a component with the given child components. The element is
     * determined by the {@link Tag} annotation of a sub class.
     *
     * @param components
     *            the child components
     */
    protected HtmlContainer(Component... components) {
        add(components);
    }

    /**
     * Creates a new empty component with a new element with the given tag name.
     *
     * @param tagName
     *            the tag name of the element to use for this component, not
     *            <code>null</code>
     */
    public HtmlContainer(String tagName) {
        super(tagName);
    }

    /**
     * Creates a new component with the given contents and a new element with
     * the given tag name.
     *
     * @param tagName
     *            the tag name of the element to use for this component, not
     *            <code>null</code>
     * @param components
     *            the child components
     */
    public HtmlContainer(String tagName, Component... components) {
        super(tagName);
        add(components);
    }

}
