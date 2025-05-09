/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.data.binder;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.internal.BeanUtil;

/**
 * A {@link PropertySet} that uses reflection to find bean properties.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 *
 * @param <T>
 *            the type of the bean
 */
public class BeanPropertySet<T> implements PropertySet<T> {

    /**
     * Serialized form of a property set. When deserialized, the property set
     * for the corresponding bean type is requested, which either returns the
     * existing cached instance or creates a new one.
     *
     * @see #readResolve()
     * @see BeanPropertyDefinition#writeReplace()
     */
    private static class SerializedPropertySet<T> implements Serializable {
        private final InstanceKey<T> instanceKey;

        private SerializedPropertySet(InstanceKey<T> instanceKey) {
            this.instanceKey = instanceKey;
        }

        private Object readResolve() {
            /*
             * When this instance is deserialized, it will be replaced with a
             * property set for the corresponding bean type and property name.
             */
            return get(instanceKey.type, instanceKey.checkNestedDefinitions,
                    new PropertyFilterDefinition(instanceKey.depth,
                            instanceKey.ignorePackageNames));
        }
    }

    /**
     * Serialized form of a property definition. When deserialized, the property
     * set for the corresponding bean type is requested, which either returns
     * the existing cached instance or creates a new one. The right property
     * definition is then fetched from the property set.
     *
     * @see #readResolve()
     * @see BeanPropertySet#writeReplace()
     */
    private static class SerializedPropertyDefinition implements Serializable {
        private final Class<?> beanType;
        private final String propertyName;

        private SerializedPropertyDefinition(Class<?> beanType,
                String propertyName) {
            this.beanType = beanType;
            this.propertyName = propertyName;
        }

        private Object readResolve() throws IOException {
            /*
             * When this instance is deserialized, it will be replaced with a
             * property definition for the corresponding bean type and property
             * name.
             */
            return get(beanType).getProperty(propertyName)
                    .orElseThrow(() -> new IOException(
                            beanType + " no longer has a property named "
                                    + propertyName));
        }
    }

    private static class BeanPropertyDefinition<T, V>
            extends AbstractBeanPropertyDefinition<T, V> {

        public BeanPropertyDefinition(BeanPropertySet<T> propertySet,
                Class<T> propertyHolderType, PropertyDescriptor descriptor) {
            super(propertySet, propertyHolderType, descriptor);
        }

        @Override
        public ValueProvider<T, V> getGetter() {
            return bean -> {
                Method readMethod = getDescriptor().getReadMethod();
                Object value = invokeWrapExceptions(readMethod, bean);
                return getType().cast(value);
            };
        }

        @Override
        public Optional<Setter<T, V>> getSetter() {
            if (getDescriptor().getWriteMethod() == null) {
                return Optional.empty();
            }

            Setter<T, V> setter = (bean, value) -> {
                // Do not "optimize" this getter call,
                // if its done outside the code block, that will produce
                // NotSerializableException because of some lambda compilation
                // magic
                Method innerSetter = getDescriptor().getWriteMethod();
                invokeWrapExceptions(innerSetter, bean, value);
            };
            return Optional.of(setter);
        }

        private Object writeReplace() {
            /*
             * Instead of serializing this actual property definition, only
             * serialize a DTO that when deserialized will get the corresponding
             * property definition from the cache.
             */
            return new SerializedPropertyDefinition(
                    getPropertySet().instanceKey.type, getName());
        }

        @Override
        public PropertyDefinition<T, ?> getParent() {
            return null;
        }
    }

