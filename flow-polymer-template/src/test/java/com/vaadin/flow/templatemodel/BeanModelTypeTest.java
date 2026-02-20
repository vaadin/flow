/**
 * Copyright (C) 2022-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.templatemodel;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.internal.JacksonCodec;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeanModelTypeTest {
    // Partial overlap with Bean
    public static class DifferentBean {
        private double intValue;
        private String string;
        private Date date;

        public DifferentBean() {
            // Using default values
        }

        public DifferentBean(long value) {
            intValue = value;
            string = String.valueOf(value);
            date = new Date(value);
        }

        public double getIntValue() {
            return intValue;
        }

        void setIntValue(double intValue) {
            this.intValue = intValue;
        }

        public String getString() {
            return string;
        }

        void setString(String string) {
            this.string = string;
        }

        void setDate(Date date) {
            this.date = date;
        }

        public Date getDate() {
            return date;
        }
    }

    public interface DeniedPropertyWithGetterModel extends TemplateModel {

        @AllowClientUpdates(ClientUpdateMode.DENY)
        String getName();
    }

    public interface AllowPropertyModel extends TemplateModel {

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        String getName();

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        void setAge(int age);
    }

    public interface TwoWayBindingPropertyModel extends TemplateModel {

        @AllowClientUpdates(ClientUpdateMode.IF_TWO_WAY_BINDING)
        String getName();

        @AllowClientUpdates(ClientUpdateMode.IF_TWO_WAY_BINDING)
        void setAge(int age);
    }

    public interface ListPropertyModel extends TemplateModel {

        @AllowClientUpdates()
        @AllowClientUpdates(path = "intValue", value = ClientUpdateMode.DENY)
        @AllowClientUpdates(path = "doubleValue", value = ClientUpdateMode.IF_TWO_WAY_BINDING)
        List<Bean> getList();
    }

    public interface SubPropertyModel extends TemplateModel {

        @AllowClientUpdates()
        @AllowClientUpdates(path = "intValue", value = ClientUpdateMode.DENY)
        @AllowClientUpdates(path = "doubleValue", value = ClientUpdateMode.IF_TWO_WAY_BINDING)
        Bean getBean();
    }

    @Test
    void onlyDenyProperty_noAllowedProperties() {
        assertEquals(0, getClientUpdateAllowedProperties(
                DeniedPropertyWithGetterModel.class).size());

        assertEquals(0, getClientUpdateAllowedProperties(
                DeniedPropertyWithGetterModel.class, "name").size());
    }

    @Test
    void twoWayDatabindingPropertyDeclared_propertyIsNotAllowedIfNotTwoWayDataBinding() {
        assertEquals(0, getClientUpdateAllowedProperties(
                TwoWayBindingPropertyModel.class).size());

        // test name property
        assertEquals(1, getClientUpdateAllowedProperties(
                TwoWayBindingPropertyModel.class, "name").size());
        // name property has getter
        assertTrue(getClientUpdateAllowedProperties(
                TwoWayBindingPropertyModel.class, "name").get("name"));

        // test age property
        assertEquals(1, getClientUpdateAllowedProperties(
                TwoWayBindingPropertyModel.class, "age").size());
        // age property has no getter
        assertFalse(getClientUpdateAllowedProperties(
                TwoWayBindingPropertyModel.class, "age").get("age"));
    }

    @Test
    void allowPropertyDeclared_propertyIsAllowed() {
        Map<String, Boolean> properties = getClientUpdateAllowedProperties(
                AllowPropertyModel.class);
        assertEquals(2, properties.size());

        // name property has getter
        assertTrue(properties.get("name"));

        // age property has no getter
        assertFalse(properties.get("age"));
    }

    @Test
    void allowListSubPropertyDeclared_propertyIsAllowed() {
        Map<String, Boolean> properties = getClientUpdateAllowedProperties(
                ListPropertyModel.class, "list.booleanValue");
        // Bean has a couple of explicitly allowed properties
        assertEquals(4, properties.size());

        // property has getter
        assertTrue(properties.get("list.booleanValue"));

        // list property has getter
        assertTrue(properties.get("list"));
    }

    @Test
    void denyListSubPropertyDeclared_propertyIsDisallowed() {
        Map<String, Boolean> properties = getClientUpdateAllowedProperties(
                ListPropertyModel.class, "list.intValue");
        // Bean has a couple of explicitly allowed properties
        assertEquals(3, properties.size());

        assertFalse(properties.containsKey("list.intValue"));

        // list property has getter
        assertTrue(properties.get("list"));
    }

    @Test
    void allowListSubPropertyDeclared_propertyIsTwoWayDataBinding_propertyIsAllowed() {
        Map<String, Boolean> properties = getClientUpdateAllowedProperties(
                ListPropertyModel.class, "list.doubleValue");
        // Bean has a couple of explicitly allowed properties
        assertEquals(4, properties.size());

        assertTrue(properties.get("list.doubleValue"));

        // list property has getter
        assertTrue(properties.get("list"));
    }

    @Test
    void denyListSubPropertyDeclared_propertyIsNotTwoWayDataBinding_propertyIsDisallowed() {
        Map<String, Boolean> properties = getClientUpdateAllowedProperties(
                ListPropertyModel.class);
        // Bean has a couple of explicitly allowed properties
        assertEquals(3, properties.size());

        assertFalse(properties.containsKey("list.doubleValue"));

        // list property has getter
        assertTrue(properties.get("list"));
    }

    @Test
    void allowSubPropertyDeclared_propertyIsAllowed() {
        Map<String, Boolean> properties = getClientUpdateAllowedProperties(
                SubPropertyModel.class, "bean.booleanValue");
        // Bean has a couple of explicitly allowed properties
        assertEquals(4, properties.size());

        // property has getter
        assertTrue(properties.get("bean.booleanValue"));

        // list property has getter
        assertTrue(properties.get("bean"));
    }

    @Test
    void denySubPropertyDeclared_propertyIsDisallowed() {
        Map<String, Boolean> properties = getClientUpdateAllowedProperties(
                SubPropertyModel.class, "bean.intValue");
        // Bean has a couple of explicitly allowed properties
        assertEquals(3, properties.size());

        assertFalse(properties.containsKey("bean.intValue"));

        // list property has getter
        assertTrue(properties.get("bean"));
    }

    @Test
    void allowSubPropertyDeclared_propertyIsTwoWayDataBinding_propertyIsAllowed() {
        Map<String, Boolean> properties = getClientUpdateAllowedProperties(
                SubPropertyModel.class, "bean.doubleValue");
        // Bean has a couple of explicitly allowed properties
        assertEquals(4, properties.size());

        assertTrue(properties.get("bean.doubleValue"));

        // list property has getter
        assertTrue(properties.get("bean"));
    }

    @Test
    void denySubPropertyDeclared_propertyIsNotTwoWayDataBinding_propertyIsDisallowed() {
        Map<String, Boolean> properties = getClientUpdateAllowedProperties(
                SubPropertyModel.class);
        // Bean has a couple of explicitly allowed properties
        assertEquals(3, properties.size());

        assertFalse(properties.containsKey("bean.doubleValue"));

        // list property has getter
        assertTrue(properties.get("bean"));
    }

    @Test
    void importBean() {
        BeanModelType<Bean> beanType = new BeanModelType<>(Bean.class,
                PropertyFilter.ACCEPT_ALL, false);

        ElementPropertyMap model = createEmptyModel();

        Bean bean = new Bean(3);

        beanType.importProperties(model, bean, PropertyFilter.ACCEPT_ALL);

        assertThreeBean(model);
    }

    private void assertThreeBean(ElementPropertyMap model) {
        assertEquals(7, model.getPropertyNames().count());

        assertEquals(Integer.valueOf(3), model.getProperty("intValue"));
        assertEquals(Integer.valueOf(3), model.getProperty("intObject"));

        assertEquals(Double.valueOf(3), model.getProperty("doubleValue"));
        assertEquals(Double.valueOf(3), model.getProperty("doubleObject"));

        assertEquals(Boolean.TRUE, model.getProperty("booleanValue"));
        assertEquals(Boolean.TRUE, model.getProperty("booleanObject"));

        assertEquals("3", model.getProperty("string"));
    }

    @Test
    void importBean_withTypeFilter() {
        BeanModelType<Bean> beanType = new BeanModelType<>(Bean.class,
                new PropertyFilter(name -> "intValue".equals(name)), false);

        ElementPropertyMap model = createEmptyModel();

        Bean bean = new Bean(3);

        beanType.importProperties(model, bean, PropertyFilter.ACCEPT_ALL);

        assertEquals(1, model.getPropertyNames().count());

        assertEquals(Integer.valueOf(3), model.getProperty("intValue"));
    }

    @Test
    void importBean_withImportFilter() {
        BeanModelType<Bean> beanType = new BeanModelType<>(Bean.class,
                PropertyFilter.ACCEPT_ALL, false);

        ElementPropertyMap model = createEmptyModel();

        Bean bean = new Bean(3);

        beanType.importProperties(model, bean,
                new PropertyFilter(name -> "intObject".equals(name)));

        assertEquals(1, model.getPropertyNames().count());

        assertEquals(Integer.valueOf(3), model.getProperty("intObject"));
    }

    @Test
    void importBean_differentBean() {
        BeanModelType<Bean> beanType = new BeanModelType<>(Bean.class,
                PropertyFilter.ACCEPT_ALL, false);

        ElementPropertyMap model = createEmptyModel();

        DifferentBean bean = new DifferentBean(3);

        // Ignore intValue which has an incompatible type
        beanType.importProperties(model, bean,
                new PropertyFilter(name -> !"intValue".equals(name)));

        assertEquals(1, model.getPropertyNames().count());

        assertEquals("3", model.getProperty("string"));

        assertFalse(model.hasProperty("date"));
    }

    @Test
    void importBean_incompatibleBean() {
        assertThrows(IllegalArgumentException.class, () -> {
            BeanModelType<Bean> beanType = new BeanModelType<>(Bean.class,
                    PropertyFilter.ACCEPT_ALL, false);

            ElementPropertyMap model = createEmptyModel();

            DifferentBean bean = new DifferentBean(3);

            beanType.importProperties(model, bean, PropertyFilter.ACCEPT_ALL);
        });
    }

    @Test
    void modelToApplication() {
        BeanModelType<Bean> beanType = new BeanModelType<>(Bean.class,
                PropertyFilter.ACCEPT_ALL, false);

        ElementPropertyMap model = createEmptyModel();
        model.setProperty("string", "3");
        model.setProperty("intValue", Integer.valueOf(3));

        Bean bean = beanType.modelToApplication(model.getNode());

        assertEquals("3", bean.getString());
        assertEquals(3, bean.getIntValue());
        assertNull(bean.getIntObject());
    }

    @Test
    void clientValueToApplication() {
        BeanModelType<Bean> beanType = new BeanModelType<>(Bean.class,
                PropertyFilter.ACCEPT_ALL, false);

        ElementPropertyMap model = createEmptyModel();
        model.setProperty("doubleValue",
                JacksonCodec.decodeWithoutTypeInfo(JacksonUtils.writeValue(3)));
        model.setProperty("doubleObject",
                JacksonCodec.decodeWithoutTypeInfo(JacksonUtils.writeValue(3)));
        model.setProperty("intValue",
                JacksonCodec.decodeWithoutTypeInfo(JacksonUtils.writeValue(3)));
        model.setProperty("intObject",
                JacksonCodec.decodeWithoutTypeInfo(JacksonUtils.writeValue(3)));
        model.setProperty("booleanValue", JacksonCodec
                .decodeWithoutTypeInfo(JacksonUtils.writeValue(true)));
        model.setProperty("booleanObject", JacksonCodec
                .decodeWithoutTypeInfo(JacksonUtils.writeValue(true)));
        model.setProperty("string", JacksonCodec
                .decodeWithoutTypeInfo(JacksonUtils.writeValue("3")));

        Bean bean = beanType.modelToApplication(model.getNode());

        assertEquals(3.0, bean.getDoubleValue(), 0);
        assertEquals(3.0, bean.getDoubleObject(), 0);
        assertEquals(3, bean.getIntValue());
        assertEquals(3, bean.getIntObject().intValue());
        assertEquals(Boolean.TRUE, bean.getBooleanObject());
        assertTrue(bean.isBooleanValue());
        assertEquals("3", bean.getString());
    }

    @Test
    void applicationToModel() {
        BeanModelType<Bean> beanType = new BeanModelType<>(Bean.class,
                PropertyFilter.ACCEPT_ALL, false);

        Bean bean = new Bean(3);

        StateNode applicationToModel = beanType.applicationToModel(bean,
                PropertyFilter.ACCEPT_ALL);

        ElementPropertyMap model = ElementPropertyMap
                .getModel(applicationToModel);

        assertThreeBean(model);
    }

    @Test
    void applicationToModel_filtered() {
        BeanModelType<Bean> beanType = new BeanModelType<>(Bean.class,
                new PropertyFilter(name -> !name.equals("intValue")), false);

        Bean bean = new Bean(3);

        StateNode applicationToModel = beanType.applicationToModel(bean,
                new PropertyFilter(name -> name.equals("string")
                        || name.equals("intValue")));

        ElementPropertyMap model = ElementPropertyMap
                .getModel(applicationToModel);

        assertEquals(Arrays.asList("string"),
                model.getPropertyNames().collect(Collectors.toList()));

        assertEquals("3", model.getProperty("string"));
    }

    @Test
    void clientUpdateModes() {
        BeanModelType<BeanContainingBeans> beanType = new BeanModelType<>(
                BeanContainingBeans.class, PropertyFilter.ACCEPT_ALL, false);

        BeanModelType<?> bean1Type = (BeanModelType<?>) beanType
                .getPropertyType("bean1");
        BeanModelType<?> bean2Type = (BeanModelType<?>) beanType
                .getPropertyType("bean2");

        BeanModelType<?> bean3Type = (BeanModelType<?>) beanType
                .getPropertyType("bean3");

        assertEquals(ClientUpdateMode.ALLOW, bean1Type
                .getClientUpdateMode(bean1Type.getExistingProperty("string")));
        assertEquals(ClientUpdateMode.ALLOW, bean1Type.getClientUpdateMode(
                bean1Type.getExistingProperty("booleanObject")));
        assertEquals(ClientUpdateMode.IF_TWO_WAY_BINDING,
                bean1Type.getClientUpdateMode(
                        bean1Type.getExistingProperty("intValue")));

        assertEquals(ClientUpdateMode.ALLOW, bean2Type
                .getClientUpdateMode(bean2Type.getExistingProperty("string")));
        assertEquals(ClientUpdateMode.IF_TWO_WAY_BINDING,
                bean2Type.getClientUpdateMode(
                        bean2Type.getExistingProperty("booleanObject")));
        assertEquals(ClientUpdateMode.ALLOW, bean2Type.getClientUpdateMode(
                bean2Type.getExistingProperty("intValue")));

        assertEquals(ClientUpdateMode.DENY, bean3Type
                .getClientUpdateMode(bean3Type.getExistingProperty("denyInt")));

        assertEquals(ClientUpdateMode.IF_TWO_WAY_BINDING,
                bean3Type.getClientUpdateMode(
                        bean3Type.getExistingProperty("doubleObject")));
    }

    private <T extends TemplateModel> Map<String, Boolean> getClientUpdateAllowedProperties(
            Class<T> clazz, String... twoWayBindingPaths) {
        ModelDescriptor<? extends T> descriptor = ModelDescriptor.get(clazz);

        T model = TemplateModelProxyHandler
                .createModelProxy(createEmptyModel().getNode(), descriptor);

        BeanModelType<?> modelType = TemplateModelProxyHandler
                .getModelTypeForProxy(model);

        return modelType.getClientUpdateAllowedProperties(
                new HashSet<>(Arrays.asList(twoWayBindingPaths)));
    }

    private static ElementPropertyMap createEmptyModel() {
        StateNode node = new StateNode(ElementPropertyMap.class);
        return ElementPropertyMap.getModel(node);
    }
}
