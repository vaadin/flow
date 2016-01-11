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

import com.vaadin.annotations.JS;
import com.vaadin.data.util.BeanUtil;

public class ValueType {
    private static final AtomicInteger nextId = new AtomicInteger(-1);

    private final int id;

    private ValueType(boolean generateId) {
        if (generateId) {
            id = nextId.incrementAndGet();
        } else {
            id = -1;
        }
    }

    private static class PrimitiveType extends ValueType {
        private final Object defaultValue;

        public PrimitiveType(Object defaultValue) {
            super(true);
            this.defaultValue = defaultValue;
        }

        @Override
        public Object getDefaultValue() {
            return defaultValue;
        }
    }

    public static class ObjectType extends ValueType {
        private final Map<Object, ValueType> propertyTypes;
        private final Map<String, ComputedProperty> computedProperties;

        // Cached hashCode
        private final int hashCode;

        public ObjectType(boolean generateId,
                Map<Object, ValueType> propertyTypes,
                Map<String, ComputedProperty> computedProperties) {
            super(generateId);
            this.propertyTypes = propertyTypes;
            this.computedProperties = computedProperties;

            hashCode = Objects.hash(propertyTypes, computedProperties);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        public Map<Object, ValueType> getPropertyTypes() {
            return propertyTypes;
        }

        public Map<String, ComputedProperty> getComputedProperties() {
            return computedProperties;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj.getClass() == ObjectType.class) {
                ObjectType that = (ObjectType) obj;
                return propertyTypes.equals(that.propertyTypes)
                        && computedProperties.equals(that.computedProperties);
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("ObjectType");

            if (!propertyTypes.isEmpty()) {
                sb.append(' ').append(propertyTypes);
            }

            if (!computedProperties.isEmpty()) {
                sb.append(' ').append(computedProperties);
            }

            return sb.toString();
        }
    }

    public static class ArrayType extends ObjectType {
        private final ValueType memberType;
        private final int hashCode;

        private ArrayType(boolean generateId,
                Map<Object, ValueType> propertyTypes, ValueType memberType) {
            // Not supporting computed properties for array types for now
            super(generateId, propertyTypes, Collections.emptyMap());
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

        @Override
        public String toString() {
            return "ArrayType {" + memberType + ", " + getPropertyTypes() + "}";
        }

        @Override
        public Object getDefaultValue() {
            return new ListNode(memberType);
        }
    }

    public int getId() {
        return id;
    }

    private static final ConcurrentHashMap<ArrayType, ArrayType> arrayTypes = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<ObjectType, ObjectType> objectTypes = new ConcurrentHashMap<>();

    // Singleton instances
    public static final ValueType STRING = new ValueType(true);

    public static final ValueType BOOLEAN = new ValueType(true);
    public static final ValueType BOOLEAN_PRIMITIVE = new PrimitiveType(
            Boolean.FALSE);

    public static final ValueType INTEGER = new ValueType(true);
    public static final ValueType INTEGER_PRIMITIVE = new PrimitiveType(
            Integer.valueOf(0));

    public static final ValueType NUMBER = new ValueType(true);
    public static final ValueType NUMBER_PRIMITIVE = new PrimitiveType(
            Double.valueOf(0));

    public static final ObjectType EMPTY_OBJECT = get(Collections.emptyMap());

    public static final ValueType UNDEFINED = new ValueType(true);
    public static final ArrayType UNDEFINED_ARRAY = get(Collections.emptyMap(),
            UNDEFINED);

    private static final Map<Type, ValueType> builtInTypes = new HashMap<>();

    static {
        builtInTypes.put(String.class, STRING);
        builtInTypes.put(boolean.class, BOOLEAN_PRIMITIVE);
        builtInTypes.put(Boolean.class, BOOLEAN);
        builtInTypes.put(int.class, INTEGER_PRIMITIVE);
        builtInTypes.put(Integer.class, INTEGER);
        builtInTypes.put(double.class, NUMBER_PRIMITIVE);
        builtInTypes.put(Double.class, NUMBER);
    }

