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
package com.vaadin.flow.template.angular.model;

import java.util.AbstractList;

import com.vaadin.flow.StateNode;
import com.vaadin.flow.nodefeature.ModelList;
import com.vaadin.flow.model.ComplexModelType;
import com.vaadin.flow.model.PropertyFilter;

/**
 * A list implementation which uses a {@link ModelList} in a {@link StateNode}
 * as the data source.
 *
 * @author Vaadin Ltd
 * @param <T>
 *            the type of items in the list
 */
public class TemplateModelListProxy<T> extends AbstractList<T> {
    private StateNode stateNode;
    private ComplexModelType<T> itemType;

    /**
     * Creates a new proxy for the given node and item type.
     *
     * @param stateNode
     *            the state node containing the model list
     * @param itemType
     *            the type of items in the list
     */
    public TemplateModelListProxy(StateNode stateNode,
            ComplexModelType<T> itemType) {
        this.stateNode = stateNode;
        this.itemType = itemType;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get(int index) {
        StateNode modelNode = getModelList().get(index);
        if (itemType instanceof ListModelType<?>) {
            ComplexModelType<?> listItemType = ((ListModelType<?>) itemType)
                    .getItemType();
            return (T) new TemplateModelListProxy<>(modelNode, listItemType);
        } else if (itemType instanceof BeanModelType<?>) {
            return TemplateModelProxyHandler.createModelProxy(modelNode,
                    (BeanModelType<T>) itemType);
        }
        throw new IllegalStateException("Item type has unexpected type "
                + itemType + ". Don't know how to create a proxy for it");
    }

    @Override
    public T set(int index, T object) {
        T old = remove(index);
        add(index, object);
        return old;
    }

    @Override
    public void add(int index, T object) {
        if (object == null) {
            throw new IllegalArgumentException(
                    "Null values cannot be added to a list in the model");
        }

        StateNode nodeToAdd = itemType.applicationToModel(object,
                PropertyFilter.ACCEPT_ALL);

        getModelList().add(index, nodeToAdd);
    }

    @Override
    public int indexOf(Object object) {
        if (object == null) {
            return -1;
        }
        if (!TemplateModelProxyHandler.isProxy(object)) {
            throw new IllegalArgumentException(
                    "Only proxy objects can be used together with proxy lists");
        }

        StateNode node = TemplateModelProxyHandler.getStateNodeForProxy(object);

        ModelList modelList = getModelList();
        int size = modelList.size();

        for (int i = 0; i < size; i++) {
            if (modelList.get(i).equals(node)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean remove(Object object) {
        int i = indexOf(object);
        if (i == -1) {
            return false;
        }

        remove(i);
        return true;
    }

    @Override
    public T remove(int index) {
        T oldValue = get(index);

        getModelList().remove(index);
        return oldValue;
    }

    @Override
    public int size() {
        return getModelList().size();
    }

    private ModelList getModelList() {
        return stateNode.getFeature(ModelList.class);
    }

}
