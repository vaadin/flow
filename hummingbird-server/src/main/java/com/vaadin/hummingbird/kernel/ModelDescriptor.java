package com.vaadin.hummingbird.kernel;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.vaadin.data.util.BeanUtil;
import com.vaadin.ui.Template.Model;

public class ModelDescriptor {
    private final Map<String, PropertyDescriptor> descriptors;

    private Class<?> modelType;

    private ModelDescriptor(Class<?> modelType) {
        this.modelType = modelType;
        try {
            descriptors = Collections.unmodifiableMap(
                    BeanUtil.getBeanPropertyDescriptor(modelType).stream()
                            .filter(d -> d.getWriteMethod() != null)
                            .collect(Collectors.toMap(
                                    PropertyDescriptor::getName,
                                    Function.identity())));
        } catch (IntrospectionException e) {
            throw new RuntimeException(
                    "Introspection failed for " + modelType.getName(), e);
        }
    }

    public Class<?> getModelType() {
        return modelType;
    }

    public Set<String> getPropertyNames() {
        return descriptors.keySet();
    }

    private PropertyDescriptor getDescriptorOrThrow(String propertyName) {
        PropertyDescriptor descriptor = descriptors.get(propertyName);
        if (descriptor == null) {
            throw new IllegalArgumentException(
                    propertyName + " is not a property");
        }
        return descriptor;
    }

    public Type getPropertyType(String propertyName) {
        PropertyDescriptor descriptor = getDescriptorOrThrow(propertyName);

        Method readMethod = descriptor.getReadMethod();
        if (readMethod != null) {
            return readMethod.getGenericReturnType();
        }

        Method writeMethod = descriptor.getWriteMethod();
        if (writeMethod != null) {
            return writeMethod.getGenericParameterTypes()[0];
        }

        // Fall back to Class<?> without generic type information
        return descriptor.getPropertyType();
    }

    public boolean isReadonly(String propertyName) {
        return getDescriptorOrThrow(propertyName).getWriteMethod() != null;
    }

    public void setPropertyValue(String propertyName, Object modelInstance,
            Object value) {
        Method writeMethod = getDescriptorOrThrow(propertyName)
                .getWriteMethod();
        if (writeMethod == null) {
            throw new IllegalArgumentException(
                    propertyName + " is a readonly property");
        }
        try {
            writeMethod.invoke(modelInstance, value);
        } catch (Exception e) {
            throw new RuntimeException("Error setting " + propertyName, e);
        }
    }

    public Object getPropertyValue(String propertyName, Object modelInstance) {
        Method readMethod = getDescriptorOrThrow(propertyName).getReadMethod();

        try {
            return readMethod.invoke(modelInstance);
        } catch (Exception e) {
            throw new RuntimeException("Error getting " + propertyName, e);
        }
    }

    public ModelDescriptor getChildDescriptor(String propertyName) {
        Type type = getPropertyType(propertyName);
        if (type instanceof Class<?>) {
            Class<?> clazz = (Class<?>) type;
            if (Model.isSimpleType(clazz)) {
                return null;
            } else {
                return get(clazz);
            }
        } else {
            throw new RuntimeException("Not yet supported: " + type.getClass());
        }
    }

    public static ModelDescriptor get(Class<?> modelType) {
        // TODO cache (preferably without leaking classloaders)
        return new ModelDescriptor(modelType);
    }
}