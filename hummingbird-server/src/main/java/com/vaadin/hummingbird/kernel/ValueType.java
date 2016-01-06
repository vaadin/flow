package com.vaadin.hummingbird.kernel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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

    private static final AtomicInteger nextId = new AtomicInteger(4);

    private static final ConcurrentHashMap<Map<Object, ValueType>, ObjectType> objectTypes = new ConcurrentHashMap<>();

    public static ObjectType get(Map<Object, ValueType> propertyTypes) {
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

    public static ArrayType get(Map<Object, ValueType> propertyTypes,
            ValueType memberType) {

        // Quickly created instance just for the map lookup
        ArrayType lookupKey = new ArrayType(-1, propertyTypes, memberType);

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
}
