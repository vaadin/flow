/**
 * Copyright (C) 2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.polymertemplate;

import java.util.Optional;
import java.util.stream.Stream;

import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

/**
 * Utilities for JSOUP DOM manipulations.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @deprecated This class is internal and used only for Polymer templates.
 *             Polymer template support is deprecated - we recommend you to use
 *             {@code LitTemplate} instead. Read more details from <a href=
 *             "https://vaadin.com/blog/future-of-html-templates-in-vaadin">the
 *             Vaadin blog.</a>
 */
@Deprecated
final class JsoupUtils {

    private JsoupUtils() {
        // Utility class
    }

    /**
     * Removes all comments from the {@code node} tree.
     *
     * @param node
     *            a Jsoup node
     */
    static void removeCommentsRecursively(Node node) {
        int i = 0;
        while (i < node.childNodeSize()) {
            Node child = node.childNode(i);
            if (child instanceof Comment) {
                child.remove();
            } else {
                removeCommentsRecursively(child);
                i++;
            }
        }
    }

    /**
     * Finds {@code "dom-module"} element inside the {@code parent}.
     * <p>
     * If {@code id} is provided then {@code "dom-module"} element is searched
     * with the given {@code id} value.
     *
     * @param parent
     *            the parent element
     * @param id
     *            optional id attribute value to search {@code "dom-module"}
     *            element, may be {@code null}
     * @return
     */
    static Optional<Element> getDomModule(Element parent, String id) {
        Stream<Element> stream = parent.getElementsByTag("dom-module").stream();
        if (id != null) {
            stream = stream.filter(element -> id.equals(element.id()));
        }
        return stream.findFirst();
    }

}
