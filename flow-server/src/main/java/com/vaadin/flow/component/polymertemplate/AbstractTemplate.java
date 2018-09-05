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

package com.vaadin.flow.component.polymertemplate;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import com.googlecode.gentyref.GenericTypeReflector;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.internal.StateNode;

/**
 * @author Vaadin Ltd
 * @since 1.0.
 *
 * @param <M>
 *            a model class that will be used for template data propagation
 */
public abstract class AbstractTemplate<M> extends Component {
    private final StateNode stateNode;

    protected AbstractTemplate() {
        this.stateNode = getElement().getNode();
    }

    protected AbstractTemplate(StateNode stateNode) {
        super(null);
        this.stateNode = stateNode;
    }

    /**
     * Returns the model of this template.
     * <p>
     * The type of the model will be the type that this method returns in the
     * instance it is invoked on - meaning that you should override this method
     * and return your own model type.
     *
     * @return the model of this template
     */
    protected abstract M getModel();

    /**
     * Gets the type of the template model to use with with this template.
     *
     * @return the model type, not <code>null</code>
     */
    @SuppressWarnings("unchecked")
    protected Class<? extends M> getModelType() {
        Type type = GenericTypeReflector.getTypeParameter(
                getClass().getGenericSuperclass(),
                AbstractTemplate.class.getTypeParameters()[0]);
        if (type instanceof Class || type instanceof ParameterizedType) {
            return (Class<M>) GenericTypeReflector.erase(type);
        }
        throw new IllegalStateException(getExceptionMessage(type));
    }

    private static String getExceptionMessage(Type type) {
        if (type == null) {
            return "AbstractTemplate is used as raw type: either add type information or override getModelType().";
        }

        if (type instanceof TypeVariable) {
            return String.format(
                    "Could not determine the composite content type for TypeVariable '%s'. "
                            + "Either specify exact type or override getModelType().",
                    type.getTypeName());
        }
        return String.format(
                "Could not determine the composite content type for %s. Override getModelType().",
                type.getTypeName());
    }

    /**
     * Gets the state node for current template.
     *
     * @return state node
     */
    protected StateNode getStateNode() {
        return stateNode;
    }
}
