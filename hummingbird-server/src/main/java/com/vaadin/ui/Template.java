/*
 * Copyright 2000-2014 Vaadin Ltd.
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
package com.vaadin.ui;

import java.util.List;

import com.vaadin.hummingbird.kernel.BoundElementTemplate;
import com.vaadin.hummingbird.kernel.Element;
import com.vaadin.hummingbird.kernel.ElementTemplate;
import com.vaadin.hummingbird.kernel.StateNode;
import com.vaadin.hummingbird.parser.EventBinding;
import com.vaadin.hummingbird.parser.TemplateParser;

import elemental.json.JsonObject;

public abstract class Template extends AbstractComponent {
    private final StateNode node = StateNode.create();

    public Template() {
        setElement(Element.getElement(TemplateParser.parse(getClass()), node));

        getNode().put(TemplateEventHandler.class, this::handleTemplateEvent);
    }

    private void handleTemplateEvent(StateNode node, ElementTemplate template,
            String eventType, JsonObject eventData) {
        Element element = Element.getElement(template, node);
        List<EventBinding> eventBindings = ((BoundElementTemplate) template)
                .getEventBindings(eventType);
        for (EventBinding eventBinding : eventBindings) {
            String methodName = eventBinding.getMethodName();
            List<String> paramsDefinitions = eventBinding.getParams();

            Object[] params = new Object[paramsDefinitions.size()];
            for (int i = 0; i < params.length; i++) {
                String definition = paramsDefinitions.get(i);
                if ("element".equals(definition)) {
                    params[i] = element;
                } else {
                    params[i] = eventData.get(definition);
                }
            }

            onBrowserEvent(node, element, methodName, params);
        }
    }

    protected void onBrowserEvent(StateNode node, Element element, String methodName, Object[] params) {
        // Default does nothing
    }

    protected StateNode getNode() {
        return node;
    }

}
