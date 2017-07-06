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
package com.vaadin.flow.template.model;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.vaadin.flow.StateNode;
import com.vaadin.flow.nodefeature.ElementPropertyMap;
import com.vaadin.flow.util.ReflectionCache;
import com.vaadin.util.ReflectTools;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * A model type corresponding to a Java bean type.
 *
 * @author Vaadin Ltd
 * @param <T>
 *            the proxy type used by this bean type
 */
public class BeanModelType<T> implements ComplexModelType<T> {

    private static class PropertyMapBuilder {
        private static final Function<Method, Predicate<String>> emptyFilterProvider = method -> name -> true;

        private final Map<String, ModelType> properties = new HashMap<>();
        private PropertyFilter propertyFilter;

        private PropertyMapBuilder(PropertyFilter propertyFilter) {
            this.propertyFilter = propertyFilter;
        }

        private void addProperty(Method method,
                Function<Method, Predicate<String>> filterProvider) {
            String propertyName = ReflectTools.getPropertyName(method);
            if (properties.containsKey(propertyName)) {
                return;
            }

            if (!propertyFilter.test(propertyName)) {
                return;
            }

            PropertyFilter innerFilter = new PropertyFilter(propertyFilter,
                    propertyName, filterProvider.apply(method));

            ModelType propertyType = getModelType(
                    ReflectTools.getPropertyType(method), innerFilter,
                    propertyName, method.getDeclaringClass());

            properties.put(propertyName, propertyType);
        }

        private void addProperty(Method method) {
            addProperty(method, emptyFilterProvider);
        }

        private Map<String, ModelType> getProperties() {
            return properties;
        }
    }

    private final Map<String, ModelType> properties;
    private final Class<T> proxyType;

    private transient ReflectionCache<Object, Map<String, Method>> beanPropertyCache;

    /**
     * Creates a new bean model type from the given class and properties.
     *
     * @param proxyType
     *            the class to use for proxies of this type, not
     *            <code>null</code>
     * @param properties
     *            a map of properties of this type. The contents of the map will
     *            be copied. Not <code>null</code>.
     */
    public BeanModelType(Class<T> proxyType,
            Map<String, ModelType> properties) {
        assert proxyType != null;
        assert properties != null;

        this.proxyType = proxyType;
        this.properties = new HashMap<>(properties);

        initBeanPropertyCache();
    }

    /**
     * Creates a new bean model type with the bean properties of the provided
     * class that passes the provided property filter.
     *
     * @param javaType
     *            the java type of this bean type
     * @param propertyFilter
     *            the filter that determines which bean properties to include in
     *            this model type
     */
    public BeanModelType(Class<T> javaType, PropertyFilter propertyFilter) {
        this(javaType, findProperties(javaType, propertyFilter));
    }

    private static Map<String, ModelType> findProperties(Class<?> javaType,
            PropertyFilter propertyFilter) {
        assert javaType != null;
        assert propertyFilter != null;

        PropertyMapBuilder builder = new PropertyMapBuilder(propertyFilter);

        // Check setters first because they might have additional filters
        ReflectTools.getSetterMethods(javaType)
                .forEach(setter -> builder.addProperty(setter,
                        TemplateModelUtil::getFilterFromIncludeExclude));

        // Then go through the getters in case there are readonly-ish properties
        ReflectTools.getGetterMethods(javaType).forEach(builder::addProperty);

        return builder.getProperties();
    }

    private static ModelType getModelType(Type propertyType,
            PropertyFilter propertyFilter, String propertyName,
            Class<?> declaringClass) {
        if (propertyType instanceof Class<?>) {
            Class<?> propertyTypeClass = (Class<?>) propertyType;
            if (isBean(propertyTypeClass)) {
                return new BeanModelType<>(propertyTypeClass, propertyFilter);
            } else {
                Optional<ModelType> maybeBasicModelType = BasicModelType
                        .get(propertyTypeClass);
                if (maybeBasicModelType.isPresent()) {
                    return maybeBasicModelType.get();
                }
            }
        } else if (ListModelType.isList(propertyType)) {
            return getListModelType(propertyType, propertyFilter, propertyName,
                    declaringClass);
        }

        throw new InvalidTemplateModelException("Type "
                + propertyType.toString() + " is not supported. Used in class "
                + declaringClass.getSimpleName() + " with property named "
                + propertyName + ". " + ModelType.getSupportedTypesString());
    }

