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
package com.vaadin.flow.spring;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Quick prefix lookup for package inclusion and exclusion lists.
 */
class PrefixTree implements Serializable {

    private final Node root;

    PrefixTree(Collection<String> prefixes) {
        root = new Node();
        root.terminal = false;
        prefixes.forEach(this::addPrefix);
    }

    void addPrefix(String prefix) {
        if (prefix.isEmpty()) {
            throw new IllegalArgumentException("empty prefix");
        }
        root.addPrefix(prefix);
    }

    boolean hasPrefix(String s) {
        Node node = root;
        final int slen = s.length();
        int sidx = 0;
        while (node != null) {
            if (node.terminal) {
                return true;
            } else if (sidx < slen) {
                node = node.children.get(s.charAt(sidx++));
            } else {
                return false;
            }
        }
        return false;
    }

    static class Node implements Serializable {
        private final Map<Character, Node> children = new HashMap<>();
        private boolean terminal = true;

        void addPrefix(String prefix) {
            terminal = false;
            char ch = prefix.charAt(0);
            children.putIfAbsent(ch, new Node());
            if (prefix.length() > 1) {
                children.get(ch).addPrefix(prefix.substring(1));
            }
        }
    }
}
