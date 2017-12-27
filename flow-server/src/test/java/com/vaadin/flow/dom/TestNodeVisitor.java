/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.Node;
import com.vaadin.flow.dom.NodeVisitor;
import com.vaadin.flow.dom.ShadowRoot;

class TestNodeVisitor implements NodeVisitor {

    Map<Node<?>, ElementType> visited = new HashMap<>();

    @Override
    public void visit(ElementType type, Element element) {
        visited.put(element, type);
    }

    @Override
    public void visit(ShadowRoot root) {
        visited.put(root, null);
    }
}
