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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.OrderedList.NumberingType;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.change.NodeChange;
import com.vaadin.flow.server.AbstractStreamResource;
import com.vaadin.flow.server.streams.DownloadHandler;

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
    static {
        testValues.put(String.class, "asdf");
        testValues.put(boolean.class, false);
        testValues.put(NumberingType.class, NumberingType.LOWERCASE_ROMAN);
        testValues.put(int.class, 42);
        testValues.put(IFrame.ImportanceType.class, IFrame.ImportanceType.HIGH);
        testValues.put(IFrame.SandboxType[].class,
                new IFrame.SandboxType[] { IFrame.SandboxType.ALLOW_POPUPS,
                        IFrame.SandboxType.ALLOW_MODALS });
        testValues.put(Component.class, new Paragraph("Component"));
        testValues.put(HasText.WhiteSpace.class, HasText.WhiteSpace.PRE_LINE);
    }

    private static final Map<Class<?>, Map<Class<?>, Object>> specialTestValues = new HashMap<>();
    static {
        specialTestValues.put(NativeDetails.class, new HashMap<>());
        specialTestValues.computeIfPresent(NativeDetails.class,
                (key, nestedTestValueMap) -> {
                    nestedTestValueMap.put(boolean.class, true); // special case
                                                                 // because
                                                                 // setOpen
                                                                 // defaults to
                                                                 // false
                    return nestedTestValueMap;
                });
    }

    // For classes registered here testStringConstructor will be ignored. This
    // test checks whether the content of the
    // element is the constructor argument. However, for some HTMLComponents
    // this test is not valid.
    //
    // - NativeDetails delegates it's string constructor to the nested <summary>
    private static final Set<Class<?>> ignoredStringConstructors = new HashSet<>();
    static {
        ignoredStringConstructors.add(IFrame.class);
        ignoredStringConstructors.add(NativeDetails.class);
        ignoredStringConstructors.add(FieldSet.class);
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
                | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException e) {
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
        // Shorthand for Label.setFor(String)
        if (method.getDeclaringClass() == Label.class
                && method.getName().equals("setFor")
                && method.getParameterTypes()[0] == Component.class) {
            return true;
        }
        if (method.getDeclaringClass() == NativeLabel.class
                && method.getName().equals("setFor")
                && method.getParameterTypes()[0] == Component.class) {
            return true;
        }

        // Anchor.setTarget(AnchorTargetValue) -
        // https://github.com/vaadin/flow/issues/8346
        if (method.getDeclaringClass() == Anchor.class
                && method.getName().equals("setTarget")
                && method.getParameterTypes()[0] == AnchorTargetValue.class) {
            return true;
        }

        // setFoo(AbstractStreamResource) for resource URLs
        if (method.getParameterCount() == 1 && AbstractStreamResource.class
                .isAssignableFrom(method.getParameters()[0].getType())) {
            return true;
        }

        // - NativeDetails delegates it's setSummaryText to the nested <summary>
        // NativeDetails::setSummaryText(String summary)
        // - NativeDetails allows to setSummary(Component..) but it returns
        // Summary getSummary instead of Component[]
        // NativeDetails::setSummary(Component... components)
        if (method.getDeclaringClass() == NativeDetails.class
                && method.getName().startsWith("setSummary")) {
            return true;
        }

        // NativeTable delegates caption text to the nested <caption> element
        if (method.getDeclaringClass() == NativeTable.class
                && method.getName().startsWith("setCaptionText")) {
            return true;
        }

        if (method.getDeclaringClass() == FieldSet.class
                && method.getName().startsWith("setContent")) {
            return true;
        }

        if (method.getDeclaringClass() == IFrame.class
                && method.getName().startsWith("setSrc")) {
            return true;
        }

        if (method.getDeclaringClass() == HtmlObject.class
                && method.getName().startsWith("setData")
                && method.getParameterTypes()[0] == DownloadHandler.class) {
            return true;
        }

        if (method.getDeclaringClass() == Anchor.class
                && method.getName().startsWith("setHref")
                && method.getParameterTypes()[0] == DownloadHandler.class) {
            return true;
        }

        if (method.getDeclaringClass() == Image.class
                && method.getName().startsWith("setSrc")
                && method.getParameterTypes()[0] == DownloadHandler.class) {
            return true;
        }

        return false;
    }

    private static void testSetter(HtmlComponent instance, Method setter) {
        instance.setVisible(true);

        Class<?> propertyType = setter.getParameterTypes()[0];

        Method getter = findGetter(setter);
        Class<?> getterType = getter.getReturnType();
        boolean isOptional = (getterType == Optional.class);
        if (isOptional) {
            // setFoo(String) + Optional<String> getFoo() is ok
            Type gen = getter.getGenericReturnType();
            getterType = (Class<?>) ((ParameterizedType) gen)
                    .getActualTypeArguments()[0];
        }
        Assert.assertEquals(setter + " should have the same type as its getter",
                propertyType, getterType);

        Map<Class<?>, Object> specialValueMap = specialTestValues
                .get(instance.getClass());
        Object testValue;
        if (specialValueMap != null
                && specialValueMap.containsKey(propertyType)) {
            testValue = specialValueMap.get(propertyType);
        } else {
            testValue = testValues.get(propertyType);
        }

        if (testValue == null) {
            throw new UnsupportedOperationException(
                    "No test value for " + propertyType);
        }

        StateNode elementNode = instance.getElement().getNode();

        try {
            Object originalGetterValue = null;

            try {
                originalGetterValue = getter.invoke(instance);
                if (isOptional) {
                    originalGetterValue = ((Optional<?>) originalGetterValue)
                            .orElse(null);
                }
            } catch (InvocationTargetException e) {
                // Unable to retrieve original value, assuming null
            }

            // Purge all pending changes
            elementNode.collectChanges(c -> {
            });

            setter.invoke(instance, testValue);

            // Might have to add a blacklist for this logic at some point
            if (!testValue.equals(originalGetterValue)) {
                Assert.assertTrue(
                        setter + " should update the underlying state node",
                        hasPendingChanges(elementNode));
            }

            Object getterValue = getter.invoke(instance);
            if (isOptional) {
                getterValue = ((Optional<?>) getterValue).get();
            }

            AssertUtils.assertEquals(getter + " should return the set value",
                    testValue, getterValue);

        } catch (IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
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
            throws InstantiationException, IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {
        Supplier<HtmlComponent> constructor = customConstructors.get(clazz);
        if (constructor != null) {
            return constructor.get();
        } else {
            return clazz.getDeclaredConstructor().newInstance();
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
