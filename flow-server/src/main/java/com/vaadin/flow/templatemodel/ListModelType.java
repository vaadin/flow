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
package com.vaadin.flow.templatemodel;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.googlecode.gentyref.GenericTypeReflector;

import com.vaadin.flow.internal.JsonUtils;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;
import com.vaadin.flow.internal.nodefeature.ModelList;

import elemental.json.JsonValue;

/**
 * A model type corresponding to a list of bean types.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @param <T>
 *            the proxy type used by the bean type of this type
 */
public class ListModelType<T> implements ComplexModelType<T> {

    private ComplexModelType<T> itemType;

    /**
     * Creates a new list model type with the given bean model type.
     *
     * @param itemType
     *            the model type of the list items
     */
    public ListModelType(ComplexModelType<T> itemType) {
        assert itemType != null;
        this.itemType = itemType;
    }

    /**
     * Gets the item type.
     *
     * @return the item type, not <code>null</code>
     */
    public ComplexModelType<T> getItemType() {
        return itemType;
    }

    @Override
    public List<T> modelToApplication(Serializable modelValue) {
        if (modelValue instanceof StateNode) {
            return new TemplateModelListProxy<>((StateNode) modelValue,
                    itemType);
        } else {
            throw new IllegalArgumentException(String.format(
                    "The stored model value '%s' type '%s' "
                            + "cannot be used as a type for a model list property",
                    modelValue, modelValue.getClass()));
        }
    }

    @Override
    public StateNode applicationToModel(Object applicationValue,
            PropertyFilter filter) {
        if (applicationValue == null) {
            return null;
        }

        StateNode node = new StateNode(
                Collections.singletonList(ModelList.class));

        @SuppressWarnings("unchecked")
        List<T> list = (List<T>) applicationValue;

        importBeans(node.getFeature(ModelList.class), list, filter);

        return node;
    }

    @Override
    public <C> ComplexModelType<C> cast(Class<C> proxyType) {
        if (getItemType() instanceof ListModelType<?>
                && GenericTypeReflector.erase(proxyType).equals(List.class)) {
            return (ComplexModelType<C>) this;
        }
        throw new IllegalArgumentException(
                "Got " + proxyType + ", expected list type");

    }

    /**
     * Checks if the given type will be handled as a list of beans in the model.
     *
     * @param type
     *            the type to check
     * @return <code>true</code> if the given type will be handled as a list of
     *         beans, <code>false</code> otherwise
     */
    public static boolean isList(Type type) {
        return type instanceof ParameterizedType
                && ((ParameterizedType) type).getRawType().equals(List.class);
    }

    /**
     * Imports beans into a model list based on the properties in the item type
     * of this model type.
     *
     * @param modelList
     *            the model list to import beans into
     * @param beans
     *            the list of beans to import
     * @param propertyFilter
     *            defines which properties from the item model type to import
     */
    public void importBeans(ModelList modelList, List<T> beans,
            PropertyFilter propertyFilter) {
        // Collect all child nodes before clearing anything
        List<StateNode> childNodes = new ArrayList<>();
        for (Object bean : beans) {
            StateNode childNode = itemType.applicationToModel(bean,
                    propertyFilter);
            childNodes.add(childNode);
        }

        modelList.clear();

        modelList.addAll(childNodes);
    }

    @Override
    public boolean accepts(Type applicationType) {
        return isList(applicationType);
    }

    @Override
    public Type getJavaType() {
        return ReflectTools.createParameterizedType(List.class,
                getItemType().getJavaType());
    }

    @Override
    public JsonValue toJson() {
        return JsonUtils.createArray(itemType.toJson());
    }

    @Override
    public void createInitialValue(StateNode node, String property) {
        node.getFeature(ElementPropertyMap.class).resolveModelList(property);
    }
}
