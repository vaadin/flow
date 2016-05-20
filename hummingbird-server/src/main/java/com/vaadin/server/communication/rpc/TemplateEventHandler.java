/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.server.communication.rpc;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.annotations.EventHandler;
import com.vaadin.hummingbird.JsonCodec;
import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.nodefeature.ComponentMapping;
import com.vaadin.shared.JsonConstants;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;
import com.vaadin.util.ReflectTools;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;

/**
 * RPC handler for events defined in templates (which triggers client->server
 * communication).
 * 
 * @see JsonConstants#RPC_TYPE_TEMPLATE_EVENT
 * 
 * @author Vaadin Ltd
 *
 */
public class TemplateEventHandler extends AbstractInvocationHandler {

    @Override
    public String getRpcType() {
        return JsonConstants.RPC_TYPE_TEMPLATE_EVENT;
    }

    @Override
    public void handle(UI ui, JsonObject invocationJson) {
        assert invocationJson
                .hasKey(JsonConstants.RPC_TEMPLATE_EVENT_METHOD_NAME);

        StateNode node = getNode(ui, invocationJson);
        if (node == null) {
            return;
        }
        String methodName = invocationJson
                .getString(JsonConstants.RPC_TEMPLATE_EVENT_METHOD_NAME);
        if (methodName == null) {
            throw new IllegalArgumentException(
                    "Event handler method name may not be null");
        }
        JsonValue args = invocationJson
                .get(JsonConstants.RPC_TEMPLATE_EVENT_ARGS);
        if (args == null) {
            args = Json.createArray();
        }
        if (args.getType() != JsonType.ARRAY) {
            throw new IllegalArgumentException(
                    "Incorrect type for method arguments :" + args.getClass());
        }
        assert node.hasFeature(ComponentMapping.class);
        Optional<Component> component = node.getFeature(ComponentMapping.class)
                .getComponent();
        if (!component.isPresent()) {
            throw new IllegalStateException(
                    "Unable to handle RPC template event JSON message: "
                            + "there is no component available for the target node.");
        }

        invokeMethod(component.get(), component.get().getClass(), methodName,
                (JsonArray) args);

    }

    static void invokeMethod(Component instance, Class<?> clazz,
            String methodName, JsonArray args) {
        assert instance != null;
        List<Method> methods = Stream.of(clazz.getDeclaredMethods())
                .filter(method -> methodName.equals(method.getName()))
                .filter(method -> method
                        .isAnnotationPresent(EventHandler.class))
                .collect(Collectors.toList());
        if (methods.size() > 1) {
            String msg = String.format(
                    "Class '%s' contains "
                            + "several event handler method with the same name '%s'",
                    instance.getClass().getName(), methodName);
            throw new IllegalStateException(msg);
        } else if (methods.size() == 1) {
            invokeMethod(instance, methods.get(0), args);
        } else if (!Component.class.equals(clazz)) {
            invokeMethod(instance, clazz.getSuperclass(), methodName, args);
        } else {
            String msg = String.format(
                    "Neither class '%s' "
                            + "nor its super classes declare event handler method '%s'",
                    instance.getClass().getName(), methodName);
            throw new IllegalStateException(msg);
        }
    }

    private static void invokeMethod(Component instance, Method method,
            JsonArray args) {
        try {
            method.setAccessible(true);
            method.invoke(instance, decodeArgs(method, args));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            Logger.getLogger(TemplateEventHandler.class.getName())
                    .log(Level.FINE, null, e);
            throw new RuntimeException(e.getCause());
        }
    }

