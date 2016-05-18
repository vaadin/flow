/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.client.hummingbird.template;

import com.vaadin.client.hummingbird.StateNode;
import com.vaadin.client.hummingbird.StateTree;
import com.vaadin.client.hummingbird.binding.BinderContext;

import elemental.client.Browser;
import elemental.dom.Text;

/**
 * Text template binding strategy.
 * 
 * @author Vaadin Ltd
 *
 */
public class TextTemplateBindingStrategy
        extends AbstractTemplateStrategy<Text> {

    @Override
    protected String getTemplateType() {
        return com.vaadin.hummingbird.template.TextTemplateNode.TYPE;
    }

    @Override
    protected Text create(StateTree tree, int templateId) {
        return Browser.getDocument().createTextNode("");
    }

    @Override
    protected void bind(StateNode stateNode, Text node, int templateId,
            BinderContext context) {
        TextTemplateNode templateNode = (TextTemplateNode) getTemplateNode(
                stateNode.getTree(), templateId);
        Binding binding = templateNode.getTextBinding();
        bind(stateNode, binding, value -> node
                .setTextContent(value.map(Object::toString).orElse("")));
    }

}
