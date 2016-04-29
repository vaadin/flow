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
package com.vaadin.hummingbird.template;

import com.vaadin.hummingbird.dom.ElementStateProvider;
import com.vaadin.hummingbird.dom.impl.TemplateTextElementStateProvider;

import elemental.json.JsonObject;

/**
 * A template AST node representing a text node.
 *
 * @author Vaadin Ltd
 */
public class TextTemplateNode extends SingleElementTemplateNode {
    /**
     * Type value for text template nodes in JSON messages.
     */
    public static final String TYPE = "text";

    /**
     * Key used for the text binding definition in JSON messages.
     */
    public static final String BINDING_KEY = "binding";

    private final TemplateBinding textBinding;

    /**
     * Creates a new text node with the given content binding.
     *
     * @param parent
     *            the parent node of this node, or <code>null</code> if this
     *            node is the root of a template tree
     * @param textBinding
     *            the binding for the text content
     */
    public TextTemplateNode(TemplateNode parent, TemplateBinding textBinding) {
        super(parent);
        assert textBinding != null;

        this.textBinding = textBinding;
    }

    /**
     * Gets the text content binding.
     *
     * @return the text content binding, not <code>null</code>
     */
    public TemplateBinding getTextBinding() {
        return textBinding;
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public TemplateNode getChild(int index) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    protected ElementStateProvider createStateProvider() {
        return new TemplateTextElementStateProvider(this);
    }

    @Override
    protected void populateJson(JsonObject json) {
        json.put(TemplateNode.KEY_TYPE, TYPE);

        json.put(BINDING_KEY, textBinding.toJson());
    }
}