    /**
     * Contains properties for a bean type which is nested in another
     * definition.
     *
     * @param <T>
     *            the bean type
     * @param <V>
     *            the value type returned by the getter and set by the setter
     */
    public static class NestedBeanPropertyDefinition<T, V>
            extends AbstractBeanPropertyDefinition<T, V> {

        /**
         * Default maximum depth for scanning nested properties.
         *
         */
        protected static final int MAX_PROPERTY_NESTING_DEPTH = 10;

        private final PropertyDefinition<T, ?> parent;

        /**
         * Creates a new instance of a nested property definition.
         *
         * @param propertySet
         *            the {@link PropertySet} that this property belongs to
         * @param parent
         *            the parent property definition
         * @param descriptor
         *            the property descriptor
         */
        public NestedBeanPropertyDefinition(BeanPropertySet<T> propertySet,
                PropertyDefinition<T, ?> parent,
                PropertyDescriptor descriptor) {
            super(propertySet, parent.getType(), descriptor);
            this.parent = parent;
        }

        @Override
        public ValueProvider<T, V> getGetter() {
            return bean -> {
                Method readMethod = getDescriptor().getReadMethod();
                Object value = invokeWrapExceptions(readMethod,
                        parent.getGetter().apply(bean));
                return getType().cast(value);
            };
        }

        @Override
        public Optional<Setter<T, V>> getSetter() {
            if (getDescriptor().getWriteMethod() == null) {
                return Optional.empty();
            }

            Setter<T, V> setter = (bean, value) -> {
                // Do not "optimize" this getter call,
                // if its done outside the code block, that will produce
                // NotSerializableException because of some lambda compilation
                // magic
                Method innerSetter = getDescriptor().getWriteMethod();
                invokeWrapExceptions(innerSetter,
                        parent.getGetter().apply(bean), value);
            };
            return Optional.of(setter);
        }

        @Override
        public String getName() {
            return parent.getName() + "." + super.getName();
        }

        @Override
        public String getTopLevelName() {
            return super.getName();
        }

        private Object writeReplace() {
            /*
             * Instead of serializing this actual property definition, only
             * serialize a DTO that when deserialized will get the corresponding
             * property definition from the cache.
             */
            return new SerializedPropertyDefinition(
                    getPropertySet().instanceKey.type, getName());
        }

        @Override
        public PropertyDefinition<T, ?> getParent() {
            return parent;
        }
    }

    /**
     * Key for identifying cached BeanPropertySet instances.
     *
     */
    private static class InstanceKey<T> implements Serializable {
        private Class<T> type;
        private boolean checkNestedDefinitions;
        private int depth;
        private List<String> ignorePackageNames;
        private final boolean isRecord;

        public InstanceKey(Class<T> type, boolean checkNestedDefinitions,
                int depth, List<String> ignorePackageNames, boolean isRecord) {
            this.type = type;
            this.checkNestedDefinitions = checkNestedDefinitions;
            this.depth = depth;
            this.ignorePackageNames = ignorePackageNames;
            this.isRecord = isRecord;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (checkNestedDefinitions ? 1231 : 1237);
            result = prime * result + depth;
            result = prime * result + (isRecord ? 1249 : 1259);
            result = prime * result + ((ignorePackageNames == null) ? 0
                    : ignorePackageNames.hashCode());
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            InstanceKey other = (InstanceKey) obj;
            if (checkNestedDefinitions != other.checkNestedDefinitions) {
                return false;
            }
            if (depth != other.depth) {
                return false;
            }
            if (ignorePackageNames == null) {
                if (other.ignorePackageNames != null) {
                    return false;
                }
            } else if (!ignorePackageNames.equals(other.ignorePackageNames)) {
                return false;
            }
            if (isRecord != other.isRecord) {
                return false;
            }
            return Objects.equals(type, other.type);
        }

    }

    private static final ConcurrentMap<InstanceKey<?>, BeanPropertySet<?>> INSTANCES = new ConcurrentHashMap<>();

    private final InstanceKey<T> instanceKey;

    private final Map<String, PropertyDefinition<T, ?>> definitions;

    private BeanPropertySet(InstanceKey<T> instanceKey) {
        this.instanceKey = instanceKey;

        try {
            definitions = BeanUtil.getBeanPropertyDescriptors(instanceKey.type)
                    .stream().filter(BeanPropertySet::hasNonObjectReadMethod)
                    .map(descriptor -> new BeanPropertyDefinition<>(this,
                            instanceKey.type, descriptor))
                    .collect(Collectors.toMap(PropertyDefinition::getName,
                            Function.identity(), this::mergePropertyDefinitions,
                            LinkedHashMap::new));
        } catch (IntrospectionException e) {
            throw new IllegalArgumentException(
                    "Cannot find property descriptors for "
                            + instanceKey.type.getName(),
                    e);
        }
    }

