/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal;

import org.jsoup.nodes.TextNode;

/**
 * Helpers for HTML related aspects.
 *
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public final class HtmlUtils {

    private HtmlUtils() {
        // avoid instantiation
    }

    /**
     * Escape a string which may contain html.
     *
     * @param maybeHtml
     *            the text to escape
     * @return escaped string
     */
    public static String escape(String maybeHtml) {
        return new TextNode(maybeHtml).outerHtml();
    }
}
