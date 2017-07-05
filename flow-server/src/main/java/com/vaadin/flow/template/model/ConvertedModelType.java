package com.vaadin.flow.template.model;

import java.io.Serializable;
import java.lang.reflect.Type;

import elemental.json.JsonValue;

/**
 * 
 * @author Vaadin Ltd
 *
 * @param <T>
 * @param <C>
 */
public class ConvertedModelType<T, C extends Serializable>
        implements ModelType {

    private final ModelType wrappedModelType;
    private final ModelConverter<T, C> converter;

    /**
     * 
     * @param modelType
     * @param converter
     */
    ConvertedModelType(ModelType modelType,
            ModelConverter<T, C> converter) {
        wrappedModelType = modelType;
        this.converter = converter;
    }

    @Override
    public Object modelToApplication(Serializable modelValue) {
        @SuppressWarnings("unchecked")
        C wrappedApplicationValue = (C) wrappedModelType
                .modelToApplication(modelValue);
        return converter.toApplication(wrappedApplicationValue);
    }

    @Override
    public Object modelToNashorn(Serializable modelValue) {
        throw new UnsupportedOperationException("Obsolete functionality");
    }


    @Override
    public Serializable applicationToModel(Object applicationValue,
            PropertyFilter filter) {
        @SuppressWarnings("unchecked")
        C convertedValue = converter.toModel((T) applicationValue);
        return wrappedModelType.applicationToModel(convertedValue,
                filter);
    }

    @Override
    public boolean accepts(Type applicationType) {
        return converter.getApplicationType()
                .isAssignableFrom((Class<?>) applicationType);
    }

    @Override
    public Type getJavaType() {
        return converter.getApplicationType();
    }

    @Override
    public JsonValue toJson() {
        return wrappedModelType.toJson();
    }
}
