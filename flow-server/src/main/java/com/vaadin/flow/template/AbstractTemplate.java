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

package com.vaadin.flow.template;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import com.googlecode.gentyref.GenericTypeReflector;
import com.vaadin.flow.StateNode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.impl.AbstractElementStateProvider;
import com.vaadin.flow.nodefeature.TemplateMap;
import com.vaadin.flow.router.HasChildView;
import com.vaadin.flow.router.View;
import com.vaadin.flow.template.model.ModelDescriptor;
import com.vaadin.flow.template.model.TemplateModel;
import com.vaadin.flow.template.model.TemplateModelProxyHandler;
import com.vaadin.ui.Component;

/**
 * @author Vaadin Ltd.
 *
 * @param <M>
 *            a model class that will be used for template data propagation
 */
public abstract class AbstractTemplate<M extends TemplateModel>
        extends Component implements HasChildView {
    private final StateNode stateNode;

    private transient M model;

    protected AbstractTemplate(AbstractElementStateProvider provider) {
        super(provider);
        this.stateNode = getElement().getNode();
    }

    protected AbstractTemplate(StateNode stateNode) {
        super((Element) null);
        this.stateNode = stateNode;
    }

    /**
     * Returns the {@link TemplateModel model} of this template.
     * <p>
     * The type of the model will be the type that this method returns in the
     * instance it is invoked on - meaning that you should override this method
     * and return your own model type that extends {@link TemplateModel}.
     *
     * @return the model of this template
     * @see TemplateModel
     */
    protected M getModel() {
        if (model == null) {
            model = createTemplateModelInstance();
        }
        return model;
    }

    private M createTemplateModelInstance() {
        ModelDescriptor<? extends M> descriptor = ModelDescriptor
                .get(getModelType());
        updateModelDescriptor(descriptor);
        return TemplateModelProxyHandler.createModelProxy(stateNode,
                descriptor);
    }

    /**
     * Method that allows to update model descriptor, if needed.
     * 
     * @param currentDescriptor
     *            current descriptor for a model
     */
    protected void updateModelDescriptor(
            ModelDescriptor<? extends M> currentDescriptor) {
        // No need to update descriptor by default
    }

    @Override
    public void setChildView(View childView) {
        TemplateMap templateMap = stateNode.getFeature(TemplateMap.class);
        if (childView == null) {
            templateMap.setChild(null);
        } else {
            templateMap.setChild(childView.getElement().getNode());
        }
    }

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
