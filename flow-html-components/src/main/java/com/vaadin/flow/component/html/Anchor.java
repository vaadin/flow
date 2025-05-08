/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasAriaLabel;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.server.AbstractStreamResource;
import com.vaadin.flow.server.DownloadHandler;
import com.vaadin.flow.server.ElementRequestHandler;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.StreamResourceRegistry;

/**
 * Component representing an <code>&lt;a&gt;</code> element.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Tag(Tag.A)
public class Anchor extends HtmlContainer
        implements Focusable<Anchor>, HasAriaLabel {

    private static final PropertyDescriptor<String, String> hrefDescriptor = PropertyDescriptors
            .attributeWithDefault("href", "", false);

    private static final PropertyDescriptor<String, Optional<String>> targetDescriptor = PropertyDescriptors
            .optionalAttributeWithDefault("target",
                    AnchorTarget.DEFAULT.getValue());

    private static final String ROUTER_IGNORE_ATTRIBUTE = "router-ignore";
    private Serializable href;

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
     * Creates an anchor component with the given target, text content and href.
     *
     * @see #setHref(String)
     * @see #setText(String)
     * @see #setTarget(AnchorTargetValue)
     *
     * @param href
     *            the href to set
     * @param text
     *            the text content to set
     * @param target
     *            the target window, tab or frame
     */
    public Anchor(String href, String text, AnchorTarget target) {
        setHref(href);
        setText(text);
        setTarget(target);
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
     * Creates an anchor component with the given text content and a callback
     * that handles data download from the server to the client when clicking an
     * anchor.
     *
     * @see #setHref(DownloadHandler)
     * @see #setText(String)
     *
     * @param downloadHandler
     *            the callback that handles data download, not null
     * @param text
     *            the text content to set
     */
    public Anchor(DownloadHandler downloadHandler, String text) {
        setHref(downloadHandler);
        setText(text);
    }

    /**
     * Creates an anchor component with the given href and components as
     * children of this component.
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
     * A disabled Anchor removes the attribute from the HTML element, but it is
     * stored (and reused when enabled again) in the server-side component.
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
        if (href == null) {
            throw new IllegalArgumentException("Href must not be null");
        }
        this.href = href;
        assignHrefAttribute();
    }

    /**
     * Removes href attribute.
     *
     * @see Anchor#setHref(String)
     *
     */
    public void removeHref() {
        getElement().removeAttribute("href");
        href = null;
    }

    /**
     * Sets the URL that this anchor links to with the URL of the given
     * {@link StreamResource}.
     *
     * @param href
     *            the resource value, not null
     */
    public void setHref(AbstractStreamResource href) {
        this.href = href;
        setRouterIgnore(true);
        assignHrefAttribute();
    }

    /**
     * Sets the URL that this anchor links to and that is bound to a given
     * {@link DownloadHandler} callback on the server for handling data download
     * from the server to the client when clicking an anchor.
     *
     * @param downloadHandler
     *            the callback that handles data download, not null
     */
    public void setHref(DownloadHandler downloadHandler) {
        this.href = new StreamResourceRegistry.ElementStreamResource(
                downloadHandler, this.getElement());
        setRouterIgnore(true);
        assignHrefAttribute();
    }

    /**
     * The routing mechanism in Vaadin by default intercepts all anchor elements
     * with relative URL. This method can be used make the router ignore this
     * anchor and this way make this anchor behave normally and cause a full
     * page load.
     *
     * @param ignore
     *            true if this link should not be intercepted by the single-page
     *            web application routing mechanism in Vaadin.
     */
    public void setRouterIgnore(boolean ignore) {
        getElement().setAttribute(ROUTER_IGNORE_ATTRIBUTE, ignore);
    }

    /**
     * @return true if this anchor should be ignored by the Vaadin router and
     *         behave normally.
     */
    public boolean isRouterIgnore() {
        return getElement().hasAttribute(ROUTER_IGNORE_ATTRIBUTE);
    }

    /**
     * Gets the URL that this anchor links to.
     *
     * @see #setHref(String)
     *
     * @return the href value, or <code>""</code> if no href has been set
     */
    public String getHref() {
        if (href instanceof String) {
            // let the method return the actual href string even if disabled
            return (String) href;
        } else if (href instanceof AbstractStreamResource) {
            return StreamResourceRegistry.getURI((AbstractStreamResource) href)
                    .toString();
        }
        return get(hrefDescriptor);
    }

    @Override
    public void onEnabledStateChanged(boolean enabled) {
        super.onEnabledStateChanged(enabled);
        assignHrefAttribute();
    }

    private void assignHrefAttribute() {
        if (isEnabled()) {
            if (href != null) {
                if (href instanceof AbstractStreamResource) {
                    getElement().setAttribute("href",
                            (AbstractStreamResource) href);
                } else {
                    set(hrefDescriptor, (String) href);
                }
            }
        } else {
            getElement().removeAttribute("href");
        }
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

    /**
     * Sets the target window, tab or frame for this anchor. The target may be
     * the one of these special values:
     * <ul>
     * <li><code>AnchorTarget.DEFAULT</code>: Removes the target value. This has
     * the same effect as setting the target to <code>AnchorTarget.SELF</code>.
     * <li><code>AnchorTarget.SELF</code>: Opens the link in the current
     * context.
     * <li><code>AnchorTarget.BLANK</code>: Opens the link in a new unnamed
     * context.
     * <li><code>AnchorTarget.PARENT</code>: Opens the link in the parent
     * context, or the current context if there is no parent context.
     * <li><code>AnchorTarget.TOP</code>: Opens the link in the top most
     * grandparent context, or the current context if there is no parent
     * context.
     * </ul>
     *
     * @param target
     *            the target value, not null
     */
    public void setTarget(AnchorTargetValue target) {
        Objects.requireNonNull(target, "target cannot be null.");
        setTarget(target.getValue());
    }

    /**
     * Gets the target window, tab or frame value for this anchor.
     *
     * @see #setTarget(AnchorTargetValue)
     * @see #getTarget()
     *
     * @return the target window value , or {@link AnchorTarget#DEFAULT} if no
     *         target has been set
     */
    public AnchorTargetValue getTargetValue() {
        Optional<String> target = getTarget();

        if (target.isPresent()) {
            return AnchorTargetValue.forString(target.get());
        }
        return AnchorTarget.DEFAULT;
    }

}
