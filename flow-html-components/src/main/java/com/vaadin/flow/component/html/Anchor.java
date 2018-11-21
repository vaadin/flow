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

import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.server.AbstractStreamResource;
import com.vaadin.flow.server.StreamResource;

/**
 * Component representing an <code>&lt;a&gt;</code> element.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Tag(Tag.A)
public class Anchor extends HtmlContainer {

    private static final PropertyDescriptor<String, String> hrefDescriptor = PropertyDescriptors
            .attributeWithDefault("href", "", false);

    private static final PropertyDescriptor<String, Optional<String>> targetDescriptor = PropertyDescriptors
            .optionalAttributeWithDefault("target", "");

    /**
     * Creates a new empty anchor component.
     */
    public Anchor() {
        super();
    }

    /**
     * Creates an anchor component with the given text content and href.
     *
     * @see #setHref(String)
     * @see #setText(String)
     *
     * @param href
     *            the href to set
     * @param text
     *            the text content to set
     */
    public Anchor(String href, String text) {
        setHref(href);
        setText(text);
    }

    /**
     * Creates an anchor component with the given text content and stream
     * resource.
     *
     * @see #setHref(AbstractStreamResource)
     * @see #setText(String)
     *
     * @param href
     *            the resource value, not null
     * @param text
     *            the text content to set
     */
    public Anchor(AbstractStreamResource href, String text) {
        setHref(href);
        setText(text);
    }

    /**
     * Creates an anchor component with the given href and components
     * as children of this component.
     *
     * @see #setHref(AbstractStreamResource)
     * @see #add(Component...)
     *
     * @param href
     *            the href to set
     * @param components
     *            the components to add
     */
    public Anchor(String href, Component... components) {
        setHref(href);
        add(components);
    }

    /**
     * Sets the URL that this anchor links to.
     * <p>
     * Use the method {@link #removeHref()} to remove the <b>href</b> attribute
     * instead of setting it to an empty string.
     *
     * @see #removeHref()
     * @see #setHref(AbstractStreamResource)
     *
     * @param href
     *            the href to set
     */
    public void setHref(String href) {
        set(hrefDescriptor, href);
    }

    /**
     * Removes href attribute.
     *
     * @see Anchor#setHref(String)
     *
     */
    public void removeHref() {
        getElement().removeAttribute("href");
    }

    /**
     * Sets the URL that this anchor links to with the URL of the given
     * {@link StreamResource}.
     *
     * @param href
     *            the resource value, not null
     */
    public void setHref(AbstractStreamResource href) {
        getElement().setAttribute("href", href);
    }

    /**
     * Gets the URL that this anchor links to.
     *
     * @see #setHref(String)
     *
     * @return the href value, or <code>""</code> if no href has been set
     */
    public String getHref() {
        return get(hrefDescriptor);
    }

    /**
     * Sets the target window, tab or frame for this anchor. The target is
     * either the <code>window.name</code> of a specific target, or one of these
     * special values:
     * <ul>
     * <li><code>_self</code>: Open the link in the current context. This is the
     * default behavior.
     * <li><code>_blank</code>: Opens the link in a new unnamed context.
     * <li><code>_parent</code>: Opens the link in the parent context, or the
     * current context if there is no parent context.
     * <li><code>_top</code>: Opens the link in the top most grandparent
     * context, or the current context if there is no parent context.
     * </ul>
     *
     * @param target
     *            the target value, or <code>""</code> to remove the target
     *            value
     */
    public void setTarget(String target) {
        set(targetDescriptor, target);
    }

    /**
     * Gets the target window, tab or frame name for this anchor.
     *
     * @see #setTarget(String)
     *
     * @return an optional target, or an empty optional if no target has been
     *         set
     */
    public Optional<String> getTarget() {
        return get(targetDescriptor);
    }

}
