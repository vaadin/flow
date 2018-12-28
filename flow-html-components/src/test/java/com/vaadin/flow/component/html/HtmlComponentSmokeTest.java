/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.component.html;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.OrderedList.NumberingType;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.change.NodeChange;
import com.vaadin.flow.server.AbstractStreamResource;

public class HtmlComponentSmokeTest {

    // Custom logic for components without a no-args constructor
    private static final Map<Class<? extends HtmlComponent>, Supplier<HtmlComponent>> customConstructors = new HashMap<>();
    static {
        customConstructors.put(HtmlComponent.class,
                () -> new HtmlComponent(Tag.DIV));
        customConstructors.put(HtmlContainer.class,
                () -> new HtmlContainer(Tag.DIV));
    }

    private static final Map<Class<?>, Object> testValues = new HashMap<>();
    private static final Map<Class<?>, Object> processTestValue = new HashMap<>();
    static {
        testValues.put(String.class, "asdf");
        testValues.put(boolean.class, false);
        testValues.put(NumberingType.class, NumberingType.LOWERCASE_ROMAN);
        testValues.put(int.class, 42);
        testValues.put(IFrame.ImportanceType.class, IFrame.ImportanceType.HIGH);
        testValues.put(IFrame.SandboxType[].class, new IFrame.SandboxType[] { IFrame.SandboxType.ALLOW_POPUPS, IFrame.SandboxType.ALLOW_MODALS });

        // Transform Arrays into Lists so that assertEqual may work properly over collections.
        testValues.keySet().stream().filter(clazz -> clazz.isArray()).forEach(clazz -> {
            processTestValue.put(clazz, Arrays.asList((Object[]) testValues.get(clazz)));
        });

    }

    // For classes registered here testStringConstructor will be ignored. This test checks whether the content of the
    // element is the constructor argument. However, for some HTMLComponents this test is not valid.
    private static final Set<Class<?>> ignoredStringConstructors = new HashSet<>();
    static {
        ignoredStringConstructors.add(IFrame.class);
    }

    private static final Collection<Function<Optional<GetterStrategyOutput>, GetterStrategyInput>> getterStrategies = new ArrayList<>(2);
    static {
//        getterStrategies.add(HtmlComponentSmokeTest::)
    }

    @Test
    public void testAllHtmlComponents() throws IOException {
        URL divClassLocationLocation = Div.class.getResource("Div.class");
        Assert.assertEquals(divClassLocationLocation.getProtocol(), "file");

        Path componentClassesLocation = new File(
                divClassLocationLocation.getPath()).getParentFile().toPath();

        Files.list(componentClassesLocation)
                .filter(HtmlComponentSmokeTest::isClassFile)
                .map(HtmlComponentSmokeTest::loadClass)
                .filter(HtmlComponentSmokeTest::isHtmlComponentSubclass)
                .map(HtmlComponentSmokeTest::asHtmlComponentSubclass)
                .forEach(HtmlComponentSmokeTest::smokeTestComponent);
    }