    private static Object[] decodeArgs(Method method,
            JsonArray argsFromClient) {
        if (argsFromClient.length() < method.getParameterCount()) {
            String msg = String.format(
                    "The number of received values (%d) is smaller "
                            + "than the number of arguments (%d) in the method '%s' "
                            + "' declared in '%s'",
                    argsFromClient.length(), method.getParameterCount(),
                    method.getName(), method.getDeclaringClass().getName());
            throw new IllegalArgumentException(msg);
        }
        if (method.getParameterCount() == 0) {
            if (argsFromClient.length() > 0) {
                String msg = String.format(
                        "Method '%s'  declared in '%s'"
                                + " has no parameters and may not be applied to "
                                + "received argument values whose length is %d",
                        method.getName(), method.getDeclaringClass().getName(),
                        argsFromClient.length());
                throw new IllegalArgumentException(msg);
            } else {
                return new Object[0];
            }
        }
        List<Object> decoded = new ArrayList<>(method.getParameterCount());
        boolean hasVarargs = argsFromClient.length() != method
                .getParameterCount();
        int argsCount = hasVarargs ? method.getParameterCount() - 1
                : method.getParameterCount();
        Class<?>[] methodParameterTypes = method.getParameterTypes();
        for (int i = 0; i < argsCount; i++) {
            Class<?> type = methodParameterTypes[i];
            decoded.add(decodeArg(method, type, i, argsFromClient.get(i)));
        }
        if (hasVarargs) {
            Class<?> type = methodParameterTypes[methodParameterTypes.length
                    - 1];
            if (!type.isArray()) {
                String msg = String.format(
                        "The number of received values is greater than "
                                + "arguments length in the method '%s' "
                                + "declared in '%s' and the last argument of the method "
                                + "has type '%s' which is not vararg and does not have an array type",
                        method.getName(), method.getDeclaringClass().getName(),
                        type.getName());
                throw new IllegalArgumentException(msg);
            }
            JsonArray rest = Json.createArray();
            int newIndex = 0;
            for (int i = method.getParameterCount() - 1; i < argsFromClient
                    .length(); i++) {
                JsonValue value = argsFromClient.get(i);
                rest.set(newIndex, value);
                newIndex++;
            }
            decoded.add(decodeArray(method, type,
                    method.getParameterCount() - 1, rest));
        }
        return decoded.toArray(new Object[method.getParameterCount()]);
    }

    private static Object decodeArg(Method method, Class<?> type, int index,
            JsonValue argValue) {
        assert argValue != null;
        if (type.isPrimitive() && argValue.getType() == JsonType.NULL) {
            String msg = String.format(
                    "Null values are not allowed for primitive types but "
                            + "a 'null' value was received for parameter %d"
                            + "which refers to primitive type '%s' "
                            + " in the method '%s' defined in the class '%s'",
                    index, type.getName(), method.getName(),
                    method.getDeclaringClass().getName());
            throw new IllegalArgumentException(msg);
        } else if (type.isArray()) {
            return decodeArray(method, type, index, argValue);
        } else {
            Class<?> convertedType = ReflectTools.convertPrimitiveType(type);
            if (!JsonCodec.canEncodeWithoutTypeInfo(convertedType)) {
                String msg = String.format(
                        "Class '%s' has the method '%s'"
                                + " whose parameter %d refers to unsupported type '%s'",
                        method.getDeclaringClass().getName(), method.getName(),
                        index, type.getName());
                throw new IllegalArgumentException(msg);
            }
            return JsonCodec.decodeAs(argValue, convertedType);
        }
    }

    private static Object decodeArray(Method method, Class<?> type, int index,
            JsonValue argValue) {
        if (argValue.getType() != JsonType.ARRAY) {
            String msg = String.format(
                    "Class '%s' has the method '%s'"
                            + " whose parameter %d refers to the array type '%s'"
                            + "but received value is not an array, its type is '%s'",
                    method.getDeclaringClass().getName(), method.getName(),
                    index, type.getName(), argValue.getType().name());
            throw new IllegalArgumentException(msg);
        }
        Class<?> componentType = type.getComponentType();
        JsonArray array = (JsonArray) argValue;
        Object result = Array.newInstance(componentType, array.length());
        for (int i = 0; i < array.length(); i++) {
            Array.set(result, i,
                    decodeArg(method, componentType, index, array.get(i)));
        }
        return result;
    }

}
