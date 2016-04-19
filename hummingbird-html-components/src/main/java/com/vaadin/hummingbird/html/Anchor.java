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

import com.vaadin.annotations.Tag;

/**
 * Component representing an <code>&lt;a&gt;</code> element.
 *
 * @since
 * @author Vaadin Ltd
 */
@Tag("a")
public class Anchor extends HtmlContainer {

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
     * Sets the URL that this anchor links to.
     *
     * @param href
     *            the href to set, or <code>null</code> to remove the href value
     */
    public void setHref(String href) {
        setAttribute("href", href);
    }

    /**
     * Gets the URL that this anchor links to.
     *
     * @see #setHref(String)
     *
     * @return the href value, or <code>null</code> if no href has been set
     */
    public String getHref() {
        return getAttribute("href");
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
     *            the target value, or <code>null</code> to remove the current
     *            target
     */
    public void setTarget(String target) {
        setAttribute("target", target);
    }

    /**
     * Gets the target window, tab or frame name for this anchor.
     *
     * @see #setTarget(String)
     *
     * @return the target value, or <code>null</code> if no value has been set
     */
    public String getTarget() {
        return getAttribute("target");
    }

}
