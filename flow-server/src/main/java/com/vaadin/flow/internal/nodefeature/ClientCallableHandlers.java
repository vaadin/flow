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
package com.vaadin.flow.internal.nodefeature;

import java.lang.reflect.Method;
import java.util.Locale;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.internal.JsonCodec;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.internal.StateNode;

/**
 * Methods which are published as <code>element.$server.&lt;name&gt;</code> on
 * the client side.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class ClientCallableHandlers extends AbstractServerHandlers<Component> {

    /**
     * Creates a new meta information list for the given state node.
     *
     * @param node
     *            the state node this list belongs to
     */
    public ClientCallableHandlers(StateNode node) {
        super(node);
    }

    @Override
    protected Class<? extends ClientCallable> getHandlerAnnotation() {
        return ClientCallable.class;
    }

    @Override
    protected void ensureSupportedParameterTypes(Method method) {
        // decoder may be able to convert any value to any type so no need to
        // limit supported types
    }

    @Override
    protected void ensureSupportedReturnType(Method method) {
        Class<?> returnType = method.getReturnType();
        if (returnType.isPrimitive()) {
            returnType = ReflectTools.convertPrimitiveType(returnType);
        }

        if (!void.class.equals(returnType)
                && !JsonCodec.canEncodeWithTypeInfo(returnType)) {
            String msg = String.format(Locale.ENGLISH,
                    "Only return types that can be used as Element.executeJs parameters are supported. "
                            + "Component '%s' has method '%s' annotated with '%s' whose return type is \"%s\"",
                    method.getDeclaringClass().getName(), method.getName(),
                    getHandlerAnnotation().getName(),
                    method.getReturnType().getSimpleName());
            throw new IllegalStateException(msg);
        }
    }

    @Override
    protected DisabledUpdateMode getUpdateMode(Method method) {
        return method.getAnnotation(getHandlerAnnotation()).value();
    }
}
