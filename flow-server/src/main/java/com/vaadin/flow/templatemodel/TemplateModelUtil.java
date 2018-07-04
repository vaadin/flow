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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;
import com.vaadin.flow.internal.nodefeature.ModelList;

/**
 * Utility class for mapping Bean values to {@link TemplateModel} values.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public final class TemplateModelUtil {

    private TemplateModelUtil() {
        // NOOP
    }

    /**
     * Resolves a bean model type and model map based on a model instance and
     * passes those values to the provided callback.
     *
     * @param <R>
     *            the return type
     * @param model
     *            the model instance for which to resolve a type and a map, not
     *            <code>null</code>
     * @param modelPath
     *            the model path to resolve, not <code>null</code>
     * @param callback
     *            the callback to run with the resolved bean type and the model
     *            map, not <code>null</code>
     * @return the value returned by the callback
     */
    public static <R> R resolveBeanAndRun(TemplateModel model, String modelPath,
            BiFunction<BeanModelType<?>, ElementPropertyMap, R> callback) {
        assert model != null;
        assert modelPath != null;
        assert callback != null;

        BeanModelType<?> modelType = TemplateModelProxyHandler
                .getModelTypeForProxy(model);

        ModelType beanType = modelType.resolveType(modelPath);
        if (beanType instanceof BeanModelType<?>) {
            StateNode stateNode = TemplateModelProxyHandler
                    .getStateNodeForProxy(model);
            ElementPropertyMap modelMap = ElementPropertyMap
                    .getModel(stateNode);

            ElementPropertyMap beanMap = modelMap.resolveModelMap(modelPath);

            return callback.apply((BeanModelType<?>) beanType, beanMap);
        } else {
            throw new IllegalArgumentException(
                    modelPath + " does not resolve to a bean");
        }
    }

    /**
     * Resolves a list model type and a model list based on a model instance and
     * passes those to the provided callback.
     *
     * @param <R>
     *            the return type
     * @param model
     *            the model instance for which to resolve a type and a list, not
     *            <code>null</code>
     * @param modelPath
     *            the model path to resolve, not <code>null</code>
     * @param callback
     *            the callback to run with the resolved list type and model
     *            list, not <code>null</code>
     * @return the value returned by the callback
     */
    public static <R> R resolveListAndRun(TemplateModel model, String modelPath,
            BiFunction<ListModelType<?>, ModelList, R> callback) {
        assert model != null;
        assert modelPath != null;
        assert callback != null;

        BeanModelType<?> modelType = TemplateModelProxyHandler
                .getModelTypeForProxy(model);

        ModelType listType = modelType.resolveType(modelPath);
        if (listType instanceof ListModelType<?>) {
            StateNode stateNode = TemplateModelProxyHandler
                    .getStateNodeForProxy(model);
            ElementPropertyMap modelMap = ElementPropertyMap
                    .getModel(stateNode);

            ModelList modelList = modelMap.resolveModelList(modelPath);

            return callback.apply((ListModelType<?>) listType, modelList);
        } else {
            throw new IllegalArgumentException(
                    modelPath + " does not resolve to a list");
        }
    }

    /**
     * Gets a filter based on any <code>@Include</code> and/or
     * <code>@Exclude</code> annotations present on the given method.
     *
     * @param method
     *            the method to check
     * @return a filter based on the given annotations
     */
    public static Predicate<String> getFilterFromIncludeExclude(Method method) {
        Exclude exclude = method.getAnnotation(Exclude.class);
        Include include = method.getAnnotation(Include.class);
        Set<String> toExclude = new HashSet<>();
        Set<String> toInclude = new HashSet<>();

        if (exclude != null) {
            Collections.addAll(toExclude, exclude.value());
        }

        if (include != null) {
            for (String includeProperty : include.value()) {
                toInclude.add(includeProperty);

                // If "some.bean.value" is included,
                // we should automatically include "some" and "some.bean"
                String property = includeProperty;
                int dotLocation = property.lastIndexOf('.');
                while (dotLocation != -1) {
                    property = property.substring(0, dotLocation);
                    toInclude.add(property);
                    dotLocation = property.lastIndexOf('.');
                }
            }
        }

        return propertyName -> {
            if (toExclude.contains(propertyName)) {
                return false;
            }

            if (!toInclude.isEmpty()) {
                return toInclude.contains(propertyName);
            }

            return true;
        };
    }
}
