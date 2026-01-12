/**
 * Copyright (C) 2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.polymertemplate.rpc;

import java.lang.reflect.Type;
import java.util.Optional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.component.template.internal.DeprecatedPolymerPublishedEventHandler;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.templatemodel.ModelType;

/**
 * Polymer utilitiy class for handling polymer rpc events for @EventHandler.
 *
 * Registers {@link PolymerPublishedEventRpcHandler} as a service to make it
 * available in {@link Lookup} in an OSGi container.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 */
// This is OSGi specific annotation for the class which may be used without
// OSGi. But RetentionPolicy.CLASS used for the annotation makes it safe to use
// in runtime because JVM doesn't see it.
@org.osgi.service.component.annotations.Component(immediate = true)
public class PolymerPublishedEventRpcHandler
        implements DeprecatedPolymerPublishedEventHandler {

    /**
     * Validate that the given Component instance is a PolymerTemplate and that
     * the value can be converted.
     *
     * @param instance
     *            Component to be validated
     * @param argValue
     *            received value
     * @param convertedType
     *            target type that value should be converted to
     * @return true if valid template model value
     */
    @Override
    public boolean isTemplateModelValue(Component instance, JsonNode argValue,
            Class<?> convertedType) {
        return instance instanceof PolymerTemplate
                && argValue instanceof ObjectNode
                && ((PolymerTemplate<?>) instance).isSupportedClass(
                        convertedType)
                && argValue.has("nodeId");
    }

    /**
     * Get the template model object and type.
     *
     * @param template
     *            polymer template to get model from
     * @param argValue
     *            argument value
     * @param convertedType
     *            value type
     * @return the provided model value
     * @throws IllegalStateException
     *             if the component is not attached to the UI
     */
    @Override
    public Object getTemplateItem(Component template, JsonNode argValue,
            Type convertedType) {
        final Optional<UI> ui = template.getUI();
        if (ui.isPresent()) {
            StateNode node = ui.get().getInternals().getStateTree()
                    .getNodeById(argValue.get("nodeId").intValue());

            ModelType propertyType = ((PolymerTemplate<?>) template)
                    .getModelType(convertedType);

            return propertyType.modelToApplication(node);
        }
        throw new IllegalArgumentException(
                "Event sent for a non attached template component");
    }

}
