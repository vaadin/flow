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

import com.fasterxml.jackson.databind.JsonNode;

import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.internal.StateNode;

/**
 * A {@link ModelType} implementation that wraps a model type for performing
 * type conversions on together with a {@link ModelEncoder}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @param <A>
 *            application type of the converter used by this class
 * @param <M>
 *            model type of the converter used by this class
 *
 * @deprecated Template model and model types are not supported for lit
 *             template, but you can use {@code @Id} mapping and the component
 *             API or the element API with property synchronization instead.
 *             Polymer template support is deprecated - we recommend you to use
 *             {@code LitTemplate} instead. Read more details from <a href=
 *             "https://vaadin.com/blog/future-of-html-templates-in-vaadin">the
 *             Vaadin blog.</a>
 */
@Deprecated
public class ConvertedModelType<A, M extends Serializable>
        implements ModelType {

    private final ModelType wrappedModelType;
    private final ModelEncoder<A, M> converter;

    /**
     * Creates a new ConvertedModelType from the given model type and converter.
     *
     * @param modelType
     *            the model type to wrap
     * @param converter
     *            the converter to use
     */
    ConvertedModelType(ModelType modelType, ModelEncoder<A, M> converter) {
        wrappedModelType = modelType;
        this.converter = converter;
    }

    @Override
    public Object modelToApplication(Serializable modelValue) {
        @SuppressWarnings("unchecked")
        M wrappedApplicationValue = (M) wrappedModelType
                .modelToApplication(modelValue);
        return converter.decode(wrappedApplicationValue);
    }

    @Override
    public Serializable applicationToModel(Object applicationValue,
            PropertyFilter filter) {
        @SuppressWarnings("unchecked")
        M convertedValue = converter.encode((A) applicationValue);
        return wrappedModelType.applicationToModel(convertedValue, filter);
    }

    @Override
    public boolean accepts(Type applicationType) {
        return ReflectTools.convertPrimitiveType(converter.getDecodedType())
                .isAssignableFrom(ReflectTools
                        .convertPrimitiveType((Class<?>) applicationType));
    }

    @Override
    public Type getJavaType() {
        return converter.getDecodedType();
    }

    @Override
    public JsonNode toJson() {
        return wrappedModelType.toJson();
    }

    @Override
    public void createInitialValue(StateNode node, String property) {
        wrappedModelType.createInitialValue(node, property);
    }

    /**
     * Gets the model type describing the data actually stored in the model.
     *
     * @return the wrapped model type
     */
    public ModelType getWrappedModelType() {
        return wrappedModelType;
    }
}
