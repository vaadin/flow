/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.component.trigger;

import java.util.Objects;

import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.internal.ConfigContext;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonUtils;

/**
 * Calls {@code target.click()} on a target component, dispatching a synthetic
 * click event on its root element. Typically used to chain a trigger onto
 * another component's click handling — for example, a shortcut that fires the
 * same path as a button press.
 */
public class ClickAction extends AbstractAction {

    public static final String TYPE_ID = "flow:click";

    private final Element target;

    /**
     * Creates a click action that clicks the given target component.
     *
     * @param target
     *            the component to click, not {@code null}
     */
    public ClickAction(Component target) {
        super(TYPE_ID);
        this.target = Objects.requireNonNull(target).getElement();
    }

    /**
     * @return the target element
     */
    public Element getTarget() {
        return target;
    }

    @Override
    public ObjectNode buildClientConfig(ConfigContext context) {
        ObjectNode node = JacksonUtils.createObjectNode();
        node.put("element", context.referenceElement(target));
        return node;
    }
}
