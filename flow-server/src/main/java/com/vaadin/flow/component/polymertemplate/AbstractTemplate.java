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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.googlecode.gentyref.GenericTypeReflector;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;
import com.vaadin.flow.templatemodel.BeanModelType;
import com.vaadin.flow.templatemodel.ListModelType;
import com.vaadin.flow.templatemodel.ModelDescriptor;
import com.vaadin.flow.templatemodel.ModelType;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.templatemodel.TemplateModelProxyHandler;

import elemental.json.Json;
import elemental.json.JsonArray;

/**
 * @author Vaadin Ltd
 * @since 1.0.
 *
 * @param <M>
 *            a model class that will be used for template data propagation
 */
public abstract class AbstractTemplate<M extends TemplateModel>
        extends Component {
    private final StateNode stateNode;
    private transient M model;

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
    protected M getModel() {
        if (model == null) {
            model = createTemplateModelInstance();
        }
        return model;
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

    /**
     * Check if the given Class {@code type} is found in the Model.
     *
     * @param type
     *            Class to check support for
     * @return True if supported by this PolymerTemplate
     */
    public boolean isSupportedClass(Class<?> type) {
        List<ModelType> modelTypes = ModelDescriptor.get(getModelType())
                .getPropertyNames().map(this::getModelType)
                .collect(Collectors.toList());

        boolean result = false;
        for (ModelType modelType : modelTypes) {
            if (type.equals(modelType.getJavaType())) {
                result = true;
            } else if (modelType instanceof ListModelType) {
                result = checkListType(type, modelType);
            }
            if (result) {
                break;
            }
        }
        return result;
    }

    private static boolean checkListType(Class<?> type, ModelType modelType) {
        if (type.isAssignableFrom(List.class)) {
            return true;
        }
        ModelType model = modelType;
        while (model instanceof ListModelType) {
            model = ((ListModelType<?>) model).getItemType();
        }
        return type.equals(model.getJavaType());
    }

    private ModelType getModelType(String type) {
        return ModelDescriptor.get(getModelType()).getPropertyType(type);
    }

    /**
     * Get the {@code ModelType} for given class.
     *
     * @param type
     *            Type to get the ModelType for
     * @return ModelType for given Type
     */
    public ModelType getModelType(Type type) {
        List<ModelType> modelTypes = ModelDescriptor.get(getModelType())
                .getPropertyNames().map(this::getModelType)
                .collect(Collectors.toList());

        for (ModelType mtype : modelTypes) {
            if (type.equals(mtype.getJavaType())) {
                return mtype;
            } else if (mtype instanceof ListModelType) {
                ModelType modelType = getModelTypeForListModel(type, mtype);
                if (modelType != null) {
                    return modelType;
                }
            }
        }
        String msg = String.format(
                "Couldn't find ModelType for requested class %s",
                type.getTypeName());
        throw new IllegalArgumentException(msg);
    }

    private M createTemplateModelInstance() {
        ModelDescriptor<? extends M> descriptor = ModelDescriptor
                .get(getModelType());
        return TemplateModelProxyHandler.createModelProxy(getStateNode(),
                descriptor);
    }

    private static ModelType getModelTypeForListModel(Type type,
            ModelType mtype) {
        ModelType modelType = mtype;
        while (modelType instanceof ListModelType) {
            if (type.equals(modelType.getJavaType())) {
                return modelType;
            }
            modelType = ((ListModelType<?>) modelType).getItemType();
        }
        // If type was not a list type then check the bean for List if it
        // matches the type
        if (type.equals(modelType.getJavaType())) {
            return modelType;
        }
        return null;
    }

    protected void initModel(Set<String> twoWayBindingPaths) {
        // Find metadata, fill initial values and create a proxy
        getModel();

        BeanModelType<?> modelType = TemplateModelProxyHandler
                .getModelTypeForProxy(model);

        Map<String, Boolean> allowedProperties = modelType
                .getClientUpdateAllowedProperties(twoWayBindingPaths);

        Set<String> allowedPropertyName = Collections.emptySet();
        if (!allowedProperties.isEmpty()) {
            // copy to avoid referencing a map in the filter below
            allowedPropertyName = new HashSet<>(allowedProperties.keySet());
        }
        ElementPropertyMap.getModel(getStateNode())
                .setUpdateFromClientFilter(allowedPropertyName::contains);

        // remove properties whose values are not StateNode from the property
        // map and return their names as a list
        List<String> propertyNames = removeSimpleProperties();

        // This has to be executed BEFORE model population to be able to know
        // which properties needs update to the server
        getStateNode().runWhenAttached(ui -> ui.getInternals().getStateTree()
                .beforeClientResponse(getStateNode(),
                        context -> context.getUI().getPage().executeJs(
                                "this.registerUpdatableModelProperties($0, $1)",
                                getElement(),
                                filterUpdatableProperties(allowedProperties))));

        /*
         * Now populate model properties on the client side. Only explicitly set
         * by the developer properties are in the map at the moment of execution
         * since all simple properties have been removed from the map above.
         * Such properties are excluded from the argument list and won't be
         * populated on the client side.
         *
         * All explicitly set model properties will be sent from the server as
         * usual and will take precedence over the client side values.
         */
        getStateNode().runWhenAttached(ui -> ui.getInternals().getStateTree()
                .beforeClientResponse(getStateNode(),
                        context -> context.getUI().getPage().executeJs(
                                "this.populateModelProperties($0, $1)",
                                getElement(),
                                filterUnsetProperties(propertyNames))));
    }

    private JsonArray filterUnsetProperties(List<String> properties) {
        JsonArray array = Json.createArray();
        ElementPropertyMap map = getStateNode()
                .getFeature(ElementPropertyMap.class);
        int i = 0;
        for (String property : properties) {
            if (!map.hasProperty(property)) {
                array.set(i, property);
                i++;
            }
        }
        return array;
    }

    /*
     * Keep only properties with getter.
     */
    private JsonArray filterUpdatableProperties(
            Map<String, Boolean> allowedProperties) {
        JsonArray array = Json.createArray();
        int i = 0;
        for (Entry<String, Boolean> entry : allowedProperties.entrySet()) {
            if (entry.getValue()) {
                array.set(i, entry.getKey());
                i++;
            }
        }
        return array;
    }

    private List<String> removeSimpleProperties() {
        ElementPropertyMap map = getStateNode()
                .getFeature(ElementPropertyMap.class);
        List<String> props = map.getPropertyNames()
                .filter(name -> !(map.getProperty(name) instanceof StateNode))
                .collect(Collectors.toList());
        props.forEach(map::removeProperty);
        return props;
    }

}
