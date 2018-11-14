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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.SerializablePredicate;
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
 * @since 1.0
 * @param <T>
 *            the proxy type used by this bean type
 */
public class BeanModelType<T> implements ComplexModelType<T> {
    static class BeanModelTypeProperty implements Serializable {
        private final ModelType propretyType;
        private final ClientUpdateMode clientUpdateMode;
        private final boolean hasGetter;

        public BeanModelTypeProperty(ModelType propretyType,
                ClientUpdateMode clientUpdateMode, boolean hasGetter) {
            this.propretyType = propretyType;
            this.clientUpdateMode = clientUpdateMode;
            this.hasGetter = hasGetter;
        }

        public ModelType getType() {
            return propretyType;
        }

        public ClientUpdateMode getClientUpdateMode() {
            return clientUpdateMode;
        }

        boolean hasGetter() {
            return hasGetter;
        }
    }

    private final HashMap<String, BeanModelTypeProperty> properties;
    private final Class<T> proxyType;

    private static final ReflectionCache<Object, Map<String, Method>> beanPropertyCache = new ReflectionCache<>(
            BeanModelType::findBeanGetters);

    private static final Set<Class<?>> UNSUPPORTED_BOXED_TYPES = Collections
            .unmodifiableSet(Stream.of(Long.class, Float.class, Byte.class,
                    Character.class, Short.class).collect(Collectors.toSet()));

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
            Map<String, BeanModelTypeProperty> properties,
            boolean allowEmptyProperties) {
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
            PathLookup<ModelEncoder<?, ?>> converterLookup,
            PathLookup<ClientUpdateMode> clientUpdateLookup) {
        this(javaType,
                new PropertyMapBuilder(javaType, propertyFilter,
                        converterLookup, clientUpdateLookup).getProperties(),
                false);
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
                        PathLookup.empty(), PathLookup.empty()).getProperties(),
                allowEmptyProperties);
    }

    static ModelType getModelType(Type propertyType,
            PropertyFilter propertyFilter, String propertyName,
            Class<?> declaringClass,
            PathLookup<ModelEncoder<?, ?>> converterLookup,
            PathLookup<ClientUpdateMode> clientUpdateLookup) {
        if (propertyType instanceof Class<?>) {
            Class<?> propertyTypeClass = (Class<?>) propertyType;
            if (isBean(propertyTypeClass)) {
                return new BeanModelType<>(propertyTypeClass, propertyFilter,
                        converterLookup, clientUpdateLookup);
            } else {
                Optional<ModelType> maybeBasicModelType = BasicModelType
                        .get(propertyTypeClass);
                if (maybeBasicModelType.isPresent()) {
                    return maybeBasicModelType.get();
                }
            }
        } else if (ListModelType.isList(propertyType)) {
            return getListModelType(propertyType, propertyFilter, propertyName,
                    declaringClass, converterLookup, clientUpdateLookup);
        }

        throw new InvalidTemplateModelException(String.format(
                "Type '%s' is not supported."
                        + " Used in class '%s' with property named '%s'. %s. "
                        + "Use @%s annotation to convert the type to a supported type.",
                propertyType.toString(), declaringClass.getSimpleName(),
                propertyName, ModelType.getSupportedTypesString(),
                Encode.class.getSimpleName()));
    }

    static ModelType getConvertedModelType(Type propertyType,
            PropertyFilter propertyFilter, String propertyName,
            Class<?> declaringClass,
            PathLookup<ModelEncoder<?, ?>> converterLookup,
            PathLookup<ClientUpdateMode> clientUpdateLookup) {

        if (!(propertyType instanceof Class<?>)) {
            throw new UnsupportedOperationException(String.format(
                    "Using converters with parameterized types is not currently supported."
                            + "Used in class '%s' with property named '%s'",
                    declaringClass.getSimpleName(), propertyName));
        }

        Optional<ModelEncoder<?, ?>> converterOptional = converterLookup
                .getItem(propertyFilter.getPrefix());
        if (!converterOptional.isPresent()) {
            throw new IllegalStateException(
                    "The ModelConverterProvider passed to "
                            + "getConvertedModelType is unable to provide a converter "
                            + "for the given PropertyFilter.");
        }

        ModelEncoder<?, ?> converter = converterOptional.get();
        if (!isCompatible(converter.getDecodedType(), propertyType)) {
            throw new InvalidTemplateModelException(String.format(
                    "Converter '%s' is incompatible with the type '%s'.",
                    converter.getClass().getName(),
                    propertyType.getTypeName()));
        }

        if (isBean(converter.getEncodedType())) {
            return new ConvertedModelType<>(new BeanModelType<>(
                    converter.getEncodedType(), propertyFilter, converterLookup,
                    clientUpdateLookup), converter);
        } else {
            Optional<ModelType> maybeBasicModelType = BasicModelType
                    .get(converter.getEncodedType());
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

    private static boolean isCompatible(Class<?> clazz, Type type) {
        if (clazz.equals(type)) {
            return true;
        } else if (type instanceof Class<?>) {
            return ReflectTools.convertPrimitiveType(clazz)
                    .equals(ReflectTools.convertPrimitiveType((Class<?>) type));
        }
        return false;
    }

    private static ModelType getListModelType(Type propertyType,
            PropertyFilter propertyFilter, String propertyName,
            Class<?> declaringClass,
            PathLookup<ModelEncoder<?, ?>> converterLookup,
            PathLookup<ClientUpdateMode> clientUpdateLookup) {
        assert ListModelType.isList(propertyType);
        ParameterizedType pt = (ParameterizedType) propertyType;

        Type itemType = pt.getActualTypeArguments()[0];
        if (itemType instanceof ParameterizedType) {
            return new ListModelType<>((ComplexModelType<?>) getModelType(
                    itemType, propertyFilter, propertyName, declaringClass,
                    converterLookup, clientUpdateLookup));
        } else if (BasicComplexModelType.isBasicType(itemType)) {
            return new ListModelType<>(
                    BasicComplexModelType.get((Class<?>) itemType).get());
        } else if (isBean(itemType)) {
            Class<?> beansListItemType = (Class<?>) itemType;
            return new ListModelType<>(new BeanModelType<>(beansListItemType,
                    propertyFilter, converterLookup, clientUpdateLookup));
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
        if (BasicModelType.get(cls).isPresent()
                || isBoxedUnsupportedType(cls)) {
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
        return getExistingProperty(propertyName).getType();
    }

    /**
     * Gets the client update mode for a property.
     *
     * @param property
     *            the property descriptor for which to find the client update
     *            mode
     * @return the client update mode, or IF_TWO_WAY_BINDING if no mode has been
     *         explicitly configured
     *
     * @see AllowClientUpdates
     */
    protected ClientUpdateMode getClientUpdateMode(
            BeanModelTypeProperty property) {
        ClientUpdateMode clientUpdateMode = property.getClientUpdateMode();
        if (clientUpdateMode == null) {
            return ClientUpdateMode.IF_TWO_WAY_BINDING;
        } else {
            return clientUpdateMode;
        }
    }

    protected BeanModelTypeProperty getExistingProperty(String propertyName) {
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
                        "Expected type '%s' for property '%s' but imported type is '%s'",
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

        properties.forEach((name, property) -> json.put(name,
                property.getType().toJson()));

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
        properties.forEach((name, property) -> property.getType()
                .createInitialValue(node, name));
    }

    /**
     * Gets a map whose keys are all properties (including subproperties) that
     * allowed to be updated from the client-side and values indicate the
     * property getter presence.
     *
     * @see ElementPropertyMap#setUpdateFromClientFilter(SerializablePredicate)
     *
     * @param twoWayBindingPaths
     *            a set of path names for which two way bindings are defined in
     *            the template
     * @return a map of properties whose update is allowed from the client-side
     *         and indicator of their getters presence
     */
    public Map<String, Boolean> getClientUpdateAllowedProperties(
            Set<String> twoWayBindingPaths) {
        Map<String, Boolean> allowedProperties = new HashMap<>();

        // Recurse through all properties
        collectAllowedProperties("", allowedProperties,
                Collections.unmodifiableSet(twoWayBindingPaths));

        return allowedProperties;
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

    private void collectAllowedProperties(String prefix,
            Map<String, Boolean> allowedProperties,
            Set<String> twoWayBindingPaths) {
        properties.forEach((name, property) -> {
            String fullName = prefix + name;

            // Mark as allowed if appropriate
            BeanModelTypeProperty modelProperty = getExistingProperty(name);
            ClientUpdateMode clientUpdateMode = getClientUpdateMode(
                    modelProperty);
            if (clientUpdateMode == ClientUpdateMode.ALLOW
                    || (clientUpdateMode == ClientUpdateMode.IF_TWO_WAY_BINDING
                            && twoWayBindingPaths.contains(fullName))) {
                allowedProperties.put(fullName, modelProperty.hasGetter());
            }

            if (clientUpdateMode == ClientUpdateMode.DENY
                    && modelProperty.hasGetter()) {
                LoggerFactory.getLogger(BeanModelType.class).debug(
                        "There is a getter for the property '{}' whose update from the client-side to "
                                + "the server-side is explicitly forbidden via @'{}' annotation value '{}'.",
                        fullName, AllowClientUpdates.class.getSimpleName(),
                        ClientUpdateMode.DENY);
            } else if (clientUpdateMode == ClientUpdateMode.IF_TWO_WAY_BINDING
                    && !twoWayBindingPaths.contains(fullName)
                    && modelProperty.hasGetter()) {
                LoggerFactory.getLogger(BeanModelType.class).debug(
                        "There is a getter for the property '{}' whose update from the client-side to "
                                + "the server-side is forbidden because the property is not a"
                                + "two way binding property but it's required to be "
                                + "(implicitly if there is no '{}' annotation for this "
                                + "property or explicitly if it's value is '{}')",
                        fullName, AllowClientUpdates.class.getSimpleName(),
                        ClientUpdateMode.IF_TWO_WAY_BINDING);
            }

            // Recurse if it's a bean
            ModelType propertyType = unwrapTypes(property.getType());
            BeanModelType<?> beanType = null;
            if (propertyType instanceof BeanModelType<?>) {
                beanType = (BeanModelType<?>) propertyType;
            } else if (propertyType instanceof ListModelType<?>) {
                beanType = getListItemBeanModelType(
                        (ListModelType<?>) propertyType);
            }
            if (beanType != null) {
                ((BeanModelType<?>) beanType).collectAllowedProperties(
                        fullName + ".", allowedProperties, twoWayBindingPaths);
            }
        });
    }

    private BeanModelType<?> getListItemBeanModelType(ListModelType<?> type) {
        ComplexModelType<?> itemType = type.getItemType();
        if (itemType instanceof BeanModelType<?>) {
            return (BeanModelType<?>) itemType;
        } else if (itemType instanceof ListModelType<?>) {
            return getListItemBeanModelType((ListModelType<?>) itemType);
        }
        return null;
    }

    private static ModelType unwrapTypes(ModelType type) {
        if (type instanceof ConvertedModelType<?, ?>) {
            return unwrapTypes(
                    ((ConvertedModelType<?, ?>) type).getWrappedModelType());
        } else {
            return type;
        }
    }

    /**
     * Checks whether the {@code clazz} represents a boxed promitive type which
     * is unsupported by {@link BasicModelType}.
     *
     * @param clazz
     *            java type to check
     * @return {@coe true} if {@code clazz} is unsupported boxed primitive type
     */
    private static boolean isBoxedUnsupportedType(Class<?> clazz) {
        return UNSUPPORTED_BOXED_TYPES.contains(clazz);
    }
}
