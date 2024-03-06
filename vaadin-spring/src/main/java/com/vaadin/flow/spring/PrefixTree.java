/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
