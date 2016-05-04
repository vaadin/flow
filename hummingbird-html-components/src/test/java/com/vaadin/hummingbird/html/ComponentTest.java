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
package com.vaadin.hummingbird.html;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.hummingbird.dom.Element;
import com.vaadin.ui.Component;

public abstract class ComponentTest {

    private Component component;
    private List<ComponentProperty> properties = new ArrayList<>();

    @Before
    public void setup() throws IntrospectionException, InstantiationException,
            IllegalAccessException, ClassNotFoundException {
        component = createComponent();
        addProperty("title", String.class, "", true);
        addProperty("id", String.class, "", true);
        addProperties();
        BeanInfo componentInfo = Introspector.getBeanInfo(component.getClass());
        for (PropertyDescriptor pd : componentInfo.getPropertyDescriptors()) {
            if (pd.getReadMethod() != null && pd.getWriteMethod() != null) {
                if (!hasProperty(pd.getName())) {
                    throw new IllegalStateException("Property information for '"
                            + pd.getName() + "' missing");
                }
            }
        }

    }

    protected abstract void addProperties();

    private boolean hasProperty(String name) {
        for (ComponentProperty a : properties) {
            if (a.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    protected void addStringProperty(String propertyName, String defaultValue) {
        addProperty(propertyName, String.class, defaultValue, false);
    }

    protected void addOptionalStringProperty(String propertyName) {
        addProperty(propertyName, String.class, "", true);
    }

    protected <U> void addProperty(String propertyName, Class<U> propertyType,
            U defaultValue, boolean isOptional) {
        properties.add(new ComponentProperty(getComponent().getClass(),
                propertyName, propertyType, defaultValue, isOptional));
    }

    protected Component createComponent() throws InstantiationException,
            IllegalAccessException, ClassNotFoundException {
        String componentClass = getClass().getName().replace("Test", "");
        return (Component) Class.forName(componentClass).newInstance();
    }

    protected Component getComponent() {
        return component;
    };

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
    public void testOptionalProperties() {
        properties.stream().filter(ComponentProperty::isOptional)
                .forEach(p -> testOptionalPropertyNullSetting(p));
    }

    @Test
    public void testNullForStringPropertiesWithEmptyStringDefault() {
        properties.stream().filter(p -> !p.isOptional())
                .filter(p -> p.type == String.class)
                .forEach(p -> testNullForStringPropertyWithEmptyStringDefault(
                        p));
    }

    private void testNullForStringPropertyWithEmptyStringDefault(
            ComponentProperty p) {
        try {
            p.setUsingSetter(component, null);
            Assert.fail("Setting '" + p.name
                    + "' with \"\" default to null should throw an exception");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testOptionalStringProperties() {
        properties.stream().filter(ComponentProperty::isOptional)
                .filter(p -> p.type == String.class)
                .forEach(p -> testOptionalPropertyEmptyStringSetting(p));
    }

    private void testOptionalPropertyEmptyStringSetting(ComponentProperty p) {
        try {
            p.setUsingSetter(component, "");
            Assert.assertEquals(
                    "The getter for '" + p.name
                            + "' should return an empty optional after setting \"\"",
                    Optional.empty(), p.getUsingGetter(component));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    private void testOptionalPropertyNullSetting(ComponentProperty p) {
        try {
            p.setUsingSetter(component, null);
            Assert.assertEquals(
                    "Setting the property " + p.name
                            + " to null should cause an empty optional to be returned by the getter",
                    Optional.empty(), p.getUsingGetter(component));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("The optional property '" + p.name
                    + "' does not work when setting the value to null");
        }

    }

    private void setDefaultValues() throws Exception {
        for (ComponentProperty property : properties) {
            property.setUsingSetter(component, property.defaultValue);
        }
    }

    protected void assertDefaultValues() throws Exception {
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
            assertNoPropertyOrAttribute(component, property.name);
        }
    }

    protected void assertNonDefaultValues() throws Exception {
        for (ComponentProperty property : properties) {
            if (property.optional) {
                Assert.assertEquals(
                        "Getter for " + property.name
                                + " should return Optional<"
                                + property.type.getSimpleName() + ">",
                        Optional.ofNullable(property.otherValue),
                        property.getUsingGetter(component));
            } else {
                Assert.assertEquals(property.otherValue,
                        property.getUsingGetter(component));
            }
            assertPropertyOrAttribute(component, property.name);
        }
    }

    protected void setNonDefaultValues() throws Exception {
        for (ComponentProperty a : properties) {
            a.setToOtherValueUsingSetter(component);
        }
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

    protected static void assertNoPropertyOrAttribute(Component component,
            String propertyOrAttribute) {
        assertProperty(component, propertyOrAttribute, false);
        assertAttribute(component, propertyOrAttribute, false);
    }

    protected static void assertPropertyOrAttribute(Component component,
            String propertyOrAttribute) {
        Element element = component.getElement();
        Assert.assertNotEquals(element.hasAttribute(propertyOrAttribute),
                element.hasProperty(propertyOrAttribute));
    }

    protected static void assertPropertyNotAttribute(Component component,
            String property) {
        assertProperty(component, property, true);
        assertAttribute(component, property, false);
    }

}
