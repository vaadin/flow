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
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.BasicTypeValue;

/**
 * A model type representing an immutable leaf value (e.g. strings, numbers or
 * booleans) to use them in a list.
 * <p>
 * There is a similar class {@link BasicModelType} which do the same but it
 * keeps handles the values as is. This class wraps them into {@link StateNode}
 * to be able to use them in side lists.
 *
 * @see BasicModelType
 *
 * @param <T>
 *            the (basic) Java type used by this model type
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class BasicComplexModelType<T> extends AbstractBasicModelType<T>
        implements ComplexModelType<T> {
    @SuppressWarnings("unchecked")
    private static final Map<Class<?>, BasicComplexModelType<?>> TYPES = loadBasicTypes(
            BasicComplexModelType::new);

    private BasicComplexModelType(Class<T> type) {
        super(type);
    }

    /**
     * Gets the basic model type definition for the given Java class.
     *
     * @param type
     *            the Java class to find a basic model type for
     * @return the basic model type, or an empty optional if the provided type
     *         is not a basic type
     */
    public static Optional<ComplexModelType<?>> get(Class<?> type) {
        return Optional.ofNullable(TYPES.get(type));
    }

    /**
     * Checks whether the {@code type} is a basic supported type.
     *
     * @param type
     *            type to check
     * @return {@code true} is the {@code type} is basic supported type,
     *         {@code false} otherwise
     */
    public static boolean isBasicType(Type type) {
        return TYPES.keySet().contains(type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T modelToApplication(Serializable modelValue) {
        assert modelValue instanceof StateNode;
        StateNode stateNode = (StateNode) modelValue;
        assert stateNode.hasFeature(BasicTypeValue.class);
        Serializable value = stateNode.getFeature(BasicTypeValue.class)
                .getValue();

        return (T) convertToApplication(value);
    }

    @Override
    public StateNode applicationToModel(Object applicationValue,
            PropertyFilter filter) {
        if (applicationValue == null) {
            return null;
        }
        assert applicationValue instanceof Serializable;

        StateNode stateNode = new StateNode(
                Collections.singletonList(BasicTypeValue.class));
        stateNode.getFeature(BasicTypeValue.class)
                .setValue((Serializable) applicationValue);
        return stateNode;
    }

    @Override
    public <C> ComplexModelType<C> cast(Class<C> proxyType) {
        if (getJavaType() != proxyType) {
            throw new IllegalArgumentException(
                    "Got " + proxyType + ", expected " + getJavaType());
        }
        return (BasicComplexModelType<C>) this;
    }

}
