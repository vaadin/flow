package com.vaadin.hummingbird.kernel;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType.Builder.FieldValueTarget;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.utility.ByteBuddyCommons;

public abstract class ClassBackedStateNode extends AbstractStateNode {
    private Map<Object, Object> values;

    protected abstract Map<Object, Field> getFieldMap();

    protected Field getField(Object key) {
        return getFieldMap().get(key);
    }

    private Map<Object, Object> getMap(boolean create) {
        if (values == null && create) {
            values = new HashMap<>();
        }
        return values;
    }

    @Override
    protected Object doGet(Object key) {
        Field field = getField(key);
        if (field != null) {
            try {
                return field.get(this);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } else {
            Map<Object, Object> map = getMap(false);
            if (map != null) {
                return map.get(key);
            } else {
                return null;
            }
        }
    }

    @Override
    protected boolean doesContainKey(Object key) {
        Field field = getField(key);
        if (field != null) {
            return true;
        } else {
            Map<Object, Object> map = getMap(false);
            if (map != null) {
                return map.containsKey(key);
            } else {
                return false;
            }
        }
    }

    @Override
    protected Object removeValue(Object key) {
        Field field = getField(key);
        if (field != null) {
            throw new IllegalArgumentException(
                    "Can't remove explicitly defined value");
        } else {
            Map<Object, Object> map = getMap(false);
            if (map != null) {
                Object oldValue = map.remove(key);
                if (map.isEmpty()) {
                    map = null;
                }
                return oldValue;
            } else {
                return null;
            }
        }
    }

    @Override
    protected Object setValue(Object key, Object value) {
        Field field = getField(key);
        if (field != null) {
            try {
                Object oldValue = field.get(this);
                field.set(this, value);
                return oldValue;
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } else {
            return getMap(true).put(key, value);
        }
    }

    @Override
    protected Stream<Object> doGetKeys() {
        Stream<Object> objectKeys = getFieldMap().keySet().stream();
        Map<Object, Object> map = getMap(false);
        if (map != null) {
            return Stream.concat(objectKeys, map.keySet().stream());
        } else {
            return objectKeys;
        }
    }

    @Override
    public Class<?> getType(Object key) {
        Field field = getField(key);
        if (field != null) {
            return field.getType();
        } else {
            return Object.class;
        }
    }

    private static final ConcurrentMap<Map<Object, Class<?>>, Class<? extends ClassBackedStateNode>> typeCache = new ConcurrentHashMap<>();

    public static ClassBackedStateNode create(
            Map<Object, Class<?>> explicitTypes) {
        Class<? extends ClassBackedStateNode> typeDescriptor = typeCache
                .computeIfAbsent(explicitTypes,
                        ClassBackedStateNode::createMapping);
        try {
            return typeDescriptor.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Class<? extends ClassBackedStateNode> createMapping(
            Map<Object, Class<?>> explicitTypes) {
        FieldValueTarget<ClassBackedStateNode> buddy = new ByteBuddy()
                .subclass(ClassBackedStateNode.class)
                .defineMethod("getFieldMap", Map.class, Collections.emptyList(),
                        Visibility.PUBLIC)
                .intercept(FieldAccessor.ofField("fieldMap"))
                .defineField("fieldMap", Map.class, Visibility.PUBLIC,
                        Ownership.STATIC);

        HashMap<Object, String> fieldNames = new HashMap<>();
        Map<String, Integer> usedNames = new HashMap<>();

        for (Entry<Object, Class<?>> entry : explicitTypes.entrySet()) {
            String name = getFieldName(entry.getKey().toString(), usedNames);

            fieldNames.put(entry.getKey(), name);
            buddy = buddy.defineField(name, entry.getValue(),
                    Visibility.PUBLIC);
        }

        Class<? extends ClassBackedStateNode> type = buddy.make()
                .load(ClassBackedStateNode.class.getClassLoader(),
                        ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();

        try {
            HashMap<Object, Field> fields = new HashMap<>();
            for (Entry<Object, Class<?>> entry : explicitTypes.entrySet()) {
                fields.put(entry.getKey(),
                        type.getDeclaredField(fieldNames.get(entry.getKey())));
            }

            type.getDeclaredField("fieldMap").set(null, fields);

            return type;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String getFieldName(String baseName,
            Map<String, Integer> usedNames) {
        if (baseName.indexOf('@') >= 0) {
            // Probably a package.ClassName@identityHash, extract ClassName
            baseName = baseName.replaceAll("(.*)\\.([^.]+)@.*", "$2");
        }

        // Make lower case and remove anything that can't be in a java
        // identifier
        baseName = baseName.chars().filter(Character::isJavaIdentifierPart)
                .map(Character::toLowerCase).collect(StringBuilder::new,
                        StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        if (baseName.length() == 0
                || !Character.isJavaIdentifierStart(baseName.charAt(0))) {
            baseName = "f" + baseName;
        }

        // Catch some reserved keywords
        try {
            ByteBuddyCommons.isValidIdentifier(baseName);
        } catch (IllegalArgumentException e) {
            usedNames.putIfAbsent(baseName, Integer.valueOf(0));
        }

        String name;
        Integer useCount = usedNames.get(baseName);
        if (useCount == null) {
            name = baseName;
            usedNames.put(baseName, Integer.valueOf(1));
        } else {
            name = baseName + useCount;
            usedNames.put(baseName, Integer.valueOf(useCount.intValue() + 1));
        }
        return name;
    }
}
