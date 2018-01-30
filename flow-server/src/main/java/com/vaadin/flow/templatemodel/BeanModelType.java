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
package com.vaadin.flow.templatemodel;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.internal.ReflectionCache;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;

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
    private final Map<String, ModelType> properties;
    private final Class<T> proxyType;

    private static final ReflectionCache<Object, Map<String, Method>> beanPropertyCache = new ReflectionCache<>(
            BeanModelType::findBeanGetters);

    /**
     * Creates a new bean model type from the given class and properties.
     *
     * @param proxyType
     *            the class to use for proxies of this type, not
     *            <code>null</code>
     * @param properties
     *            a map of properties of this type. The contents of the map will
     *            be copied. Not <code>null</code>.
     * @param allowEmptyProperties
     *            if {@code false} then empty properties value is not accepted
     *
     * @throws IllegalStateException
     *             if {@code allowEmptyProperties} is {@code false} and
     *             {@code properties} is an empty map
     */
    protected BeanModelType(Class<T> proxyType,
            Map<String, ModelType> properties, boolean allowEmptyProperties) {
        assert proxyType != null;
        assert properties != null;
        if (!allowEmptyProperties && properties.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "No properties are defined for the model bean type '%s'. "
                            + "Such bean is always represented by an empty "
                            + "object during server-client communication. "
                            + "It might be that you are trying to use some "
                            + "abstract super class which is not a bean instead "
                            + "of its direct bean subclass",
                    proxyType.getCanonicalName()));
        }

        this.proxyType = proxyType;

        this.properties = new HashMap<>(properties);
    }

    private BeanModelType(Class<T> javaType, PropertyFilter propertyFilter,
            PathLookup<ModelConverter<?, ?>> converterLookup) {
        this(javaType, new PropertyMapBuilder(javaType, propertyFilter,
                converterLookup).getProperties(), false);
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
     * @param allowEmptyProperties
     *            if {@code false} then empty properties value is not accepted
     * @throws IllegalStateException
     *             if {@code allowEmptyProperties} is {@code false} and
     *             {@code propertyFilter} resolves empty properties
     */
    protected BeanModelType(Class<T> javaType, PropertyFilter propertyFilter,
            boolean allowEmptyProperties) {
        this(javaType,
                new PropertyMapBuilder(javaType, propertyFilter,
                        PathLookup.empty()).getProperties(),
                allowEmptyProperties);
    }

    static ModelType getModelType(Type propertyType,
            PropertyFilter propertyFilter, String propertyName,
            Class<?> declaringClass,
            PathLookup<ModelConverter<?, ?>> converterLookup) {
        if (propertyType instanceof Class<?>) {
            Class<?> propertyTypeClass = (Class<?>) propertyType;
            if (isBean(propertyTypeClass)) {
                return new BeanModelType<>(propertyTypeClass, propertyFilter,
                        converterLookup);
            } else {
                Optional<ModelType> maybeBasicModelType = BasicModelType
                        .get(propertyTypeClass);
                if (maybeBasicModelType.isPresent()) {
                    return maybeBasicModelType.get();
                }
            }
        } else if (ListModelType.isList(propertyType)) {
            return getListModelType(propertyType, propertyFilter, propertyName,
                    declaringClass, converterLookup);
        }

        throw new InvalidTemplateModelException(String.format(
                "Type '%s' is not supported."
                        + " Used in class '%s' with property named '%s'. %s",
                propertyType.toString(), declaringClass.getSimpleName(),
                propertyName, ModelType.getSupportedTypesString()));
    }

    static ModelType getConvertedModelType(Type propertyType,
            PropertyFilter propertyFilter, String propertyName,
            Class<?> declaringClass,
            PathLookup<ModelConverter<?, ?>> converterLookup) {

        if (!(propertyType instanceof Class<?>)) {
            throw new UnsupportedOperationException(String.format(
                    "Using converters with parameterized types is not currently supported."
                            + "Used in class '%s' with property named '%s'",
                    declaringClass.getSimpleName(), propertyName));
        }

        Optional<ModelConverter<?, ?>> converterOptional = converterLookup
                .getItem(propertyFilter.getPrefix());
        if (!converterOptional.isPresent()) {
            throw new IllegalStateException(
                    "The ModelConverterProvider passed to "
                            + "getConvertedModelType is unable to provide a converter "
                            + "for the given PropertyFilter.");
        }

        ModelConverter<?, ?> converter = converterOptional.get();
        if (!converter.getModelType().equals(propertyType)) {
            throw new InvalidTemplateModelException(String.format(
                    "Converter '%s' is incompatible with the type '%s'.",
                    converter.getClass().getName(),
                    propertyType.getTypeName()));
        }

        if (isBean(converter.getPresentationType())) {
            return new ConvertedModelType<>(
                    new BeanModelType<>(converter.getPresentationType(),
                            propertyFilter, converterLookup),
                    converter);
        } else {
            Optional<ModelType> maybeBasicModelType = BasicModelType
                    .get(converter.getPresentationType());
            if (maybeBasicModelType.isPresent()) {
                return new ConvertedModelType<>(maybeBasicModelType.get(),
                        converter);
            }
        }

        throw new InvalidTemplateModelException(String.format(
                "Converter '%s' implements an unsupported model type. "
                        + "Used in class '%s' with property named '%s'. '%s'",
                converter.getClass().getName(), declaringClass.getSimpleName(),
                propertyName, ModelType.getSupportedTypesString()));
    }

    private static ModelType getListModelType(Type propertyType,
            PropertyFilter propertyFilter, String propertyName,
            Class<?> declaringClass,
            PathLookup<ModelConverter<?, ?>> converterLookup) {
        assert ListModelType.isList(propertyType);
        ParameterizedType pt = (ParameterizedType) propertyType;

        Type itemType = pt.getActualTypeArguments()[0];
        if (itemType instanceof ParameterizedType) {
            return new ListModelType<>(
                    (ComplexModelType<?>) getModelType(itemType, propertyFilter,
                            propertyName, declaringClass, converterLookup));
        } else if (BasicComplexModelType.isBasicType(itemType)) {
            return new ListModelType<>(
                    BasicComplexModelType.get((Class<?>) itemType).get());
        } else if (isBean(itemType)) {
            Class<?> beansListItemType = (Class<?>) itemType;
            return new ListModelType<>(new BeanModelType<>(beansListItemType,
                    propertyFilter, converterLookup));
        } else {
            throw new InvalidTemplateModelException(String.format(
                    "Element type '%s' is not a valid Bean type. "
                            + "Used in class '%s' with property named '%s' with list type '%s'.",
                    itemType.getTypeName(), declaringClass.getSimpleName(),
                    propertyName, propertyType.getTypeName()));
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
        if (modelValue == null) {
            return null;
        }
        if (modelValue instanceof StateNode) {
            return TemplateModelProxyHandler
                    .createModelProxy((StateNode) modelValue, this);
        } else if (modelValue instanceof JsonObject) {
            throw new IllegalArgumentException(String.format(
                    "The stored model value '%s' "
                            + "is a JSON object. It looks like you have receieved a plain "
                            + "JSON from the client side and try to use it as a model. "
                            + "Check your model definition. Client side objects cannot be "
                            + "converted automatically to model bean instances. "
                            + "Most likely you should use JsonValue type for your model property",
                    modelValue));
        } else {
            throw new IllegalArgumentException(String.format(
                    "The stored model value '%s' type '%s' "
                            + "cannot be used as a type for a model property",
                    modelValue, modelValue.getClass().getName()));
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
        Class<?> beanClass = bean.getClass();
        assert isBean(beanClass);

        /*
         * Collect all values and let getters throw before starting to populate
         * the model.
         *
         * Can't use Collectors.toMap() since it disallows null values.
         */
        Map<String, Object> values = new HashMap<>();

        beanPropertyCache.get(beanClass).forEach((propertyName, getter) -> {
            if (!hasProperty(propertyName)
                    || !propertyFilter.test(propertyName)) {
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

    @Override
    public void createInitialValue(StateNode node, String property) {
        createInitialValues(node.getFeature(ElementPropertyMap.class)
                .resolveModelMap(property).getNode());
    }

    /**
     * Creates initial values for the given {@code node} using info from this
     * model type.
     * <p>
     * Initial values are created for all sub-properties as well.
     *
     * @param node
     *            the node whose properties need to be populated
     */
    public void createInitialValues(StateNode node) {
        Predicate<Entry<String, Method>> isFinal = entry -> Modifier
                .isFinal(entry.getValue().getModifiers());
        Predicate<Entry<String, Method>> isProperty = entry -> hasProperty(
                entry.getKey());

        StringBuilder builder = new StringBuilder();
        findBeanGetters(getProxyType()).entrySet().stream().filter(isFinal)
                .filter(isProperty).forEach(entry -> writeInvalidAccessor(entry,
                        builder, "getter"));
        findBeanSetters(getProxyType()).entrySet().stream().filter(isFinal)
                .filter(isProperty).forEach(entry -> writeInvalidAccessor(entry,
                        builder, "setter"));
        if (builder.length() > 0) {
            builder.insert(0, "Bean type '" + getProxyType()
                    + "' cannot be used in "
                    + "the template model because it has accessors which cannot be proxied:\n");
            builder.append("Use @").append(Exclude.class.getSimpleName())
                    .append(" or @").append(Include.class.getSimpleName())
                    .append(" annotations to limit properties to use in the model so "
                            + "that all properties with final accessors are excluded from the model");
            throw new IllegalStateException(builder.toString());
        }
        properties.forEach(
                (property, type) -> type.createInitialValue(node, property));
    }

    private void writeInvalidAccessor(Entry<String, Method> entry,
            StringBuilder builder, String accessorType) {
        builder.append("property '").append(entry.getKey())
                .append("' has final ").append(accessorType).append(" '")
                .append(entry.getValue().getName()).append("'\n");
    }

    private static Map<String, Method> findBeanGetters(Class<?> beanType) {
        return ReflectTools.getGetterMethods(beanType)
                .collect(Collectors.toMap(ReflectTools::getPropertyName,
                        Function.identity(), (getter1, getter2) -> {
                            // For the weird case with both isXyz and getXyz,
                            // just use either
                            return getter1;
                        }));
    }

    private static Map<String, Method> findBeanSetters(Class<?> beanType) {
        return ReflectTools.getSetterMethods(beanType).collect(Collectors
                .toMap(ReflectTools::getPropertyName, Function.identity()));
    }

}
