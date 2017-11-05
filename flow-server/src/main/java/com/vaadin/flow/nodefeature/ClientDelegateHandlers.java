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
package com.vaadin.flow.nodefeature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import com.vaadin.flow.StateNode;
import com.vaadin.ui.Component;
import com.vaadin.ui.common.ClientDelegate;

/**
 * Methods which are published as <code>element.$server.&lt;name&gt;</code> on
 * the client side.
 *
 * @author Vaadin Ltd
 *
 */
public class ClientDelegateHandlers extends AbstractServerHandlers<Component> {

    /**
     * Creates a new meta information list for the given state node.
     *
     * @param node
     *            the state node this list belongs to
     */
    public ClientDelegateHandlers(StateNode node) {
        super(node);
    }

    @Override
    protected Class<? extends Annotation> getHandlerAnnotation() {
        return ClientDelegate.class;
    }

    @Override
    protected void ensureSupportedParameterTypes(Method method) {
        // decoder may be able to convert any value to any type so no need to
        // limit supported types
    }

}
