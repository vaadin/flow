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
import java.util.Optional;

import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.BasicTypeValue;

/**
 * A model type representing an immutable leaf value (e.g. strings, numbers or
 * booleans) to use them in a list.
 * <p>
 * There is a similar class {@link BasicModelType} which do the same but it
 * keeps handles the values as is. This class wraps them into {@link StateNode}
 * to be able to use them in side lists.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @see BasicModelType
 *
 * @param <T>
 *            the (basic) Java type used by this model type
 * @deprecated Template model and model types are not supported for lit
 *             template, but you can use {@code @Id} mapping and the component
 *             API or the element API with property synchronization instead.
 *             Polymer template support is deprecated - we recommend you to use
 *             {@code LitTemplate} instead. Read more details from <a href=
 *             "https://vaadin.com/blog/future-of-html-templates-in-vaadin">the
 *             Vaadin blog.</a>
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Deprecated
public class BasicComplexModelType<T> extends AbstractBasicModelType<T>
        implements ComplexModelType<T> {
    @SuppressWarnings("unchecked")
    private static final Map<Class<?>, BasicComplexModelType<?>> TYPES = loadBasicTypes(
            BasicComplexModelType::new);

    private BasicComplexModelType(Class<T> type) {
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
    public static Optional<ComplexModelType<?>> get(Class<?> type) {
        return Optional.ofNullable(TYPES.get(type));
    }

    /**
     * Checks whether the {@code type} is a basic supported type.
     *
     * @param type
     *            type to check
     * @return {@code true} is the {@code type} is basic supported type,
     *         {@code false} otherwise
     */
    public static boolean isBasicType(Type type) {
        return TYPES.keySet().contains(type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T modelToApplication(Serializable modelValue) {
        assert modelValue instanceof StateNode;
        StateNode stateNode = (StateNode) modelValue;
        assert stateNode.hasFeature(BasicTypeValue.class);
        Serializable value = stateNode.getFeature(BasicTypeValue.class)
                .getValue();

        return (T) convertToApplication(value);
    }

    @Override
    public StateNode applicationToModel(Object applicationValue,
            PropertyFilter filter) {
        if (applicationValue == null) {
            return null;
        }
        assert applicationValue instanceof Serializable;

        StateNode stateNode = new StateNode(
                Collections.singletonList(BasicTypeValue.class));
        stateNode.getFeature(BasicTypeValue.class)
                .setValue((Serializable) applicationValue);
        return stateNode;
    }

    @Override
    public <C> ComplexModelType<C> cast(Class<C> proxyType) {
        if (getJavaType() != proxyType) {
            throw new IllegalArgumentException(
                    "Got " + proxyType + ", expected " + getJavaType());
        }
        return (BasicComplexModelType<C>) this;
    }

}
