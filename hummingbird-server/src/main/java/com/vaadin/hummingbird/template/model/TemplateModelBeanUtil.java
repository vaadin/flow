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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.impl.TemplateElementStateProvider;
import com.vaadin.hummingbird.nodefeature.ModelList;
import com.vaadin.hummingbird.nodefeature.ModelMap;
import com.vaadin.util.ReflectTools;

/**
 * Utility class for mapping Bean values to {@link TemplateModel} values.
 *
 * @author Vaadin Ltd
 */
public class TemplateModelBeanUtil {

    private static final Class<?>[] SUPPORTED_NON_PRIMITIVE_TYPES = new Class[] {
            Boolean.class, Double.class, Integer.class, String.class };
    private static final Class<?>[] SUPPORTED_PRIMITIVE_TYPES = new Class[] {
            boolean.class, double.class, int.class };
    private static final Set<Class<?>> SUPPORTED_BASIC_TYPES = Stream
            .concat(Stream.of(SUPPORTED_NON_PRIMITIVE_TYPES),
                    Stream.of(SUPPORTED_PRIMITIVE_TYPES))
            .collect(Collectors.toSet());

    /**
     * Internal implementation of Pair / Tuple that encapsulates a value and the
     * method that it was retrieved from.
     */
    private static final class ModelPropertyWrapper {
        private String propertyName;
        private Type propertyType;
        private Object value;

        public ModelPropertyWrapper(String propertyName, Type propertyType,
                Object value) {
            this.propertyName = propertyName;
            this.propertyType = propertyType;
            this.value = value;
        }
    }

    private TemplateModelBeanUtil() {
        // NOOP
    }

    /**
     * Imports a bean into the given path in the model.
     *
     * @param stateNode
     *            the state node containing a model map, to which
     *            <code>modelPath</code> is relative
     * @param modelPath
     *            the path to import the bean to, relative to
     *            <code>modelNode</code>. <code>""</code> to import directly
     *            into the given model map
     * @param beanType
     *            the type of the bean to import
     * @param bean
     *            the bean to import
     * @param filterPrefix
     *            the prefix to use when filtering
     * @param filter
     *            the filter to apply to each property name to decide whether to
     *            include the property (<code>filter</code> returns true) or
     *            ignore the property (<code>filter</code> returns false)
     */
    static void importBean(StateNode modelNode, String modelPath,
            Class<?> beanType, Object bean, String filterPrefix,
            Predicate<String> filter) {
        assert modelNode != null;
        assert modelPath != null;
        assert beanType != null;
        assert filterPrefix != null
                && (filterPrefix.isEmpty() || filterPrefix.endsWith("."));

        if (bean == null) {
            throw new IllegalArgumentException("Bean cannot be null");
        }

        Method[] getterMethods = ReflectTools.getGetterMethods(beanType)
                .toArray(Method[]::new);
        if (getterMethods.length == 0) {
            throw new IllegalArgumentException("Given type "
                    + beanType.getName()
                    + " is not a bean - it has no public getter methods!");
        }

        Predicate<Method> methodBasedFilter = method -> filter
                .test(filterPrefix + ReflectTools.getPropertyName(method));

        List<ModelPropertyWrapper> values = Stream.of(getterMethods)
                .filter(methodBasedFilter)
                .map(method -> mapBeanValueToProperty(method, bean))
                .collect(Collectors.toList());

        // Resolve the state node only after all the bean values have been
        // resolved properly
        ModelMap modelMap = ModelPathResolver.forPath(modelPath)
                .resolveModelMap(modelNode);

        values.forEach(wrapper -> setModelValue(wrapper, modelMap, filterPrefix,
                filter));
    }

    private static void setModelValue(ModelPropertyWrapper modelPropertyWrapper,
            ModelMap targetModelMap, String pathPrefix,
            Predicate<String> filter) {
        setModelValue(targetModelMap, modelPropertyWrapper.propertyName,
                modelPropertyWrapper.propertyType, modelPropertyWrapper.value,
                pathPrefix, filter);
    }

