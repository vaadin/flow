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

import java.util.Collection;
import java.util.Collections;

/**
 * Builder for text template nodes.
 *
 * @author Vaadin Ltd
 */
public class TextTemplateBuilder implements TemplateNodeBuilder {

    private BindingValueProvider binding;

    /**
     * Creates a new text template builder.
     *
     * @param binding
     *            the text content binding
     */
    public TextTemplateBuilder(BindingValueProvider binding) {
        assert binding != null;

        this.binding = binding;
    }

    /**
     * Gets the text content binding.
     *
     * @return the text content binding, not <code>null</code>
     */
    public BindingValueProvider getBinding() {
        return binding;
    }

    @Override
    public Collection<TemplateNode> build(TemplateNode parent) {
        return Collections.singleton(new TextTemplateNode(parent, binding));
    }
}
