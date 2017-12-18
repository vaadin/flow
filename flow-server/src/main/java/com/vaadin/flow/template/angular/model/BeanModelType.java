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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.vaadin.flow.dom.impl.TemplateElementStateProvider;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ModelMap;
import com.vaadin.flow.templatemodel.BasicModelType;
import com.vaadin.flow.templatemodel.ComplexModelType;
import com.vaadin.flow.templatemodel.InvalidTemplateModelException;
import com.vaadin.flow.templatemodel.ModelType;
import com.vaadin.flow.templatemodel.PropertyFilter;
import com.vaadin.flow.templatemodel.TemplateModelUtil;
import com.vaadin.flow.util.ReflectTools;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * A model type corresponding to a Java bean type.
 *
 * @author Vaadin Ltd
 * @param <T>
 *            the proxy type used by this bean type
 * @deprecated do not use! feature is to be removed in the near future
 */
@Deprecated
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

    /**
     * Nashorn proxy object that uses a {@link BeanModelType} to delegate
     * JavaScript properties accesses to a {@link ModelMap}. Nashorn exposes
     * instances of this type as opaque JavaScript objects, while internally
     * delegating to the handler methods in this class.
     */
    @SuppressWarnings("restriction")
    private static final class StateNodeWrapper
            extends jdk.nashorn.api.scripting.AbstractJSObject {

        private ModelMap model;
        private BeanModelType<?> type;

        public StateNodeWrapper(ModelMap model, BeanModelType<?> type) {
            assert type != null;
            assert model != null;
            this.model = model;
            this.type = type;
        }

        @Override
        public boolean hasMember(String name) {
            return type.hasProperty(name);
        }

        @Override
        public Object getMember(String name) {
            assert hasMember(name);

            return type.getPropertyType(name)
                    .modelToNashorn(model.getValue(name));
        }
    }

    private final Map<String, ModelType> properties;
    private final Class<T> proxyType;

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
                + propertyType + " is not supported. Used in class "
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
        } else {
            if (!(itemType instanceof Class<?>)) {
                throw new InvalidTemplateModelException("Element type "
                        + itemType.getTypeName()
                        + " is not a valid Bean type. Used in class "
                        + declaringClass.getSimpleName()
                        + " with property named " + propertyName
                        + " with list type " + propertyType.getTypeName());
            }
            Class<?> beansListItemType = (Class<?>) itemType;
            return new ListModelType<>(
                    new BeanModelType<>(beansListItemType, propertyFilter));
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
        return TemplateModelProxyHandler
                .createModelProxy((StateNode) modelValue, this);
    }

    @Override
    public Object modelToNashorn(Serializable modelValue) {
        if (modelValue == null) {
            return null;
        } else {
            ModelMap modelMap = ModelMap.get((StateNode) modelValue);
            return new StateNodeWrapper(modelMap, this);
        }
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

        StateNode node = TemplateElementStateProvider
                .createSubModelNode(ModelMap.class);

        importProperties(ModelMap.get(node), applicationValue, filter);

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
    public void importProperties(ModelMap model, Object bean,
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
        properties.keySet().stream().filter(propertyFilter)
                .map(name -> ReflectTools.getGetter(beanClass, name))
                .filter(Optional::isPresent).map(Optional::get)
                .forEach(getter -> {
                    String propertyName = ReflectTools.getPropertyName(getter);

                    Type beanPropertyType = ReflectTools
                            .getPropertyType(getter);
                    ModelType modelPropertyType = getPropertyType(propertyName);
                    if (!modelPropertyType.accepts(beanPropertyType)) {
                        throw new IllegalArgumentException(
                                propertyName + " is not of the expected type");
                    }

                    try {
                        Object value = getter.invoke(bean);
                        values.put(propertyName, value);
                    } catch (Exception e) {
                        throw new IllegalArgumentException(
                                "Cannot access bean property " + propertyName,
                                e);
                    }
                });

        // Populate the model with the extracted values
        values.forEach((name, value) -> {
            ModelType type = getPropertyType(name);
            model.setValue(name, type.applicationToModel(value,
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

    @Override
    public void createInitialValue(StateNode node, String property) {
    }

}