    private PropertyDefinition<T, ?> mergePropertyDefinitions(
            PropertyDefinition<T, ?> def1, PropertyDefinition<T, ?> def2) {
        if (!def1.getType().equals(def2.getType())
                && !(def1.isGenericType() || def2.isGenericType())) {
            throw new IllegalStateException(String.format(
                    "Two property definition for property %s are discovered with different types: %s and %s",
                    def1.getName(), def1.getType(), def2.getType()));
        }
        if (!def1.getPropertyHolderType()
                .equals(def2.getPropertyHolderType())) {
            throw new IllegalStateException(String.format(
                    "Two property definition for property %s are discovered with different holder types: %s and %s",
                    def1.getName(), def1.getPropertyHolderType(),
                    def2.getPropertyHolderType()));
        }
        return def1.isGenericType() ? def2 : def1;
    }

    private BeanPropertySet(InstanceKey<T> instanceKey,
            Map<String, PropertyDefinition<T, ?>> definitions) {
        this.instanceKey = instanceKey;
        this.definitions = new HashMap<>(definitions);
    }

    private BeanPropertySet(InstanceKey<T> instanceKey,
            boolean checkNestedDefinitions,
            PropertyFilterDefinition propertyFilterDefinition) {
        this(instanceKey);
        if (checkNestedDefinitions) {
            Objects.requireNonNull(propertyFilterDefinition,
                    "You must define a property filter callback if using nested property scan.");
            findNestedDefinitions(definitions, 0, propertyFilterDefinition);
        }
    }

    private void findNestedDefinitions(
            Map<String, PropertyDefinition<T, ?>> parentDefinitions, int depth,
            PropertyFilterDefinition filterCallback) {
        if (depth >= filterCallback.getMaxNestingDepth()) {
            return;
        }
        if (parentDefinitions == null) {
            return;
        }
        Map<String, PropertyDefinition<T, ?>> moreProps = new HashMap<>();
        for (String parentPropertyKey : parentDefinitions.keySet()) {
            PropertyDefinition<T, ?> parentProperty = parentDefinitions
                    .get(parentPropertyKey);
            Class<?> type = parentProperty.getType();
            if (type.getPackage() == null || type.isEnum()) {
                continue;
            }
            String packageName = type.getPackage().getName();
            if (filterCallback.getIgnorePackageNamesStartingWith().stream()
                    .anyMatch(prefix -> packageName.startsWith(prefix))) {
                continue;
            }

            try {
                List<PropertyDescriptor> descriptors = BeanUtil
                        .getBeanPropertyDescriptors(type).stream()
                        .filter(BeanPropertySet::hasNonObjectReadMethod)
                        .collect(Collectors.toList());
                for (PropertyDescriptor descriptor : descriptors) {
                    String name = parentPropertyKey + "."
                            + descriptor.getName();
                    PropertyDescriptor subDescriptor = BeanUtil
                            .getPropertyDescriptor(instanceKey.type, name);
                    moreProps.put(name, new NestedBeanPropertyDefinition<>(this,
                            parentProperty, subDescriptor));

                }
            } catch (IntrospectionException e) {
                throw new IllegalArgumentException(
                        "Error finding nested property descriptors for "
                                + type.getName(),
                        e);
            }
        }
        if (moreProps.size() > 0) {
            definitions.putAll(moreProps);
            findNestedDefinitions(moreProps, ++depth, filterCallback);
        }

    }

    /**
     * Gets a {@link BeanPropertySet} for the given bean type.
     *
     * @param beanType
     *            the bean type to get a property set for, not <code>null</code>
     * @return the bean property set, not <code>null</code>
     * @param <T>
     *            the type of the bean
     */
    @SuppressWarnings("unchecked")
    public static <T> PropertySet<T> get(Class<? extends T> beanType) {
        Objects.requireNonNull(beanType, "Bean type cannot be null");
        InstanceKey key = new InstanceKey(beanType, false, 0, null,
                beanType.isRecord());
        // Cache the reflection results
        return (PropertySet<T>) INSTANCES
                .computeIfAbsent(key, ignored -> new BeanPropertySet<>(key))
                .copy();
    }

