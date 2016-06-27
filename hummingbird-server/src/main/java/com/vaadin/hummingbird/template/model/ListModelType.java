/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.template.model;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.impl.TemplateElementStateProvider;
import com.vaadin.hummingbird.nodefeature.ModelList;

/**
 * A model type corresponding to a list of bean types.
 *
 * @author Vaadin Ltd
 * @param <T>
 *            the proxy type used by the bean type of this type
 */
public class ListModelType<T> implements ModelType {

    private BeanModelType<T> itemType;

    /**
     * Creates a new list model type with the given bean model type.
     *
     * @param itemType
     *            the model type of the list items
     */
    public ListModelType(BeanModelType<T> itemType) {
        assert itemType != null;
        this.itemType = itemType;
    }

    /**
     * Gets the item type.
     *
     * @return the item type, not <code>null</code>
     */
    public BeanModelType<T> getItemType() {
        return itemType;
    }

    @Override
    public List<T> modelToApplication(Serializable modelValue) {
        return new TemplateModelListProxy<>((StateNode) modelValue, itemType);
    }

    @Override
    public StateNode applicationToModel(Object applicationValue,
            PropertyFilter filter) {
        if (applicationValue == null) {
            return null;
        }

        StateNode node = TemplateElementStateProvider
                .createSubModelNode(ModelList.class);

        @SuppressWarnings("unchecked")
        List<T> list = (List<T>) applicationValue;

        importBeans(node.getFeature(ModelList.class), list, filter);

        return node;
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

    /**
     * Gets the type of items for the given list type.
     *
     * @param type
     *            the type to check, must be a list of beans
     * @return the item type of the list
     */
    public static Class<?> getBeansListItemType(Type type) {
        assert isList(type);

        ParameterizedType pt = (ParameterizedType) type;

        Type itemType = pt.getActualTypeArguments()[0];
        if (!(itemType instanceof Class<?>)) {
            throw new InvalidTemplateModelException(
                    "The given type is not a beans list type: " + type);
        }
        return (Class<?>) itemType;

    }

    @Override
    public boolean accepts(Type applicationType) {
        return isList(applicationType);
    }
}
