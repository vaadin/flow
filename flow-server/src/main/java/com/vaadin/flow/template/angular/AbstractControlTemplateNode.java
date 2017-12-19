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
package com.vaadin.flow.template.angular;

import java.util.Optional;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.StateNode;

/**
 * A template node that generates a different number of child element depending
 * on state node contents.
 *
 * @author Vaadin Ltd
 * @deprecated do not use! feature is to be removed in the near future
 */
@Deprecated
public abstract class AbstractControlTemplateNode extends TemplateNode {

    /**
     * Creates a new node.
     *
     * @param parent
     *            the parent of the new template node, not null
     */
    public AbstractControlTemplateNode(AbstractElementTemplateNode parent) {
        super(parent);
        assert parent != null : "A control node can't be the root of a template";
    }

    @Override
    public Optional<Element> findElement(StateNode stateNode, String id) {
        return Optional.empty();
    }
}
