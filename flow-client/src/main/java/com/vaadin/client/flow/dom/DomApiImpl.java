/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.client.flow.dom;

import elemental.dom.Node;

/**
 * A DOM API abstraction layer to be used via {@link DomApi#wrap(Node)}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@FunctionalInterface
public interface DomApiImpl {

    /**
     * Wraps the given DOM node to make it safe to invoke any of the methods
     * from {@link DomNode} or {@link DomElement}.
     *
     * @param node
     *            the node to wrap
     * @return the wrapped element
     */
    DomElement wrap(Node node);
}
