/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.dom;

import java.util.HashMap;
import java.util.Map;

class TestNodeVisitor implements NodeVisitor {

    Map<Node<?>, ElementType> visited = new HashMap<>();
    boolean visitDescendants = true;

    @Override
    public boolean visit(ElementType type, Element element) {
        visited.put(element, type);
        return visitDescendants;
    }

    @Override
    public boolean visit(ShadowRoot root) {
        visited.put(root, null);
        return visitDescendants;
    }
}