    static void setModelValue(ModelMap modelMap, String propertyName,
            Type modelType, Object value, String filterPrefix,
            Predicate<String> filter) {
        Object oldValue = modelMap.getValue(propertyName);
        // this might cause scenario where invalid type is not
        // caught because both values are null
        if (Objects.equals(value, oldValue)) {
            return;
        }

        if (modelType instanceof Class<?>) {
            setModelValueClass(modelMap, propertyName, (Class<?>) modelType,
                    value, filterPrefix, filter);
        } else if (modelType instanceof ParameterizedType) {
            setModelValueParameterizedType(modelMap, propertyName, modelType,
                    value);
        } else {
            throw createUnsupportedTypeException(modelType, propertyName);
        }
    }

    /**
     * Creates a proxy for the given part of a model.
     *
     * @param stateNode
     *            the state node containing the model, not <code>null</code>
     * @param modelPath
     *            the part of the model to create a proxy for, not
     *            <code>null</code>
     * @param beanClass
     *            the type of the proxy to create, not <code>null</code>
     * @return the model proxy, not <code>null</code>
     */
    public static <T> T getProxy(StateNode stateNode, String modelPath,
            Class<T> beanClass) {

        if (modelPath.isEmpty()) {
            // get the whole model as a bean
            return beanClass.cast(TemplateModelProxyHandler
                    .createModelProxy(stateNode, beanClass));
        }

        ModelPathResolver resolver = ModelPathResolver.forProperty(modelPath);
        ModelMap parentMap = resolver.resolveModelMap(stateNode);
        // Create the state node for the bean if it does not exist
        ModelPathResolver.resolveStateNode(parentMap.getNode(),
                resolver.getPropertyName(), ModelMap.class);

        return beanClass.cast(getModelValue(parentMap,
                resolver.getPropertyName(), beanClass));
    }

    /**
     * Sets the value inside the given model map, identified by the given
     * <code>propertyName</code>, to the given value, interpreted as the given
     * type.
     *
     * @param modelMap
     *            the map containing the property
     * @param propertyName
     *            the name of the property to set
     * @param modelType
     *            the type of the value in the model
     * @param value
     *            the value to set
     * @param filterPrefix
     *            the filtering prefix to apply, if the value is a bean
     * @param filter
     *            the filter to apply on individual properties, if the value is
     *            a bean
     */
    private static void setModelValueClass(ModelMap modelMap,
            String propertyName, Class<?> modelType, Object value,
            String filterPrefix, Predicate<String> filter) {
        assert modelMap != null;
        assert propertyName != null && !propertyName.contains(".");

        Class<?> modelClass = modelType;
        if (isSupportedBasicType(modelClass)) {
            modelMap.setValue(propertyName, (Serializable) value);
        } else if (modelClass.isPrimitive()) {
            // Unsupported primitive
            throw createUnsupportedTypeException(modelType, propertyName);
        } else {
            // Something else, interpret as a bean

            // If this is a sub bean, e.g. getPerson().getAddress() being
            // imported into "item", then filterPrefix will be "item.person."
            // and "propertyName" will be "address"
            String newFilterPrefix = filterPrefix + propertyName + ".";

            importBean(modelMap.getNode(), propertyName, modelClass, value,
                    newFilterPrefix, filter);
        }
    }

    private static void setModelValueParameterizedType(ModelMap modelMap,
            String propertyName, Type expectedType, Object value) {
        ParameterizedType pt = (ParameterizedType) expectedType;

        if (pt.getRawType() != List.class) {
            throw createUnsupportedTypeException(expectedType, propertyName);
        }

        Type itemType = pt.getActualTypeArguments()[0];
        if (!(itemType instanceof Class<?>)) {
            throw createUnsupportedTypeException(expectedType, propertyName);
        }

        importBeans(modelMap.getNode(), propertyName, (List<?>) value,
                (Class<?>) itemType, name -> true);
    }

