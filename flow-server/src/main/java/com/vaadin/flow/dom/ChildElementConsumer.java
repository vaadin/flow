/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.dom;

import java.io.Serializable;
import java.util.function.Consumer;

/**
 * Callback which allows to handle request to map a client side DOM element to
 * the server {@link Element} instance.
 * 
 * @see Node#attachExistingElement(String, Element, ChildElementConsumer)
 * 
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
@FunctionalInterface
public interface ChildElementConsumer extends Consumer<Element>, Serializable {

    /**
     * This callback method is called when the request initiated by the
     * {@link Node#appendChild(Element...)} method is successfully executed.
     * <p>
     * The parameter value is the element created by the request.
     * 
     * @param child
     *            the server side element mapped to the requested client-side
     *            element
     */
    @Override
    void accept(Element child);

    /**
     * This callback method is called when the requested client element cannot
     * be found in the provided {@code parent} by the {@code tag} name after the
     * {@code previousSibling}.
     * 
     * @param parent
     *            the server side parent node, not {@code null}
     * @param tag
     *            the tag name of the requested element, not {@code null}
     * @param previousSibling
     *            the previous sibling element for the requested element, may be
     *            {@code null}
     */
    default void onError(Node<?> parent, String tag, Element previousSibling) {
        if (previousSibling == null) {
            throw new IllegalStateException(String.format(
                    "The element with the tag name '%s' "
                            + "is not found in the parent with id='%d",
                    tag, parent.getNode().getId()));
        }
        throw new IllegalStateException(String.format(
                "The element with the tag name '%s' "
                        + "is not found in the parent with id='%d' after the sibling with id='%d'",
                tag, parent.getNode().getId(),
                previousSibling.getNode().getId()));
    }
}
