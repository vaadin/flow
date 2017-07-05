package com.vaadin.flow.template.model;

import java.io.Serializable;
import java.lang.reflect.Type;

import elemental.json.JsonValue;

/**
 * A {@link ModelType} implementation that wraps a model type for performing
 * type conversions on together with a {@link ModelConverter}.
 * 
 * @author Vaadin Ltd
 *
 * @param <A>
 *            application type of the converter used by this class
 * @param <M>
 *            model type of the converter used by this class
 */
public class ConvertedModelType<A, M extends Serializable>
        implements ModelType {

    private final ModelType wrappedModelType;
    private final ModelConverter<A, M> converter;

    /**
     * Creates a new ConvertedModelType from the given model type and converter.
     * 
     * @param modelType
     *            the model type to wrap
     * @param converter
     *            the converter to use
     */
    ConvertedModelType(ModelType modelType,
            ModelConverter<A, M> converter) {
        wrappedModelType = modelType;
        this.converter = converter;
    }

    @Override
    public Object modelToApplication(Serializable modelValue) {
        @SuppressWarnings("unchecked")
        M wrappedApplicationValue = (M) wrappedModelType
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
        M convertedValue = converter.toModel((A) applicationValue);
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
