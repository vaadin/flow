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

package com.vaadin.hummingbird.template;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Optional;

import com.googlecode.gentyref.GenericTypeReflector;
import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.impl.TemplateElementStateProvider;
import com.vaadin.hummingbird.nodefeature.TemplateMap;
import com.vaadin.hummingbird.router.HasChildView;
import com.vaadin.hummingbird.router.View;
import com.vaadin.hummingbird.template.angular.TemplateNode;
import com.vaadin.hummingbird.template.model.ModelDescriptor;
import com.vaadin.hummingbird.template.model.TemplateModel;
import com.vaadin.hummingbird.template.model.TemplateModelProxyHandler;
import com.vaadin.ui.AttachEvent;
import com.vaadin.ui.Component;

/**
 * @author Vaadin Ltd.
 */
public abstract class AbstractTemplate<M extends TemplateModel>
        extends Component implements HasChildView {
    protected StateNode stateNode = TemplateElementStateProvider
            .createRootNode();

    private transient M model;

    protected AbstractTemplate() {
        super(null);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // initialize the model so that all properties are available in the
        // underlying node's ModelMap
        getModel();
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

        ModelDescriptor<?> oldDescriptor = getModelDescriptor();
        if (oldDescriptor == null) {
            setModelDescriptor(descriptor);
        } else {
            /*
             * Can have an existing descriptor if createTemplateModelInstance
             * has been run previously but the transient model field has been
             * cleared. Let's just verify that we're still seeing the same
             * definition.
             */
            assert oldDescriptor.toJson().toJson()
                    .equals(descriptor.toJson().toJson());
        }

        return TemplateModelProxyHandler.createModelProxy(stateNode,
                descriptor);
    }

    /**
     * Gets current descriptor of a model.
     *
     * @return model descriptor
     */
    protected abstract ModelDescriptor<?> getModelDescriptor();

    /**
     * Sets new descriptor for a model.
     *
     * @param descriptor model descriptor
     */
    protected abstract void setModelDescriptor(ModelDescriptor<?> descriptor);

    /**
     * Finds an element with the given id inside this template.
     *
     * @param id
     *            the id to look for
     * @return an optional element with the id, or an empty Optional if no
     *         element with the given id was found
     */
    public Optional<Element> getElementById(String id) {
        return stateNode.getFeature(TemplateMap.class).getRootTemplate()
                .findElement(stateNode, id);
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
     * Sets root of the template.
     *
     * @param templateRoot
     *            template root to set
     */
    protected void setTemplateRoot(TemplateNode templateRoot) {
        stateNode.getFeature(TemplateMap.class).setRootTemplate(templateRoot);
        Element rootElement = Element.get(stateNode);
        setElement(this, rootElement);
    }
}
