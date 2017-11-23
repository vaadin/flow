/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.client.flow.template;

import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.binding.BinderContext;

/**
 * A {@link BinderContext} extension which provides template root node for
 * template strategies.
 *
 * @author Vaadin Ltd
 *
 */
public interface TemplateBinderContext extends BinderContext {

    /**
     * Gets the template root {@link StateNode} in the current context.
     * <p>
     * A {@link StateNode} instance passed to {@link BinderContext} as argument
     * to binding methods may be just a model node which contains only model
     * data. Sometimes it's not enough and some data from template (represented
     * by its {@link StateNode}) is required. This method provides this template
     * node in the current context.
     * <p>
     * Template node is required e.g. when template metadata information is
     * required: to be able to find event handler methods (which are defined for
     * the template {@link StateNode} only).
     *
     * @return the template root node
     */
    StateNode getTemplateRoot();
}
