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

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementStateProvider;
import com.vaadin.flow.internal.StateNode;

/**
 * A template node that always represents one element in the DOM tree.
 *
 * @author Vaadin Ltd
 * @deprecated do not use! feature is to be removed in the near future
 */
@Deprecated
public abstract class AbstractElementTemplateNode extends TemplateNode {

    private final ElementStateProvider stateProvider;

    /**
     * Creates a new element template node with the given node as its parent.
     *
     * @param parent
     *            the parent of the new template node, or null if the node is
     *            the root of a template tree
     */
    public AbstractElementTemplateNode(TemplateNode parent) {
        super(parent);

        stateProvider = createStateProvider(this);
    }

    /*
     * Fulgy hack just to avoid complaints from sonarcube about calling
     * overrideable methods in the constructor, which is exactly what we want to
     * do in this case even though it's generally a dangerous operation.
     */
    private static ElementStateProvider createStateProvider(
            AbstractElementTemplateNode node) {
        assert node != null;

        ElementStateProvider stateProvider = node.createStateProvider();

        assert stateProvider != null;

        return stateProvider;
    }

    /**
     * Creates an element state provider that will be used for all elements
     * based on this node. This method is called by the super constructor, so
     * implementations should avoid accessing own internal fields from inside
     * the method.
     *
     * @return the element state provider, not <code>null</code>
     */
    protected abstract ElementStateProvider createStateProvider();

    @Override
    public int getGeneratedElementCount(StateNode templateStateNode) {
        return 1;
    }

    @Override
    public Element getElement(int index, StateNode templateStateNode) {
        if (index != 0) {
            throw new IndexOutOfBoundsException(
                    "0 is the only valid element index");
        }
        return Element.get(templateStateNode, stateProvider);
    }

    @Override
    public Element getParentElement(StateNode node) {
        return getElement(0, node);
    }
}
