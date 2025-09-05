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

import com.vaadin.flow.internal.StateNode;

/**
 * A model type that can convert values between a representation suitable for
 * users and a representation suitable for storage in a {@link StateNode}.
 * <p>
 * Model type instances are shared between all instances of a given
 * {@link TemplateModel} type and should therefore be immutable to prevent race
 * conditions. The root type for a model can be found using
 * {@link ModelDescriptor#get(Class)}.
 *
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
public interface ModelType extends Serializable {
    /**
     * Creates a representation of the provided model value that is intended for
     * use in application code. For mutable values, this is typically a proxy
     * that is directly connected to the underlying model value.
     *
     * @param modelValue
     *            the model value to convert
     * @return a user-friendly representation of the provided model value
     * @throws IllegalArgumentException
     *             if {@code modelValue} cannot be handled by the type
     */
    Object modelToApplication(Serializable modelValue)
            throws IllegalArgumentException;

    /**
     * Creates a model value representation of the provided application value.
     * <p>
     * For application values that contain properties (i.e. beans), the provided
     * filter is used to determine which properties from the bean should be
     * included in the model representation.
     *
     * @param applicationValue
     *            the user value to convert
     * @param filter
     *            the filter to use to determine which properties to include,
     *            not <code>null</code>
     * @return a model value representation of the provided user value.
     */
    Serializable applicationToModel(Object applicationValue,
            PropertyFilter filter);

    /**
     * Checks whether this type can accept application values of the given type.
     * The method only considers this actual type, not the types of sub
     * properties or list items.
     *
     * @param applicationType
     *            the application type to check, not <code>null</code>
     * @return <code>true</code> if the provided type is acceptable,
     *         <code>false</code> otherwise
     */
    boolean accepts(Type applicationType);

    /**
     * Gets the Java {@link Type} that this model encapsulates.
     *
     * @return the java type
     */
    Type getJavaType();

    /**
     * Gets a string explaining the supported property types in model.
     *
     * @return a string explaining supported property types
     */
    static String getSupportedTypesString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Supported types are: ");
        BasicModelType.TYPES.keySet()
                .forEach(type -> sb.append(type.getSimpleName()).append(", "));
        sb.append("Beans and Lists of Beans.");
        return sb.toString();
    }

    /**
     * Creates a JSON representation of this model type.
     *
     * @return a JSON representation of this model type, not <code>null</code>
     */
    JsonNode toJson();

    /**
     * Create initial value for the given {@code property} and set it for the
     * {@code node}.
     *
     * @param node
     *            the node where the initial value should be set the
     *            {@code property}
     * @param property
     *            the property in the {@code node} whose initial value needs to
     *            be created
     */
    void createInitialValue(StateNode node, String property);
}
