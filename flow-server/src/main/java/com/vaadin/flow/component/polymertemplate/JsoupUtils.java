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
package com.vaadin.flow.component.polymertemplate;

import java.util.Optional;
import java.util.stream.Stream;

import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

/**
 * Utilities for JSOUP DOM manipulations.
 *
 * @author Vaadin Ltd
 *
 */
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
