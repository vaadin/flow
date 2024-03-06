/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.dom.impl;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.dom.Element;

/**
 * Emulates the <code>class</code> attribute by delegating to
 * {@link Element#getClassList()}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 1.0
 */
public class ClassAttributeHandler extends CustomAttribute {
    @Override
    public boolean hasAttribute(Element element) {
        return !element.getClassList().isEmpty();
    }

    @Override
    public String getAttribute(Element element) {
        Set<String> classList = element.getClassList();
        if (classList.isEmpty()) {
            return null;
        } else {
            return classList.stream().collect(Collectors.joining(" "));
        }
    }

    @Override
    public void setAttribute(Element element, String value) {
        Set<String> classList = element.getClassList();
        classList.clear();

        String classValue = value.trim();

        if (classValue.isEmpty()) {
            return;
        }

        String[] parts = classValue.split("\\s+");
        classList.addAll(Arrays.asList(parts));
    }

    @Override
    public void removeAttribute(Element element) {
        element.getClassList().clear();
    }
}
