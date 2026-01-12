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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.googlecode.gentyref.GenericTypeReflector;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;
import com.vaadin.flow.internal.nodefeature.ModelList;

/**
 * A model type corresponding to a list of bean types.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @param <T>
 *            the proxy type used by the bean type of this type
 *
 * @deprecated Template model and polymer template support is deprecated - we
 *             recommend you to use {@code LitTemplate} instead. Read more
 *             details from <a href=
 *             "https://vaadin.com/blog/future-of-html-templates-in-vaadin">the
 *             Vaadin blog.</a> For lit templates, you can use {@code @Id}
 *             mapping and the component API or the element API with property
 *             synchronization instead.
 */
@Deprecated
public class ListModelType<T> implements ComplexModelType<T> {

    private ComplexModelType<T> itemType;

    /**
     * Creates a new list model type with the given bean model type.
     *
     * @param itemType
     *            the model type of the list items
     */
    public ListModelType(ComplexModelType<T> itemType) {
        assert itemType != null;
        this.itemType = itemType;
    }

    /**
     * Gets the item type.
     *
     * @return the item type, not <code>null</code>
     */
    public ComplexModelType<T> getItemType() {
        return itemType;
    }

    @Override
    public List<T> modelToApplication(Serializable modelValue) {
        if (modelValue instanceof StateNode) {
            return new TemplateModelListProxy<>((StateNode) modelValue,
                    itemType);
        } else {
            throw new IllegalArgumentException(String.format(
                    "The stored model value '%s' type '%s' "
                            + "cannot be used as a type for a model list property",
                    modelValue, modelValue.getClass()));
        }
    }

    @Override
    public StateNode applicationToModel(Object applicationValue,
            PropertyFilter filter) {
        if (applicationValue == null) {
            return null;
        }

        StateNode node = new StateNode(
                Collections.singletonList(ModelList.class));

        @SuppressWarnings("unchecked")
        List<T> list = (List<T>) applicationValue;

        importBeans(node.getFeature(ModelList.class), list, filter);

        return node;
    }

    @Override
    public <C> ComplexModelType<C> cast(Class<C> proxyType) {
        if (getItemType() instanceof ListModelType<?>
                && GenericTypeReflector.erase(proxyType).equals(List.class)) {
            return (ComplexModelType<C>) this;
        }
        throw new IllegalArgumentException(
                "Got " + proxyType + ", expected list type");

    }

    /**
     * Checks if the given type will be handled as a list of beans in the model.
     *
     * @param type
     *            the type to check
     * @return <code>true</code> if the given type will be handled as a list of
     *         beans, <code>false</code> otherwise
     */
    public static boolean isList(Type type) {
        return type instanceof ParameterizedType
                && ((ParameterizedType) type).getRawType().equals(List.class);
    }

    /**
     * Imports beans into a model list based on the properties in the item type
     * of this model type.
     *
     * @param modelList
     *            the model list to import beans into
     * @param beans
     *            the list of beans to import
     * @param propertyFilter
     *            defines which properties from the item model type to import
     */
    public void importBeans(ModelList modelList, List<T> beans,
            PropertyFilter propertyFilter) {
        // Collect all child nodes before clearing anything
        List<StateNode> childNodes = new ArrayList<>();
        for (Object bean : beans) {
            StateNode childNode = itemType.applicationToModel(bean,
                    propertyFilter);
            childNodes.add(childNode);
        }

        modelList.clear();

        modelList.addAll(childNodes);
    }

    @Override
    public boolean accepts(Type applicationType) {
        return isList(applicationType);
    }

    @Override
    public Type getJavaType() {
        return ReflectTools.createParameterizedType(List.class,
                getItemType().getJavaType());
    }

    @Override
    public JsonNode toJson() {
        return JacksonUtils.createArray(itemType.toJson());
    }

    @Override
    public void createInitialValue(StateNode node, String property) {
        node.getFeature(ElementPropertyMap.class).resolveModelList(property);
    }
}