    private static void smokeTestComponent(
            Class<? extends HtmlComponent> clazz) {
        try {
            // Test that an instance can be created
            HtmlComponent instance = createInstance(clazz);

            // Tests that a String constructor sets the text and not the tag
            // name for a component with @Tag
            if (!ignoredStringConstructors.contains(clazz)) {
                testStringConstructor(clazz);
            }

            // Component must be attached for some checks to work
            UI ui = new UI();
            ui.add(instance);

            // Test that all setters produce a result
            testSetters(instance);
        } catch (InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static void testStringConstructor(
            Class<? extends HtmlComponent> clazz)
            throws InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        try {
            String parameterValue = "Lorem";

            Constructor<? extends HtmlComponent> constructor = clazz
                    .getConstructor(String.class);

            HtmlComponent instance = constructor.newInstance(parameterValue);

            if (clazz.getAnnotation(Tag.class) == null) {
                Assert.assertEquals(constructor
                        + " should set the tag for a class without @Tag",
                        parameterValue, instance.getElement().getTag());
            } else {
                Assert.assertEquals(constructor
                        + " should set the text content for a class with @Tag",
                        parameterValue, instance.getElement().getText());
            }
        } catch (NoSuchMethodException e) {
            // No constructor to test
            return;
        }
    }

    private static void testSetters(HtmlComponent instance) {
        Arrays.stream(instance.getClass().getMethods())
                .filter(HtmlComponentSmokeTest::isSetter)
                .filter(m -> !isSpecialSetter(m))
                .forEach(m -> testSetter(instance, m));
    }

    private static boolean isSetter(Method method) {
        if (method.isSynthetic()) {
            return false;
        }
        if (!method.getName().startsWith("set")) {
            return false;
        }

        if (method.getParameterTypes().length != 1) {
            return false;
        }

        int modifiers = method.getModifiers();
        if (Modifier.isStatic(modifiers)) {
            return false;
        }

        if (Modifier.isAbstract(modifiers)) {
            return false;
        }

        return true;
    }

    private static boolean isSpecialSetter(Method method) {
        // Shorthand for Lablel.setFor(String)
        if (method.getDeclaringClass() == Label.class
                && method.getName().equals("setFor")
                && method.getParameterTypes()[0] == Component.class) {
            return true;
        }
        // setFoo(AbstractStreamResource) for resource URLs
        if (method.getParameterCount() == 1 && AbstractStreamResource.class
                .isAssignableFrom(method.getParameters()[0].getType())) {
            return true;
        }

        return false;
    }

    private static void testSetter(HtmlComponent instance, Method setter) {
        Method getter = findGetter(setter);
        GetterStrategyInput getterStrategyInput = null;

        Class<?> propertyType = setter.getParameterTypes()[0];

        Object testValue = testValues.get(propertyType);

        if (testValue == null) {
            throw new UnsupportedOperationException(
                    "No test value for " + propertyType);
        }

        StateNode elementNode = instance.getElement().getNode();

        try {
            // Purge all pending changes
            elementNode.collectChanges(c -> {
            });

            setter.invoke(instance, testValue);

            // Might have to add a blacklist for this logic at some point
            Assert.assertTrue(
                    setter + " should update the underlying state node",
                    hasPendingChanges(elementNode));

            Object getterValue = getter.invoke(instance);

            getterStrategyInput = GetterStrategyInput.with(getter, getterValue);

        } catch (IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new RuntimeException(e);
        }


        // Validate getter type and value.
        Optional<GetterStrategyOutput> getterStrategyOutput = process(getterStrategyInput);

        Assert.assertTrue(
                "Undefined output handling for " + getter,
                getterStrategyOutput.isPresent());

        // Try getting the actual test value.
        Object getterTestValue = processTestValue.get(propertyType);
        if (getterTestValue != null) {
            testValue = getterTestValue;
        }

        Object finalTestValue = testValue;

        getterStrategyOutput.ifPresent(output -> {
            Assert.assertEquals(setter + " should have the same type as its getter",
                    propertyType, output.type);

            Assert.assertEquals(getter + " should return the set value",
                    finalTestValue, output.value);
        });

    }

    private static Optional<GetterStrategyOutput> process(GetterStrategyInput input) {
        Class<?> getterType = input.getter.getReturnType();

        if (getterType == Optional.class) {
            Type actualType = ((ParameterizedType) input.getter.getGenericReturnType()).getActualTypeArguments()[0];

            if (actualType instanceof Class<?>) {
                // Getter's return value is an Optional wrapping a simple type.
                return Optional.of(GetterStrategyOutput.with(actualType, ((Optional<?>) input.result).get()));

            } else if (actualType instanceof ParameterizedType) {
                // Getter's return value is an Optional wrapping a generic type.

            }

        } else {
            // Getter's return value is a simple type.
            return Optional.of(GetterStrategyOutput.with(getterType, input.result));
        }

        return Optional.empty();
    }

    // Return the implicit type if found.
    private static Optional<GetterStrategyOutput> implicitType(GetterStrategyInput input) {
        Class<?> getterType = input.getter.getReturnType();

        if (getterType != Optional.class) {
            return Optional.of(GetterStrategyOutput.with(getterType, input.result));
        }

        return Optional.empty();
    }

    // Return the implicit type if found.
    private static Optional<GetterStrategyOutput> optionalType(GetterStrategyInput input) {
        Class<?> getterType = input.getter.getReturnType();

        if (getterType == Optional.class) {
            Type actualType = ((ParameterizedType) input.getter.getGenericReturnType()).getActualTypeArguments()[0];

            if (actualType instanceof Class<?>) {
                return Optional.of(GetterStrategyOutput.with(actualType, ((Optional<?>) input.result).get()));
            }
        }

        return Optional.empty();
    }

    private static boolean hasPendingChanges(StateNode elementNode) {
        List<NodeChange> changes = new ArrayList<>();

        elementNode.collectChanges(changes::add);

        return !changes.isEmpty();
    }

    private static Method findGetter(Method setter) {
        String setterName = setter.getName();
        String getterName;

        Class<?>[] parameterTypes = setter.getParameterTypes();
        if (parameterTypes.length == 1
                && boolean.class.equals(parameterTypes[0])) {
            getterName = setterName.replaceFirst("set", "is");
        } else {
            getterName = setterName.replaceFirst("set", "get");
        }

        try {
            return setter.getDeclaringClass().getMethod(getterName);
        } catch (NoSuchMethodException | SecurityException e) {
            // Should add support for isXyz when needed
            throw new RuntimeException("No getter found for " + setter);
        }
    }

    private static HtmlComponent createInstance(
            Class<? extends HtmlComponent> clazz)
            throws InstantiationException, IllegalAccessException {
        Supplier<HtmlComponent> constructor = customConstructors.get(clazz);
        if (constructor != null) {
            return constructor.get();
        } else {
            return clazz.newInstance();
        }
    }

    private static Class<?> loadClass(Path classFile) {
        String className = classFile.getFileName().toString()
                .replaceAll("\\.class$", "");
        String qualifiedName = Div.class.getPackage().getName() + "."
                + className;

        try {
            return Class.forName(qualifiedName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isClassFile(Path path) {
        return path.toString().endsWith(".class");
    }

    private static boolean isHtmlComponentSubclass(Class<?> cls) {
        return HtmlComponent.class.isAssignableFrom(cls);
    }

    private static Class<? extends HtmlComponent> asHtmlComponentSubclass(
            Class<?> cls) {
        return cls.asSubclass(HtmlComponent.class);
    }
}

// Wraps a getter method and a value it returned.
class GetterStrategyInput {

    final Method getter;

    final Object result;

    static GetterStrategyInput with(Method getter, Object result) {
        return new GetterStrategyInput(getter, result);
    }

    private GetterStrategyInput(Method getter, Object result) {
        this.getter = getter;
        this.result = result;
    }
}

// Wraps a value and its type in form of a tuple.
class GetterStrategyOutput {

    final Type type;

    final Object value;

    static GetterStrategyOutput with(Type type, Object value) {
        return new GetterStrategyOutput(type, value);
    }

    private GetterStrategyOutput(Type type, Object value) {

        if (value.getClass().isArray()) {
            value = Arrays.asList((Object[]) value);
        }

        this.type = type;
        this.value = value;
    }
}