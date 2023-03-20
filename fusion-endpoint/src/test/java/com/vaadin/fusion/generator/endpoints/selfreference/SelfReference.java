/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.selfreference;

public class SelfReference {
    private String name;
    private SelfReference[] children;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SelfReference[] getChildren() {
        return children;
    }

    public void setChildren(SelfReference[] children) {
        this.children = children;
    }
}
