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

/**
 * A template AST node representing a text node.
 *
 * @since
 * @author Vaadin Ltd
 */
public class TextTemplateNode extends TemplateNode {

    private final TemplateBinding binding;

    /**
     * Creates a new text node with the given content binding.
     *
     * @param parent
     *            the parent node of this node, or <code>null</code> if this
     *            node is the root of a template tree
     * @param binding
     *            the binding for the text content
     */
    public TextTemplateNode(TemplateNode parent, TemplateBinding binding) {
        super(parent);
        assert binding != null;

        this.binding = binding;
    }

    /**
     * Gets the text content binding.
     *
     * @return the text content binding, not <code>null</code>
     */
    public TemplateBinding getTextBinding() {
        return binding;
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public TemplateNode getChild(int index) {
        throw new IndexOutOfBoundsException();
    }
}
