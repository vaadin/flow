/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.dom;

import java.util.HashMap;
import java.util.Map;

public class TestNodeVisitor implements NodeVisitor {

    private final Map<Node<?>, ElementType> visited = new HashMap<>();
    private final boolean visitDescendants;

    public TestNodeVisitor(boolean visitDescendants) {
        this.visitDescendants = visitDescendants;
    }

    public Map<Node<?>, ElementType> getVisited() {
        return visited;
    }

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