    private BeanPropertySet<T> copy() {
        return new BeanPropertySet<>(instanceKey, definitions);
    }

    /**
     * Gets a {@link BeanPropertySet} for the given bean type.
     *
     * @param beanType
     *            the bean type to get a property set for, not <code>null</code>
     * @param checkNestedDefinitions
     *            whether to scan for nested definitions in beanType
     * @param filterDefinition
     *            filtering conditions for nested properties
     * @param <T>
     *            the bean type to get a property set for
     * @return the bean property set, not <code>null</code>
     */
    @SuppressWarnings("unchecked")
    public static <T> PropertySet<T> get(Class<? extends T> beanType,
            boolean checkNestedDefinitions,
            PropertyFilterDefinition filterDefinition) {
        Objects.requireNonNull(beanType, "Bean type cannot be null");
        InstanceKey key = new InstanceKey(beanType, false,
                filterDefinition.getMaxNestingDepth(),
                filterDefinition.getIgnorePackageNamesStartingWith(),
                beanType.isRecord());
        return (PropertySet<T>) INSTANCES
                .computeIfAbsent(key, k -> new BeanPropertySet<>(key,
                        checkNestedDefinitions, filterDefinition))
                .copy();
    }

    @Override
    public Stream<PropertyDefinition<T, ?>> getProperties() {
        if (instanceKey.isRecord) {
            return definitions.values().stream();
        } else {
            return definitions.values().stream()
                    .sorted(Comparator.comparing(PropertyDefinition::getName));
        }
    }

    @Override
    public Optional<PropertyDefinition<T, ?>> getProperty(String name)
            throws IllegalArgumentException {
        Optional<PropertyDefinition<T, ?>> definition = Optional
                .ofNullable(definitions.get(name));
        if (!definition.isPresent() && name.contains(".")) {
            try {
                String parentName = name.substring(0, name.lastIndexOf('.'));
                Optional<PropertyDefinition<T, ?>> parent = getProperty(
                        parentName);
                if (!parent.isPresent()) {
                    throw new IllegalArgumentException(
                            "Cannot find property descriptor [" + parentName
                                    + "] for " + instanceKey.type.getName());
                }

                Optional<PropertyDescriptor> descriptor = Optional.ofNullable(
                        BeanUtil.getPropertyDescriptor(instanceKey.type, name));
                if (descriptor.isPresent()) {
                    NestedBeanPropertyDefinition<T, ?> nestedDefinition = new NestedBeanPropertyDefinition<>(
                            this, parent.get(), descriptor.get());
                    definitions.put(name, nestedDefinition);
                    return Optional.of(nestedDefinition);
                } else {
                    throw new IllegalArgumentException(
                            "Cannot find property descriptor [" + name
                                    + "] for " + instanceKey.type.getName());
                }

            } catch (IntrospectionException e) {
                throw new IllegalArgumentException(
                        "Cannot find property descriptors for "
                                + instanceKey.type.getName(),
                        e);
            }
        }
        return definition;
    }

    /**
     * Gets the bean type of this bean property set.
     *
     * @return the bean type of this bean property set
     */
    public Class<T> getBeanType() {
        return instanceKey.type;
    }

    private static boolean hasNonObjectReadMethod(
            PropertyDescriptor descriptor) {
        Method readMethod = descriptor.getReadMethod();
        return readMethod != null
                && readMethod.getDeclaringClass() != Object.class;
    }

    private static Object invokeWrapExceptions(Method method, Object target,
            Object... parameters) {
        try {
            return method.invoke(target, parameters);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "Property set for bean " + instanceKey.type.getName();
    }

    private Object writeReplace() {
        /*
         * Instead of serializing this actual property set, only serialize a DTO
         * that when deserialized will get the corresponding property set from
         * the cache.
         */
        return new SerializedPropertySet(instanceKey);
    }
}
