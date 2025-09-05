/**
 * Copyright (C) 2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.templatemodel;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;

/**
 * Common abstract class with generic functionality for basic mode type.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @param <T>
 *            the (basic) Java type used by this model type
 * @author Vaadin Ltd
 * @since 1.0
 * @deprecated Template model and model types are not supported for lit
 *             template, but you can use {@code @Id} mapping and the component
 *             API or the element API with property synchronization instead.
 *             Polymer template support is deprecated - we recommend you to use
 *             {@code LitTemplate} instead. Read more details from <a href=
 *             "https://vaadin.com/blog/future-of-html-templates-in-vaadin">the
 *             Vaadin blog.</a>
 */
@Deprecated
public abstract class AbstractBasicModelType<T> implements ModelType {

    private final Class<T> type;

    protected AbstractBasicModelType(Class<T> type) {
        this.type = type;
    }

    @Override
    public boolean accepts(Type applicationType) {
        if (type.isPrimitive()
                && ReflectTools.convertPrimitiveType(type) == applicationType) {
            return true;
        }
        return type == applicationType;
    }

    @Override
    public Class<T> getJavaType() {
        return type;
    }

    @Override
    public JsonNode toJson() {
        return JacksonUtils.createNode(type.getSimpleName());
    }

    @Override
    public void createInitialValue(StateNode node, String property) {
        ElementPropertyMap feature = node.getFeature(ElementPropertyMap.class);
        if (!feature.hasProperty(property)) {
            feature.setProperty(property,
                    (Serializable) modelToApplication(null));
        }
    }

    /**
     * Converts the given model value to the application type of this model
     * type. The conversion automatically handles default values for primitive
     * types and conversion of client-originated numbers to the expected Java
     * number type.
     *
     * @param modelValue
     *            the model value to convert
     * @return the converted value, not <code>null</code> if the application
     *         type is a primitive
     */
    protected Object convertToApplication(Serializable modelValue) {
        if (modelValue == null && getJavaType().isPrimitive()) {
            return ReflectTools.getPrimitiveDefaultValue(getJavaType());
        }
        if (modelValue == null) {
            return null;
        }

        Class<?> convertedJavaType = ReflectTools
                .convertPrimitiveType(getJavaType());

        // Numeric value from the client is always Double
        if (modelValue instanceof Double
                && convertedJavaType == Integer.class) {
            modelValue = Integer.valueOf(((Double) modelValue).intValue());
        }

        if (convertedJavaType == modelValue.getClass()) {
            return modelValue;
        } else {
            throw new IllegalArgumentException(String.format(
                    "The stored model value '%s' type '%s' "
                            + "cannot be used as a type for a model property with type '%s'",
                    modelValue, modelValue.getClass().getName(),
                    getJavaType().getName()));
        }
    }

    protected static <M> Map<Class<?>, M> loadBasicTypes(
            Function<Class<?>, M> factory) {
        Map<Class<?>, M> map = Stream
                .of(int.class, Integer.class, boolean.class, Boolean.class,
                        double.class, Double.class, String.class)
                .collect(Collectors.toMap(Function.identity(), factory));

        // Make sure each type has a unique getSimpleName value since it's used
        // as an identifier in JSON messages
        assert map.keySet().stream().map(Class::getSimpleName).distinct()
                .count() == map.size();
        return Collections.unmodifiableMap(map);
    }
}
