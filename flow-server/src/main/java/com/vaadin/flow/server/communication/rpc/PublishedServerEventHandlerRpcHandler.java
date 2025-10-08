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
package com.vaadin.flow.server.communication.rpc;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeType;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.AllowInert;
import com.vaadin.flow.component.template.internal.DeprecatedPolymerPublishedEventHandler;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.internal.JacksonCodec;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ClientCallableHandlers;
import com.vaadin.flow.internal.nodefeature.ComponentMapping;
import com.vaadin.flow.internal.nodefeature.PolymerServerEventHandlers;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.shared.JsonConstants;

/**
 * RPC handler for events triggered through <code>element.$server</code> or
 * simply <code>$server</code> in template event handlers.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @see JsonConstants#RPC_PUBLISHED_SERVER_EVENT_HANDLER
 * @since 1.0
 */
public class PublishedServerEventHandlerRpcHandler
        extends AbstractRpcInvocationHandler {

    private static final Collection<RpcDecoder> DECODERS = loadDecoders();

    @Override
    public String getRpcType() {
        return JsonConstants.RPC_PUBLISHED_SERVER_EVENT_HANDLER;
    }

    @Override
    protected boolean allowInert(UI ui, JsonNode invocationJson) {
        return true;
    }

    @Override
    public Optional<Runnable> handleNode(StateNode node,
            JsonNode invocationJson) {
        assert invocationJson.has(JsonConstants.RPC_TEMPLATE_EVENT_METHOD_NAME);
        String methodName = invocationJson
                .get(JsonConstants.RPC_TEMPLATE_EVENT_METHOD_NAME).asText();
        if (methodName == null) {
            throw new IllegalArgumentException(
                    "Event handler method name may not be null");
        }
        JsonNode args = invocationJson
                .get(JsonConstants.RPC_TEMPLATE_EVENT_ARGS);
        if (args == null) {
            args = JacksonUtils.createArrayNode();
        }
        if (args.getNodeType() != JsonNodeType.ARRAY) {
            throw new IllegalArgumentException(
                    "Incorrect type for method arguments: " + args.getClass());
        }
        int promiseId;
        if (invocationJson.has(JsonConstants.RPC_TEMPLATE_EVENT_PROMISE)) {
            promiseId = invocationJson
                    .get(JsonConstants.RPC_TEMPLATE_EVENT_PROMISE).intValue();
        } else {
            promiseId = -1;
        }
        assert node.hasFeature(ComponentMapping.class);
        Optional<Component> component = ComponentMapping.getComponent(node);
        if (!component.isPresent()) {
            throw new IllegalStateException(
                    "Unable to handle RPC template event JSON message: "
                            + "there is no component available for the target node");
        }

        boolean execute = node.isEnabled();

        if (!execute) {
            ClientCallableHandlers clientDelegate = node
                    .getFeature(ClientCallableHandlers.class);
            PolymerServerEventHandlers eventHandlers = node
                    .getFeature(PolymerServerEventHandlers.class);
            if (clientDelegate.hasHandler(methodName)) {
                execute = DisabledUpdateMode.ALWAYS.equals(
                        clientDelegate.getDisabledUpdateMode(methodName));
            }
            if (eventHandlers.hasHandler(methodName)) {
                execute = execute || DisabledUpdateMode.ALWAYS.equals(
                        eventHandlers.getDisabledUpdateMode(methodName));
            }
        }

        if (execute) {
            invokeMethod(component.get(), component.get().getClass(),
                    methodName, (ArrayNode) args, promiseId, node.isInert());
        }

        return Optional.empty();
    }

    static void invokeMethod(Component instance, Class<?> clazz,
            String methodName, ArrayNode args, int promiseId) {
        invokeMethod(instance, clazz, methodName, args, promiseId, false);
    }

    static void invokeMethod(Component instance, Class<?> clazz,
            String methodName, ArrayNode args, int promiseId, boolean inert) {
        assert instance != null;
        Optional<Method> method = findMethod(instance, clazz, methodName);
        if (method.isPresent()) {
            invokeMethod(instance, method.get(), args, promiseId, inert);
        } else if (instance instanceof Composite) {
            Component compositeContent = ((Composite<?>) instance).getContent();
            invokeMethod(compositeContent, compositeContent.getClass(),
                    methodName, args, promiseId, inert);
        } else {
            getLogger().error(String.format(
                    "Faulty method invocation. Neither class '%s' "
                            + "nor its super classes declare event handler method '%s'",
                    instance.getClass().getName(), methodName));
            throw new IllegalStateException(
                    "Faulty method invocation. See server log for more details.");
        }
    }

    private static Optional<Method> findMethod(Component instance,
            Class<?> clazz, String methodName) {
        List<Method> methods = Stream.of(clazz.getDeclaredMethods())
                .filter(method -> methodName.equals(method.getName()))
                .filter(method -> hasMethodAnnotation(method))
                .collect(Collectors.toList());
        if (methods.size() > 1) {
            getLogger().error(String.format(
                    "Method conflict in event handler. Class '%s' contains "
                            + "several event handler methods with the same name '%s'",
                    instance.getClass().getName(), methodName));
            throw new IllegalStateException(
                    "Method conflict in event handler with multiple methods with same name. See server log for more details.");
        } else if (methods.size() == 1) {
            return Optional.of(methods.get(0));
        } else if (!Component.class.equals(clazz)) {
            return findMethod(instance, clazz.getSuperclass(), methodName);
        } else {
            return Optional.empty();
        }
    }

    private static boolean hasMethodAnnotation(Method method) {
        // Check for polymer event handler annotation.
        final boolean hasEventHandler = ReflectTools.hasAnnotation(method,
                "com.vaadin.flow.component.polymertemplate.EventHandler");
        return hasEventHandler
                || method.isAnnotationPresent(ClientCallable.class);
    }

    private static void invokeMethod(Component instance, Method method,
            ArrayNode args, int promiseId, boolean inert) {
        if (inert && !method.isAnnotationPresent(AllowInert.class)) {
            return;
        }
        if (promiseId == -1) {
            invokeMethod(instance, method, args);
        } else {
            try {
                Serializable returnValue = (Serializable) invokeMethod(instance,
                        method, args);

                instance.getElement()
                        .executeJs("this.$server['"
                                + JsonConstants.RPC_PROMISE_CALLBACK_NAME
                                + "']($0, true, $1)",
                                Integer.valueOf(promiseId), returnValue);
            } catch (RuntimeException e) {
                instance.getElement()
                        .executeJs("this.$server['"
                                + JsonConstants.RPC_PROMISE_CALLBACK_NAME
                                + "']($0, false)", Integer.valueOf(promiseId));

                throw e;
            }
        }
    }

    private static Object invokeMethod(Component instance, Method method,
            ArrayNode args) {
        try {
            method.setAccessible(true);
            return method.invoke(instance, decodeArgs(instance, method, args));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            LoggerFactory.getLogger(
                    PublishedServerEventHandlerRpcHandler.class.getName())
                    .debug(null, e);
            throw new RuntimeException(e.getCause());
        }
    }

    private static Object[] decodeArgs(Component instance, Method method,
            ArrayNode argsFromClient) {
        int methodArgs = method.getParameterCount();
        int clientValuesCount = argsFromClient.size();
        ArrayNode argValues;
        if (method.isVarArgs()) {
            if (clientValuesCount >= methodArgs - 1) {
                argValues = unwrapVarArgs(argsFromClient, method);
            } else {
                String msg = String.format(
                        "The number of received values (%d) is not enough "
                                + "to call the method '%s' declared in '%s' which "
                                + "has vararg parameter and the number of arguments %d",
                        argsFromClient.size(), method.getName(),
                        method.getDeclaringClass().getName(),
                        method.getParameterCount());
                throw new IllegalArgumentException(msg);
            }
        } else {
            if (methodArgs == clientValuesCount) {
                argValues = argsFromClient;
            } else {
                String msg = String.format(
                        "The number of received values (%d) is not equal "
                                + "to the number of arguments (%d) in the method '%s' "
                                + "declared in '%s'",
                        argsFromClient.size(), method.getParameterCount(),
                        method.getName(), method.getDeclaringClass().getName());
                throw new IllegalArgumentException(msg);
            }
        }
        List<Object> decoded = new ArrayList<>(method.getParameterCount());
        Class<?>[] methodParameterTypes = method.getParameterTypes();
        for (int i = 0; i < argValues.size(); i++) {
            Class<?> type = methodParameterTypes[i];
            decoded.add(decodeArg(instance, method, type, i, argValues.get(i)));
        }
        return decoded.toArray(new Object[method.getParameterCount()]);
    }

    private static ArrayNode unwrapVarArgs(ArrayNode argsFromClient,
            Method method) {
        int paramCount = method.getParameterCount();
        if (argsFromClient.size() == paramCount) {
            if (argsFromClient.get(paramCount - 1).getNodeType()
                    .equals(JsonNodeType.ARRAY)) {
                return argsFromClient;
            }
        }
        ArrayNode result = JacksonUtils.createArrayNode();
        ArrayNode rest = JacksonUtils.createArrayNode();
        for (int i = 0; i < argsFromClient.size(); i++) {
            JsonNode value = argsFromClient.get(i);
            if (i < paramCount - 1) {
                result.add(value);
            } else {
                rest.add(value);
            }
        }
        result.add(rest);
        return result;
    }

    private static Object decodeArg(Component instance, Method method,
            Class<?> type, int index, JsonNode argValue) {
        // come up with method to know that it's an id and should be gotten from
        // the model
        assert argValue != null;

        if (type.isPrimitive() && argValue.getNodeType() == JsonNodeType.NULL) {
            return JacksonCodec.decodeAs(argValue, type);
        } else if (type.isArray()) {
            return decodeArray(method, type, index, argValue);
        } else if (isGenericCollection(type, argValue)) {
            return decodeGenericCollection(method, type, index, argValue);
        } else {
            Class<?> convertedType = ReflectTools.convertPrimitiveType(type);

            if (instance != null) {
                Optional<UI> ui = instance.getUI();
                if (!ui.isPresent()) {
                    throw new IllegalStateException(
                            "Rpc handler may not be called for a detached component");
                }
                VaadinContext context = ui.get().getSession().getService()
                        .getContext();
                DeprecatedPolymerPublishedEventHandler handler = context
                        .getAttribute(Lookup.class)
                        .lookup(DeprecatedPolymerPublishedEventHandler.class);
                if (handler != null && handler.isTemplateModelValue(instance,
                        argValue, convertedType)) {
                    return handler.getTemplateItem(instance, argValue,
                            method.getGenericParameterTypes()[index]);
                }
            }

            Optional<RpcDecoder> decoder = getDecoder(argValue, convertedType);
            if (decoder.isPresent()) {
                try {
                    return decoder.get().decode(argValue, convertedType);
                } catch (RpcDecodeException exception) {
                    throw new IllegalArgumentException(exception);
                }
            }
            String msg = String.format("Class '%s' has the method '%s' "
                    + "whose parameter %d refers to unsupported type '%s'",
                    method.getDeclaringClass().getName(), method.getName(),
                    index, type.getName());
            throw new IllegalArgumentException(msg);
        }

    }

    private static Optional<RpcDecoder> getDecoder(JsonNode value,
            Class<?> type) {
        return DECODERS.stream()
                .filter(decoder -> decoder.isApplicable(value, type))
                .findFirst();
    }

    private static Object decodeArray(Method method, Class<?> type, int index,
            JsonNode argValue) {
        if (argValue.getNodeType() != JsonNodeType.ARRAY) {
            String msg = String.format("Class '%s' has the method '%s' "
                    + "whose parameter %d refers to the array type '%s' "
                    + "but received value is not an array, its type is '%s'",
                    method.getDeclaringClass().getName(), method.getName(),
                    index, type.getName(), argValue.getNodeType().name());
            throw new IllegalArgumentException(msg);
        }
        Class<?> componentType = type.getComponentType();
        ArrayNode array = (ArrayNode) argValue;
        Object result = Array.newInstance(componentType, array.size());
        for (int i = 0; i < array.size(); i++) {
            Array.set(result, i, decodeArg(null, method, componentType, index,
                    array.get(i)));
        }
        return result;
    }

    private static Collection<RpcDecoder> loadDecoders() {
        List<RpcDecoder> decoders = new ArrayList<>();
        decoders.add(new StringToNumberDecoder());
        decoders.add(new StringToEnumDecoder());
        decoders.add(new DefaultRpcDecoder());
        return decoders;
    }

    private static boolean isGenericCollection(Class<?> type,
            JsonNode argValue) {
        // Check if it's a collection type and the JSON value is an array
        return Collection.class.isAssignableFrom(type)
                && argValue.getNodeType() == JsonNodeType.ARRAY;
    }

    private static Object decodeGenericCollection(Method method, Class<?> type,
            int index, JsonNode argValue) {
        try {
            // Get the generic type information
            java.lang.reflect.Type genericType = method
                    .getGenericParameterTypes()[index];

            // Use Jackson's advanced type system to handle generic collections
            tools.jackson.databind.ObjectMapper mapper = JacksonUtils
                    .getMapper();
            tools.jackson.databind.JavaType javaType = mapper.getTypeFactory()
                    .constructType(genericType);

            return mapper.treeToValue(argValue, javaType);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Failed to deserialize generic collection for parameter "
                            + index + " of method " + method.getName() + ": "
                            + e.getMessage(),
                    e);
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(
                PublishedServerEventHandlerRpcHandler.class.getName());
    }
}
