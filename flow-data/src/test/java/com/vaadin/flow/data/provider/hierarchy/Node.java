/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.provider.hierarchy;

import java.io.Serializable;

public class Node implements Serializable {

    private final Node parent;
    private final int number;

    public Node(int number) {
        this(number, null);
    }

    public Node(int number, Node parent) {
        this.parent = parent;
        this.number = number;
    }

    public Node getParent() {
        return parent;
    }

    public int getNumber() {
        return number;
    }

    public String toString() {
        return number + (parent != null ? " [parent: " + parent + "]" : "");
    }
}
