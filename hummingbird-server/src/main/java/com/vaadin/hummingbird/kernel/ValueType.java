package com.vaadin.hummingbird.kernel;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.vaadin.data.util.BeanUtil;

public class ValueType {
    private int id;

    private ValueType(int id) {
        this.id = id;
    }

    public static class ObjectType extends ValueType {
        private final Map<Object, ValueType> propertyTypes;

        // Cached hashCode
        private final int hashCode;

        public ObjectType(int id, Map<Object, ValueType> propertyTypes) {
            super(id);
            this.propertyTypes = propertyTypes;

            hashCode = propertyTypes.hashCode();
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        public Map<Object, ValueType> getPropertyTypes() {
            return propertyTypes;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj.getClass() == ObjectType.class) {
                return propertyTypes.equals(((ObjectType) obj).propertyTypes);
            } else {
                return false;
            }
        }
    }

    public static class ArrayType extends ObjectType {
        private final ValueType memberType;
        private final int hashCode;

        private ArrayType(int id, Map<Object, ValueType> propertyTypes,
                ValueType memberType) {
            super(id, propertyTypes);
            this.memberType = memberType;

            hashCode = Objects.hash(propertyTypes, memberType);
        }

        public ValueType getMemberType() {
            return memberType;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj.getClass() == ArrayType.class) {
                ArrayType that = (ArrayType) obj;
                return memberType.equals(that.memberType)
                        && getPropertyTypes().equals(that.getPropertyTypes());
            } else {
                return false;
            }
        }
    }

    public int getId() {
        return id;
    }

    // Singleton instances
    public static final ValueType STRING = new ValueType(0);
    public static final ValueType BOOLEAN = new ValueType(1);
    public static final ValueType INTEGER = new ValueType(2);
    public static final ValueType NUMBER = new ValueType(3);
    public static final ValueType UNDEFINED = new ValueType(4);

    private static final AtomicInteger nextId = new AtomicInteger(5);

    private static final Map<Type, ValueType> primitiveTypes = new HashMap<>();

    static {
        primitiveTypes.put(String.class, STRING);
        primitiveTypes.put(boolean.class, BOOLEAN);
        primitiveTypes.put(Boolean.class, BOOLEAN);
        primitiveTypes.put(int.class, INTEGER);
        primitiveTypes.put(Integer.class, INTEGER);
        primitiveTypes.put(double.class, NUMBER);
        primitiveTypes.put(Double.class, NUMBER);
    }

    private static final ConcurrentHashMap<Map<Object, ValueType>, ObjectType> objectTypes = new ConcurrentHashMap<>();

    public static ObjectType get(Map<?, ValueType> propertyTypes) {
        ObjectType value = objectTypes.get(propertyTypes);
        if (value == null) {
            HashMap<Object, ValueType> defensiveCopy = new HashMap<>(
                    propertyTypes);

            value = objectTypes.computeIfAbsent(defensiveCopy,
                    k -> new ObjectType(nextId.incrementAndGet(),
                            Collections.unmodifiableMap(defensiveCopy)));
        }
        return value;
    }

    private static final ConcurrentHashMap<ArrayType, ArrayType> arrayTypes = new ConcurrentHashMap<>();

    public static ArrayType get(Map<?, ValueType> propertyTypes,
            ValueType memberType) {

        // Quickly created instance just for the map lookup
        @SuppressWarnings("unchecked")
        ArrayType lookupKey = new ArrayType(-1,
                (Map<Object, ValueType>) propertyTypes, memberType);

        ArrayType value = arrayTypes.get(lookupKey);
        if (value == null) {
            // Create proper instance with real id and defensive copy of the map
            ArrayType referenceValue = new ArrayType(nextId.incrementAndGet(),
                    Collections.unmodifiableMap(new HashMap<>(propertyTypes)),
                    memberType);

            // Try to put it into the map
            arrayTypes.putIfAbsent(referenceValue, referenceValue);

            // Use the value that actually ended up in the map
            value = arrayTypes.get(referenceValue);
        }
        return value;
    }

    public static ValueType get(Type type) {
        ValueType valueType = primitiveTypes.get(type);
        if (valueType != null) {
            return valueType;
        }

        if (type instanceof Class<?>) {
            Class<?> clazz = (Class<?>) type;
            if (clazz == List.class) {
                // Raw list
                return get(Collections.emptyMap(), UNDEFINED);
            }
            return getBeanType(clazz);
        }

        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            if (pt.getRawType() == List.class) {
                Type memberType = pt.getActualTypeArguments()[0];
                return get(Collections.emptyMap(), get(memberType));
            }
        }

        throw new RuntimeException("No ValueType found for " + type);
    }

    private static ValueType getBeanType(Class<?> clazz) {
        try {
            Map<String, ValueType> properties = BeanUtil
                    .getBeanPropertyDescriptor(clazz).stream()
                    .filter(pd -> !pd.getName().equals("class"))
                    .collect(Collectors.toMap(PropertyDescriptor::getName,
                            pd -> ValueType.get(getPropertyType(pd))));
            return get(properties);
        } catch (IntrospectionException e) {
            throw new RuntimeException();
        }
    }

    public static Type getPropertyType(PropertyDescriptor pd) {
        Method readMethod = pd.getReadMethod();
        if (readMethod != null) {
            return readMethod.getGenericReturnType();
        }

        Method writeMethod = pd.getWriteMethod();
        if (writeMethod != null) {
            return writeMethod.getGenericParameterTypes()[0];
        }

        // Fall back to non-generic type
        return pd.getPropertyType();
    }
}
