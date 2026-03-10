/**
 * Copyright (C) 2022-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.templatemodel;

import java.io.Serializable;
import java.lang.reflect.Type;

import tools.jackson.databind.JsonNode;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StateNode;

/**
 * A model type representing an unsupported type that defers validation to
 * property access time rather than failing eagerly during template
 * construction.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @deprecated Template model and model types are not supported for lit
 *             template. Polymer template support is deprecated - we recommend
 *             you to use {@code LitTemplate} instead.
 */
@Deprecated
class UnsupportedModelType implements ModelType {

    private final Type type;
    private final String errorMessage;

    UnsupportedModelType(Type type, String errorMessage) {
        this.type = type;
        this.errorMessage = errorMessage;
    }

    @Override
    public Object modelToApplication(Serializable modelValue) {
        throw new InvalidTemplateModelException(errorMessage);
    }

    @Override
    public Serializable applicationToModel(Object applicationValue,
            PropertyFilter filter) {
        throw new InvalidTemplateModelException(errorMessage);
    }

    @Override
    public boolean accepts(Type applicationType) {
        return false;
    }

    @Override
    public Type getJavaType() {
        return type;
    }

    @Override
    public JsonNode toJson() {
        return JacksonUtils.createNode(type.getTypeName());
    }

    @Override
    public void createInitialValue(StateNode node, String property) {
        // No initial value for unsupported types
    }
}