    private static ModelType getListModelType(Type propertyType,
            PropertyFilter propertyFilter, String propertyName,
            Class<?> declaringClass) {
        assert ListModelType.isList(propertyType);
        ParameterizedType pt = (ParameterizedType) propertyType;

        Type itemType = pt.getActualTypeArguments()[0];
        if (itemType instanceof ParameterizedType) {
            return new ListModelType<>((ComplexModelType<?>) getModelType(
                    itemType, propertyFilter, propertyName, declaringClass));
        } else if (BasicComplexModelType.isBasicType(itemType)) {
            return new ListModelType<>(
                    BasicComplexModelType.get((Class<?>) itemType).get());
        } else if (isBean(itemType)) {
            Class<?> beansListItemType = (Class<?>) itemType;
            return new ListModelType<>(
                    new BeanModelType<>(beansListItemType, propertyFilter));
        } else {
            throw new InvalidTemplateModelException(
                    "Element type " + itemType.getTypeName()
                            + " is not a valid Bean type. Used in class "
                            + declaringClass.getSimpleName()
                            + " with property named " + propertyName
                            + " with list type " + propertyType.getTypeName());
        }
    }

    /**
     * Checks if the given type can be handled as a bean in a model.
     *
     * @param type
     *            the type to check
     * @return <code>true</code> if the given type will be handled as a bean,
     *         <code>false</code> if the given type will be handled as a basic
     *         type or is not supported
     */
    public static boolean isBean(Type type) {
        if (!(type instanceof Class<?>)) {
            return false;
        }
        Class<?> cls = (Class<?>) type;
        if (BasicModelType.get(cls).isPresent()) {
            return false;
        } else if (cls.isPrimitive()) {
            // Primitives can't be beans even if they're not basic types
            return false;
        } else if (cls.isArray()) {
            // Arrays are not beans
            return false;
        }
        return true;
    }

    /**
     * Checks whether this bean type has a property with the given name.
     *
     * @param propertyName
     *            the property name to check
     * @return <code>true</code> if this model has a property with the given
     *         name; <code>false</code> otherwise
     */
    public boolean hasProperty(String propertyName) {
        return properties.containsKey(propertyName);
    }

    /**
     * Gets the type of the property with the given name.
     *
     * @param propertyName
     *            the name of the property to check
     *
     * @return the model type
     */
    public ModelType getPropertyType(String propertyName) {
        assert hasProperty(propertyName);

        return properties.get(propertyName);
    }

    @Override
    public T modelToApplication(Serializable modelValue) {
        if (modelValue instanceof StateNode) {
            return com.vaadin.flow.template.model.TemplateModelProxyHandler
                    .createModelProxy((StateNode) modelValue, this);
        } else if (modelValue instanceof JsonObject) {
            throw new IllegalStateException(String.format(
                    "The stored model value '%s' "
                            + "is a JSON object. It looks like you have receieved a plain "
                            + "JSON from the client side and try to use it as a model. "
                            + "Check your model definition. Client side objects cannot be "
                            + "converted automatically to model bean instances. "
                            + "Most likely you should use JsonValue type for your model property",
                    modelValue));
        } else {
            throw new IllegalStateException(String.format(
                    "The stored model value '%s' "
                            + "cannot be used as a type for a model property",
                    modelValue));
        }
    }

    @Override
    public Object modelToNashorn(Serializable modelValue) {
        throw new UnsupportedOperationException("Obsolete functionality");
    }

    /**
     * Gets the Class that proxies of this bean type should extend.
     *
     * @return the proxy type to use, not <code>null</code>
     */
    public Class<T> getProxyType() {
        return proxyType;
    }

