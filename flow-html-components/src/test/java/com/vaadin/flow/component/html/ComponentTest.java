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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasAriaLabel;
import com.vaadin.flow.component.HasOrderedComponents;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.dom.Element;

public abstract class ComponentTest {

    private Component component;
    private List<ComponentProperty> properties = new ArrayList<>();

    private Set<String> WHITE_LIST = new HashSet<>();

    @Before
    public void setup() throws IntrospectionException, InstantiationException,
            IllegalAccessException, ClassNotFoundException {
        component = createComponent();
        whitelistProperty("visible");
        addProperties();
        BeanInfo componentInfo = Introspector.getBeanInfo(component.getClass());
        Stream.of(componentInfo.getPropertyDescriptors()).filter(
                descriptor -> !WHITE_LIST.contains(descriptor.getName()))
                .forEach(this::assertProperty);
    }

    protected void whitelistProperty(String property) {
        WHITE_LIST.add(property);
    }

    protected void addProperties() {
        addOptionalStringProperty("title");
        addOptionalStringProperty("id");
    }

    private boolean hasProperty(String name) {
        for (ComponentProperty a : properties) {
            if (a.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    protected void addStringProperty(String propertyName, String defaultValue) {
        addProperty(propertyName, String.class, defaultValue, false, true);
    }

    protected void addStringProperty(String propertyName, String defaultValue,
            boolean removeDefault) {
        addProperty(propertyName, String.class, defaultValue, false,
                removeDefault);
    }

    protected void addOptionalStringProperty(String propertyName) {
        addProperty(propertyName, String.class, "", true);
    }

    protected <U> void addProperty(String propertyName, Class<U> propertyType,
            U defaultValue, boolean isOptional) {
        addProperty(propertyName, propertyType, defaultValue, isOptional, true);
    }

    protected <U> void addProperty(String propertyName, Class<U> propertyType,
            U defaultValue, boolean isOptional, boolean removeDefault) {
        addProperty(propertyName, propertyType, defaultValue, null, isOptional,
                removeDefault);
    }

    protected <U> ComponentProperty addProperty(String propertyName,
            Class<U> propertyType, U defaultValue, U otherValue,
            boolean isOptional, boolean removeDefault) {
        final ComponentProperty property = new ComponentProperty(
                getComponent().getClass(), propertyName, propertyType,
                defaultValue, otherValue, isOptional, removeDefault);
        properties.add(property);
        return property;
    }

    protected <U> void addProperty(String propertyName, Class<U> propertyType,
            U defaultValue, U otherValue, boolean isOptional,
            boolean removeDefault, String propertyOrAttributeTag) {
        ComponentProperty property = new ComponentProperty(
                getComponent().getClass(), propertyName, propertyType,
                defaultValue, otherValue, isOptional, removeDefault);
        property.setPropertyOrAttributeTag(propertyOrAttributeTag);
        properties.add(property);
    }

    protected Component createComponent() throws InstantiationException,
            IllegalAccessException, ClassNotFoundException,
            NoSuchMethodException, InvocationTargetException {
        String componentClass = getClass().getName().replace("Test", "");
        return (Component) Class.forName(componentClass)
                .getDeclaredConstructor().newInstance();
    }

    protected Component getComponent() {
        return component;
    }

    protected void testHasOrderedComponents() {
        if (!(component instanceof HasOrderedComponents)) {
            Assert.fail("Component " + component.getClass()
                    + " did not implement HasOrderedComponents anymore.");
        }

        HasOrderedComponents component = (HasOrderedComponents) this.component;
        Assert.assertEquals(0, component.getComponentCount());

        Paragraph firstChildren = new Paragraph("first children");
        component.add(firstChildren);
        Assert.assertEquals(firstChildren, component.getComponentAt(0));

        Paragraph newFirstChildren = new Paragraph("new first children");
        component.addComponentAtIndex(0, newFirstChildren);
        Assert.assertEquals(newFirstChildren, component.getComponentAt(0));

        Assert.assertEquals(1, component.indexOf(firstChildren));
    }

    protected void testHasAriaLabelIsImplemented() {
        if (!(component instanceof HasAriaLabel)) {
            Assert.fail("Component " + component.getClass()
                    + " did not implement HasAriaLabel anymore.");
        }
    }

    protected void testHasAriaLabelIsNotImplemented() {
        if (component instanceof HasAriaLabel) {
            Assert.fail("Component " + component.getClass()
                    + " implemented HasAriaLabel. This is not valid."
                    + " See test case for more information.");
        }
    }

    @Test
    public void setAriaLabel() {
        if (!(component instanceof HasAriaLabel)) {
            return; // test should only run for components that implement
                    // HasAriaLabel
        }

        HasAriaLabel component = (HasAriaLabel) this.component;
        Assert.assertFalse(component.getAriaLabel().isPresent());

        component.setAriaLabel("new AriaLabel");
        Assert.assertEquals("new AriaLabel", component.getAriaLabel().get());
    }

    @Test
    public void testSetterGetterTypes() throws Exception {
        for (ComponentProperty property : properties) {
            Class<?> getterType = property.getGetter().getReturnType();
            if (property.optional) {
                String message = "Getter for " + property.name
                        + " should return Optional<"
                        + property.type.getSimpleName() + ">";
                Assert.assertEquals(message, Optional.class, getterType);
                Assert.assertEquals(message, property.type, getOptionalType(
                        property.getGetter().getGenericReturnType()));
            } else {
                Assert.assertEquals(property.type, getterType);
            }

            Assert.assertEquals(property.type,
                    property.getSetter().getParameterTypes()[0]);
        }
    }

    public Class<?> getOptionalType(Type type) {
        return (Class<?>) ((ParameterizedType) type)
                .getActualTypeArguments()[0];
    }

    @Test
    public void testDefaultValues() throws Exception {
        setDefaultValues();
    }

    @Test
    public void testNonDefaultValues() throws Exception {
        setNonDefaultValues();
        assertNonDefaultValues();
    }

    @Test
    public void setAndResetValues() throws Exception {
        setNonDefaultValues();
        setDefaultValues();
        assertDefaultValues();
    }

    @Test
    public void testNullForOptionalNonStringProperties() {
        Stream<ComponentProperty> optionalNonStringProperties = properties
                .stream().filter(ComponentProperty::isOptional)
                .filter(p -> p.type != String.class);

        optionalNonStringProperties
                .forEach(this::testNullForOptionalNonStringProperty);
    }

    @Test
    public void testNullForOptionalStringProperties() {
        getOptionalStringProperties()
                .forEach(this::testNullForOptionalStringProperty);
    }

    @Test
    public void testEmptyStringForOptionalStringProperties() {
        getOptionalStringProperties()
                .forEach(this::testEmptyStringForOptionalStringProperty);
    }

    @Test
    public void testNullForStringPropertiesWithEmptyStringDefault() {
        properties.stream().filter(p -> !p.isOptional())
                .filter(p -> p.type == String.class)
                .forEach(p -> testNullForStringPropertyWithEmptyStringDefault(
                        p));
    }

    @Test
    public void setTitle() {
        if (!(component instanceof HtmlComponent)) {
            return;
        }

        HtmlComponent component = (HtmlComponent) this.component;
        Assert.assertFalse(component.getTitle().isPresent());

        component.setTitle("myTitle");

        Assert.assertEquals("myTitle",
                component.getElement().getAttribute("title"));
        Assert.assertEquals("myTitle", component.getTitle().orElse(null));
        Assert.assertFalse(component.getElement().hasProperty("title"));

        component.setTitle("");
        Assert.assertFalse(component.getTitle().isPresent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setTitle_nullDisallowed() {
        if (!(component instanceof HtmlComponent)) {
            throw new IllegalArgumentException();
        }

        ((HtmlComponent) component).setTitle(null);
    }

    private Stream<ComponentProperty> getOptionalStringProperties() {
        return properties.stream().filter(ComponentProperty::isOptional)
                .filter(p -> p.type == String.class);
    }

    private void testNullForStringPropertyWithEmptyStringDefault(
            ComponentProperty p) {
        try {
            p.setUsingSetter(component, null);
            Assert.fail("Setting '" + p.name
                    + "' with \"\" default to null should throw an exception");
        } catch (InvocationTargetException e) {
            // OK, should throw
            Assert.assertEquals(IllegalArgumentException.class,
                    e.getCause().getClass());
        } catch (IllegalAccessException | IllegalArgumentException
                | NoSuchMethodException | SecurityException e) {
            throw new AssertionError(e);
        }
    }

    private void testEmptyStringForOptionalStringProperty(ComponentProperty p) {
        try {
            p.setUsingSetter(component, "");
            Assert.assertEquals("The getter for '" + p.name
                    + "' should return an empty optional after setting \"\"",
                    Optional.empty(), p.getUsingGetter(component));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private void testNullForOptionalNonStringProperty(ComponentProperty p) {
        try {
            p.setUsingSetter(component, null);
            Assert.assertEquals("Setting the property " + p.name
                    + " to null should cause an empty optional to be returned by the getter",
                    Optional.empty(), p.getUsingGetter(component));
        } catch (Exception e) {
            throw new AssertionError(
                    "The optional property '" + p.name
                            + "' does not work when setting the value to null",
                    e);
        }

    }

    private void testNullForOptionalStringProperty(ComponentProperty p) {
        try {
            p.setUsingSetter(component, null);
            Assert.fail(
                    "Setting an optional String attribute to null should throw an exception");
        } catch (InvocationTargetException e) {
            Assert.assertEquals(IllegalArgumentException.class,
                    e.getCause().getClass());
        } catch (IllegalAccessException | IllegalArgumentException
                | NoSuchMethodException | SecurityException e) {
            throw new AssertionError(e);
        }

    }

    private void setDefaultValues() throws Exception {
        for (ComponentProperty property : properties) {
            property.setUsingSetter(component, property.defaultValue);
        }
    }

    private void assertDefaultValues() throws Exception {
        for (ComponentProperty property : properties) {
            if (property.optional) {
                String message = "Default value for " + property.name
                        + " should be an empty Optional";
                Assert.assertEquals(message, Optional.empty(),
                        property.getUsingGetter(component));
            } else {
                Assert.assertEquals(property.defaultValue,
                        property.getUsingGetter(component));
            }
            if (property.removeDefault) {
                assertNoPropertyOrAttribute(component, property.name);
            } else {
                assertPropertyOrAttribute(component, property.name);
            }
        }
    }

    private void assertNonDefaultValues() throws Exception {
        for (ComponentProperty property : properties) {
            if (property.optional) {
                AssertUtils.assertEquals(
                        "Getter for " + property.name
                                + " should return Optional<"
                                + property.type.getSimpleName() + ">",
                        Optional.ofNullable(property.otherValue),
                        property.getUsingGetter(component));
            } else {
                Assert.assertEquals(property.otherValue,
                        property.getUsingGetter(component));
            }
            assertPropertyOrAttribute(component,
                    property.propertyOrAttributeTag);
        }
    }

    private void setNonDefaultValues() throws Exception {
        properties.forEach(p -> p.setToOtherValueUsingSetter(component));
    }

    private static void assertProperty(Component component, String property,
            boolean present) {
        Assert.assertEquals(present,
                component.getElement().hasProperty(property));
    }

    private static void assertAttribute(Component component, String attribute,
            boolean present) {
        Assert.assertEquals(
                "Component should " + (present ? "" : "not ")
                        + "have the attribute " + attribute,
                present, component.getElement().hasAttribute(attribute));
    }

    private static void assertNoPropertyOrAttribute(Component component,
            String propertyOrAttribute) {
        assertProperty(component, propertyOrAttribute, false);
        assertAttribute(component, propertyOrAttribute, false);
    }

    /**
     * Asserts that the component has either property or attribute with given
     * name; fails if both are present or neither is present.
     *
     * @param component
     * @param propertyOrAttribute
     */
    private static void assertPropertyOrAttribute(Component component,
            String propertyOrAttribute) {
        Element element = component.getElement();
        Assert.assertNotEquals(propertyOrAttribute + ": attribute="
                + element.hasAttribute(propertyOrAttribute) + ", property="
                + element.hasProperty(propertyOrAttribute),
                element.hasAttribute(propertyOrAttribute),
                element.hasProperty(propertyOrAttribute));
    }

    private void assertProperty(PropertyDescriptor descriptor) {
        if (descriptor.getReadMethod() != null
                && descriptor.getWriteMethod() != null) {
            // JDK 21 detects properties also from interface default methods
            // Test setup can safely ignore properties from mixin interfaces
            if (descriptor.getReadMethod().isDefault()
                    && descriptor.getWriteMethod().isDefault()) {
                return;
            }
            if (!hasProperty(descriptor.getName())) {
                throw new IllegalStateException("Property information for '"
                        + descriptor.getName() + "' missing");
            }
        }
    }
}
