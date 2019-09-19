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
package com.vaadin.flow.dom.impl;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.dom.Element;

/**
 * Emulates the <code>class</code> attribute by delegating to
 * {@link Element#getClassList()}.
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

        if (value.isEmpty()) {
            return;
        }

        String[] parts = value.split("\\s+");
        classList.addAll(Arrays.asList(parts));
    }

    @Override
    public void removeAttribute(Element element) {
        element.getClassList().clear();
    }
}