    @Override
    public StateNode applicationToModel(Object applicationValue,
            PropertyFilter filter) {
        if (applicationValue == null) {
            return null;
        }

        StateNode node = new StateNode(
                Collections.singletonList(ElementPropertyMap.class));

        importProperties(ElementPropertyMap.getModel(node), applicationValue,
                filter);

        return node;
    }

    /**
     * Imports properties from a bean into a model map based on the properties
     * in this model type.
     *
     * @param model
     *            the model map to import values into
     * @param bean
     *            the bean to get values from
     * @param propertyFilter
     *            defines which properties from this model type to import
     */
    public void importProperties(ElementPropertyMap model, Object bean,
            PropertyFilter propertyFilter) {
        Class<? extends Object> beanClass = bean.getClass();

        assert isBean(beanClass);

        /*
         * Collect all values and let getters throw before starting to populate
         * the model.
         *
         * Can't use Collectors.toMap() since it disallows null values.
         */
        Map<String, Object> values = new HashMap<>();

        beanPropertyCache.get(beanClass).forEach((propertyName, getter) -> {
            if (!propertyFilter.test(propertyName)) {
                return;
            }

            try {
                Object value = getter.invoke(bean);
                values.put(propertyName, value);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Cannot access bean property " + propertyName, e);
            }
        });

        // Populate the model with the extracted values
        values.forEach((name, value) -> {
            ModelType type = getPropertyType(name);
            model.setProperty(name, type.applicationToModel(value,
                    new PropertyFilter(propertyFilter, name)));
        });
    }

    /**
     * Finds the model type denoted by the given model path.
     *
     * @param modelPath
     *            the model path to resolve, not <code>null</code>
     * @return the model type of the resolved path, not <code>null</code>
     */
    public ModelType resolveType(String modelPath) {
        assert modelPath != null;

        if (modelPath.isEmpty()) {
            return this;
        }

        String[] parts = modelPath.split("\\.", 2);

        String propertyName = parts[0];
        if (!hasProperty(propertyName)) {
            throw new IllegalArgumentException(
                    "No such property: " + propertyName);
        }

        ModelType propertyType = getPropertyType(propertyName);

        if (parts.length == 1) {
            // Last segment of the path
            return propertyType;
        } else {
            String subPath = parts[1];

            if (propertyType instanceof BeanModelType<?>) {
                return ((BeanModelType<?>) propertyType).resolveType(subPath);
            } else {
                throw new IllegalArgumentException(
                        propertyName + " is not a bean");
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C> BeanModelType<C> cast(Class<C> proxyType) {
        if (getProxyType() != proxyType) {
            throw new IllegalArgumentException(
                    "Got " + proxyType + ", expected " + getProxyType());
        }
        return (BeanModelType<C>) this;
    }

    /**
     * Gets the names of all properties in this bean type.
     *
     * @return a stream of property names, not <code>null</code>
     */
    public Stream<String> getPropertyNames() {
        return properties.keySet().stream();
    }

    @Override
    public boolean accepts(Type applicationType) {
        return isBean(applicationType);
    }

    @Override
    public Type getJavaType() {
        return proxyType;
    }

    @Override
    public JsonValue toJson() {
        JsonObject json = Json.createObject();

        properties.forEach((name, type) -> json.put(name, type.toJson()));

        return json;
    }

    private void initBeanPropertyCache() {
        beanPropertyCache = new ReflectionCache<>(this::findBeanGetters);
    }

    private Map<String, Method> findBeanGetters(Class<?> beanType) {
        HashMap<String, Method> getters = new HashMap<>();
        ReflectTools.getGetterMethods(beanType).forEach(getter -> {
            String propertyName = ReflectTools.getPropertyName(getter);
            if (!properties.containsKey(propertyName)) {
                return;
            }

            Type getterType = getter.getGenericReturnType();
            ModelType propertyType = getPropertyType(propertyName);
            if (!propertyType.accepts(getterType)) {
                throw new IllegalArgumentException(String.format(
                        "Expected type %s for property %s but imported type is %s",
                        propertyType.getJavaType().getTypeName(), propertyName,
                        getterType.getTypeName()));
            }

            getters.put(propertyName, getter);
        });

        return getters;
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        initBeanPropertyCache();
    }

}
