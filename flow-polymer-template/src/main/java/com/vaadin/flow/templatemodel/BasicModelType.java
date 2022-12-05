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
import java.util.Map;
import java.util.Optional;

/**
 * A model type representing an immutable leaf value, e.g. strings, numbers or
 * booleans.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
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
public class BasicModelType extends AbstractBasicModelType {

    static final Map<Class<?>, BasicModelType> TYPES = loadBasicTypes(
            BasicModelType::new);

    private BasicModelType(Class<?> type) {
        super(type);
    }

    /**
     * Gets the basic model type definition for the given Java class.
     *
     * @param type
     *            the Java class to find a basic model type for
     * @return the basic model type, or an empty optional if the provided type
     *         is not a basic type
     */
    public static Optional<ModelType> get(Class<?> type) {
        return Optional.ofNullable(TYPES.get(type));
    }

    @Override
    public Object modelToApplication(Serializable modelValue) {
        return convertToApplication(modelValue);
    }

    @Override
    public Serializable applicationToModel(Object applicationValue,
            PropertyFilter filter) {
        return (Serializable) applicationValue;
    }

}
