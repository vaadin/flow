/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.internal.nodefeature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.template.internal.DeprecatedPolymerTemplate;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.internal.StateNode;

/**
 * Methods which are published as event-handlers on the client side.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @deprecated Polymer template support is deprecated - we recommend you to use
 *             {@code LitTemplate} instead. Read more details from <a href=
 *             "https://vaadin.com/blog/future-of-html-templates-in-vaadin">the
 *             Vaadin blog.</a>
 *
 */
@Deprecated
public class PolymerServerEventHandlers
        extends AbstractServerHandlers<DeprecatedPolymerTemplate> {
    private static final String VALUE = "value";

    private static final String REPEAT_INDEX_VALUE = "event.model.index";

    private static final String MODEL_ITEM_FQN = "com.vaadin.flow.component.polymertemplate.ModelItem";
    private static final String REPEAT_INDEX_FQN = "com.vaadin.flow.component.polymertemplate.RepeatIndex";

    /**
     * Creates a new meta information list for the given state node.
     *
     * @param node
     *            the state node this list belongs to
     */
    public PolymerServerEventHandlers(StateNode node) {
        super(node);
    }

    @Override
    protected void addHandlerMethod(Method method, Collection<Method> methods) {
        super.addHandlerMethod(method, methods);

        addMethodParameters(method);
    }

    @Override
    protected void ensureSupportedParameterTypes(Method method) {
        Stream.of(method.getParameters())
                .forEach(parameter -> checkParameterTypeAndAnnotation(method,
                        parameter));
    }

    @Override
    protected String getHandlerAnnotationFqn() {
        return "com.vaadin.flow.component.polymertemplate.EventHandler";
    }

    @Override
    protected DisabledUpdateMode getUpdateMode(Method method) {
        Optional<Annotation> annotation = ReflectTools.getAnnotation(method,
                getHandlerAnnotationFqn());
        assert annotation.isPresent();
        Object value = ReflectTools.getAnnotationMethodValue(annotation.get(),
                VALUE);
        return DisabledUpdateMode.valueOf(value.toString());
    }

    private void checkParameterTypeAndAnnotation(Method method,
            Parameter parameter) {
        boolean hasEventDataAnnotation = parameter
                .isAnnotationPresent(EventData.class)
                || ReflectTools.hasAnnotation(parameter, MODEL_ITEM_FQN);
        boolean hasRepeatIndexAnnotation = ReflectTools.hasAnnotation(parameter,
                REPEAT_INDEX_FQN);

        if (!Boolean.logicalXor(hasEventDataAnnotation,
                hasRepeatIndexAnnotation)) {
            throw new IllegalStateException(String.format(
                    "EventHandler method '%s' should have the parameter with index %s annotated either with @EventData annotation (to get any particular data from the event)"
                            + " or have 'int' or 'Integer' type and be annotated with @RepeatIndex annotation (to get element index in dom-repeat)",
                    method.getName(), getParameterIndex(parameter)));
        } else if (!hasEventDataAnnotation) {
            Class<?> parameterType = parameter.getType();
            if (!parameterType.equals(int.class)
                    && !parameterType.equals(Integer.class)) {
                throw new IllegalStateException(String.format(
                        "EventHandler method '%s' has parameter with index %s, annotated with @RepeatIndex that has incorrect type '%s', should be 'int' or 'Integer'",
                        method.getName(), getParameterIndex(parameter),
                        parameterType));
            }
        }
    }

    private static String getParameterIndex(Parameter parameter) {
        return parameter.getName().replace("arg", "");
    }

    private void addMethodParameters(Method method) {
        getNode().getFeature(PolymerEventListenerMap.class)
                .add(method.getName(), getParameters(method));
    }

    private String[] getParameters(Method method) {
        List<String> result = new ArrayList<>();
        for (Parameter parameter : method.getParameters()) {
            EventData eventData = parameter.getAnnotation(EventData.class);
            if (eventData != null) {
                result.add(eventData.value());
            }
            if (ReflectTools.hasAnnotation(parameter, REPEAT_INDEX_FQN)) {
                result.add(REPEAT_INDEX_VALUE);
            }
            Optional<Annotation> annotation = ReflectTools
                    .getAnnotation(parameter, MODEL_ITEM_FQN);
            if (annotation.isPresent()) {
                result.add(ReflectTools
                        .getAnnotationMethodValue(annotation.get(), VALUE)
                        .toString());
            }
        }
        return result.toArray(new String[result.size()]);
    }

}
