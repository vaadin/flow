/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router;

import java.util.Objects;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.dom.Element;

/**
 * Implementations of this interface represent a parent for a navigation target
 * components via the {@link Route#layout()} parameter.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface RouterLayout extends HasElement {

    /**
     * Shows the content of the layout which is the router target component
     * annotated with a {@link Route @Route}.
     * <p>
     * <strong>Note</strong> implementors should not care about old
     * {@code @Route} content, since it's handled separately by
     * {@link #removeRouterLayoutContent(HasElement)} which by default simply
     * removes the old content.
     * </p>
     *
     * @param content
     *            the content component or {@code null} if the layout content is
     *            to be cleared.
     */
    default void showRouterLayoutContent(HasElement content) {
        if (content != null) {
            getElement()
                    .appendChild(Objects.requireNonNull(content.getElement()));
        }
    }

    /**
     * Removes content that should no longer be shown in this router layout. If
     * {@link #showRouterLayoutContent(HasElement)} was previously called with a
     * non-null parameter, then this method will be called with the same
     * parameter immediately before calling
     * {@link #showRouterLayoutContent(HasElement)} again.
     * <p>
     * By default, the old content is removed from its parent using
     * {@link Element#removeFromParent()}.
     *
     * @param oldContent
     *            the old content to remove, not <code>null</code>
     */
    default void removeRouterLayoutContent(HasElement oldContent) {
        oldContent.getElement().removeFromParent();
    }

}