    /**
     * Imports a list of beans into the model.
     *
     * @param stateNode
     *            the state node to import into
     * @param modelPath
     *            the path defining which part of the model to import into
     * @param beans
     *            the beans to import
     * @param beanType
     *            the type of the beans to import
     * @param propertyNameFilter
     *            a filter determining which bean properties to import
     */
    public static void importBeans(StateNode stateNode, String modelPath,
            List<?> beans, Class<?> beanType,
            Predicate<String> propertyNameFilter) {
        if (isSupportedBasicType(beanType)) {
            // Can only use beans in lists, at least for now
            throw new InvalidTemplateModelException(
                    "Cannot import list into " + modelPath + " since "
                            + beanType.getName() + " it not a bean.");
        }

        // Collect all child nodes before trying to resolve the list node
        List<StateNode> childNodes = new ArrayList<>();
        for (Object bean : beans) {
            StateNode childNode = TemplateElementStateProvider
                    .createSubModelNode(ModelMap.class);
            importBean(childNode, "", beanType, bean, "", propertyNameFilter);
            childNodes.add(childNode);
        }

        ModelList modelList = ModelPathResolver
                .resolveStateNode(stateNode, modelPath, ModelList.class)
                .getFeature(ModelList.class);
        modelList.clear();

        modelList.addAll(childNodes);
    }

    static Object getModelValue(ModelMap modelMap, String propertyName,
            Type returnType) {
        Object value = modelMap.getValue(propertyName);
        if (returnType instanceof Class<?>) {
            return getModelValueBasicType(value, propertyName,
                    (Class<?>) returnType);
        }

        throw createUnsupportedTypeException(returnType, propertyName);
    }

    private static Object getModelValueBasicType(Object value,
            String propertyName, Class<?> returnClazz) {
        if (isSupportedBasicType(returnClazz)) {
            if (returnClazz.isPrimitive() && value == null) {
                return getPrimitiveDefaultValue(returnClazz);
            } else {
                return value;
            }
        } else if (value instanceof StateNode) {
            return TemplateModelProxyHandler.createModelProxy((StateNode) value,
                    returnClazz);
        }
        throw createUnsupportedTypeException(returnClazz, propertyName);
    }

    private static ModelPropertyWrapper mapBeanValueToProperty(
            Method getterMethod, Object bean) {
        try {
            Object value = getterMethod.invoke(bean, (Object[]) null);
            return new ModelPropertyWrapper(
                    ReflectTools.getPropertyName(getterMethod),
                    getterMethod.getGenericReturnType(), value);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Given method was not accesible "
                    + bean.getClass().getName() + "::" + getterMethod.getName(),
                    e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(
                    "Exception thrown while reading bean value from "
                            + bean.getClass().getName() + "::"
                            + getterMethod.getName()
                            + ", getters should not throw exceptions.",
                    e);
        }
    }

    private static String getSupportedTypesString() {
        return SUPPORTED_BASIC_TYPES.stream().map(Class::getName)
                .collect(Collectors.joining(", "))
                + " (and corresponding primitive types)";
    }

    private static boolean isSupportedBasicType(Class<?> clazz) {
        return SUPPORTED_BASIC_TYPES.contains(clazz);
    }

    private static InvalidTemplateModelException createUnsupportedTypeException(
            Type type, String propertyName) {
        return new InvalidTemplateModelException(
                "Template model does not support type " + type.getTypeName()
                        + " (" + propertyName + "), supported types are:"
                        + getSupportedTypesString()
                        + ", beans and lists of beans.");
    }

    private static Object getPrimitiveDefaultValue(Class<?> primitiveType) {
        if (primitiveType == int.class) {
            return Integer.valueOf(0);
        } else if (primitiveType == double.class) {
            return Double.valueOf(0);
        } else if (primitiveType == boolean.class) {
            return false;
        }
        assert !isSupportedBasicType(primitiveType);

        throw new InvalidTemplateModelException(
                "Template model does not support primitive type "
                        + primitiveType.getName()
                        + ", all supported types are: "
                        + getSupportedTypesString());
    }
}