    public static ObjectType get(Map<?, ValueType> propertyTypes) {
        return get(propertyTypes, Collections.emptyMap());
    }

    public static ObjectType get(Map<?, ValueType> propertyTypes,
            Map<String, ComputedProperty> computedProperties) {
        // Quickly created instance just for the map lookup
        @SuppressWarnings("unchecked")
        ObjectType lookupKey = new ObjectType(false,
                (Map<Object, ValueType>) propertyTypes, computedProperties);

        ObjectType value = objectTypes.get(lookupKey);
        if (value == null) {
            ObjectType referenceValue = new ObjectType(true,
                    Collections.unmodifiableMap(new HashMap<>(propertyTypes)),
                    Collections.unmodifiableMap(
                            new HashMap<>(computedProperties)));

            // Try to put it into the map
            objectTypes.putIfAbsent(referenceValue, referenceValue);

            // Use the value that actually ended up in the map
            value = objectTypes.get(referenceValue);
        }
        return value;
    }

    public static ArrayType get(Map<?, ValueType> propertyTypes,
            ValueType memberType) {

        // Quickly created instance just for the map lookup
        @SuppressWarnings("unchecked")
        ArrayType lookupKey = new ArrayType(false,
                (Map<Object, ValueType>) propertyTypes, memberType);

        ArrayType value = arrayTypes.get(lookupKey);
        if (value == null) {
            // Create proper instance with real id and defensive copy of the map
            ArrayType referenceValue = new ArrayType(true,
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
        ValueType valueType = builtInTypes.get(type);
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

    public static ObjectType getBeanType(Class<?> clazz) {
        try {

            Map<String, ValueType> properties = new HashMap<>();
            Map<String, ComputedProperty> computed = new HashMap<>();

            for (PropertyDescriptor pd : BeanUtil
                    .getBeanPropertyDescriptor(clazz)) {
                if (!pd.getName().equals("class")) {
                    String name = pd.getName();

                    properties.put(name, ValueType.get(getPropertyType(pd)));

                    ComputedProperty cp = findComputedProperty(clazz, pd);
                    if (cp != null) {
                        computed.put(name, cp);
                    }
                }
            }

            return get(properties, computed);
        } catch (IntrospectionException e) {
            throw new RuntimeException();
        }
    }

    private static Type getPropertyType(PropertyDescriptor pd) {
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

    private static ComputedProperty findComputedProperty(Class<?> beanType,
            PropertyDescriptor pd) {
        Method method = pd.getReadMethod();
        if (method == null) {
            return null;
        }

        String name = pd.getName();

        if (method.isDefault()) {
            if (method.getReturnType() == void.class) {
                throw new IllegalStateException("Computed property "
                        + method.toString() + " has no return type");
            } else if (method.getParameterCount() != 0) {
                throw new IllegalStateException(
                        "Computed property " + method.toString()
                                + " should require zero parameters");
            }

            return new DefaultMethodComputedProperty(name, beanType, method);
        }

        JS jsAnnotation = method.getAnnotation(JS.class);
        if (jsAnnotation != null) {
            String script = jsAnnotation.value();
            Class<?> type = method.getReturnType();

            return new JsComputedProperty(name, script, type);
        }

        return null;
    }

    public Object getDefaultValue() {
        return null;
    }

    @Override
    public String toString() {
        if (this == STRING) {
            return "String";
        } else if (this == BOOLEAN) {
            return "Boolean";
        } else if (this == BOOLEAN_PRIMITIVE) {
            return "boolean";
        } else if (this == INTEGER) {
            return "Integer";
        } else if (this == INTEGER_PRIMITIVE) {
            return "int";
        } else if (this == NUMBER) {
            return "Double";
        } else if (this == NUMBER_PRIMITIVE) {
            return "double";
        } else if (this == UNDEFINED) {
            return "undefined";
        } else {
            throw new RuntimeException("Unkown instance");
        }
    }
}
