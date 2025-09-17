/**
 * Copyright (C) 2022 Vaadin Ltd
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

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.internal.JacksonCodec;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;

public class BeanModelTypeTest {
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

        public void setIntValue(double intValue) {
            this.intValue = intValue;
        }

        public String getString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }

        public void setDate(Date date) {
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
    public void onlyDenyProperty_noAllowedProperties() {
        Assert.assertEquals(0, getClientUpdateAllowedProperties(
                DeniedPropertyWithGetterModel.class).size());

        Assert.assertEquals(0, getClientUpdateAllowedProperties(
                DeniedPropertyWithGetterModel.class, "name").size());
    }

    @Test
    public void twoWayDatabindingPropertyDeclared_propertyIsNotAllowedIfNotTwoWayDataBinding() {
        Assert.assertEquals(0, getClientUpdateAllowedProperties(
                TwoWayBindingPropertyModel.class).size());

        // test name property
        Assert.assertEquals(1, getClientUpdateAllowedProperties(
                TwoWayBindingPropertyModel.class, "name").size());
        // name property has getter
        Assert.assertTrue(getClientUpdateAllowedProperties(
                TwoWayBindingPropertyModel.class, "name").get("name"));

        // test age property
        Assert.assertEquals(1, getClientUpdateAllowedProperties(
                TwoWayBindingPropertyModel.class, "age").size());
        // age property has no getter
        Assert.assertFalse(getClientUpdateAllowedProperties(
                TwoWayBindingPropertyModel.class, "age").get("age"));
    }

    @Test
    public void allowPropertyDeclared_propertyIsAllowed() {
        Map<String, Boolean> properties = getClientUpdateAllowedProperties(
                AllowPropertyModel.class);
        Assert.assertEquals(2, properties.size());

        // name property has getter
        Assert.assertTrue(properties.get("name"));

        // age property has no getter
        Assert.assertFalse(properties.get("age"));
    }

    @Test
    public void allowListSubPropertyDeclared_propertyIsAllowed() {
        Map<String, Boolean> properties = getClientUpdateAllowedProperties(
                ListPropertyModel.class, "list.booleanValue");
        // Bean has a couple of explicitly allowed properties
        Assert.assertEquals(4, properties.size());

        // property has getter
        Assert.assertTrue(properties.get("list.booleanValue"));

        // list property has getter
        Assert.assertTrue(properties.get("list"));
    }

    @Test
    public void denyListSubPropertyDeclared_propertyIsDisallowed() {
        Map<String, Boolean> properties = getClientUpdateAllowedProperties(
                ListPropertyModel.class, "list.intValue");
        // Bean has a couple of explicitly allowed properties
        Assert.assertEquals(3, properties.size());

        Assert.assertFalse(properties.containsKey("list.intValue"));

        // list property has getter
        Assert.assertTrue(properties.get("list"));
    }

    @Test
    public void allowListSubPropertyDeclared_propertyIsTwoWayDataBinding_propertyIsAllowed() {
        Map<String, Boolean> properties = getClientUpdateAllowedProperties(
                ListPropertyModel.class, "list.doubleValue");
        // Bean has a couple of explicitly allowed properties
        Assert.assertEquals(4, properties.size());

        Assert.assertTrue(properties.get("list.doubleValue"));

        // list property has getter
        Assert.assertTrue(properties.get("list"));
    }

    @Test
    public void denyListSubPropertyDeclared_propertyIsNotTwoWayDataBinding_propertyIsDisallowed() {
        Map<String, Boolean> properties = getClientUpdateAllowedProperties(
                ListPropertyModel.class);
        // Bean has a couple of explicitly allowed properties
        Assert.assertEquals(3, properties.size());

        Assert.assertFalse(properties.containsKey("list.doubleValue"));

        // list property has getter
        Assert.assertTrue(properties.get("list"));
    }

    @Test
    public void allowSubPropertyDeclared_propertyIsAllowed() {
        Map<String, Boolean> properties = getClientUpdateAllowedProperties(
                SubPropertyModel.class, "bean.booleanValue");
        // Bean has a couple of explicitly allowed properties
        Assert.assertEquals(4, properties.size());

        // property has getter
        Assert.assertTrue(properties.get("bean.booleanValue"));

        // list property has getter
        Assert.assertTrue(properties.get("bean"));
    }

    @Test
    public void denySubPropertyDeclared_propertyIsDisallowed() {
        Map<String, Boolean> properties = getClientUpdateAllowedProperties(
                SubPropertyModel.class, "bean.intValue");
        // Bean has a couple of explicitly allowed properties
        Assert.assertEquals(3, properties.size());

        Assert.assertFalse(properties.containsKey("bean.intValue"));

        // list property has getter
        Assert.assertTrue(properties.get("bean"));
    }

    @Test
    public void allowSubPropertyDeclared_propertyIsTwoWayDataBinding_propertyIsAllowed() {
        Map<String, Boolean> properties = getClientUpdateAllowedProperties(
                SubPropertyModel.class, "bean.doubleValue");
        // Bean has a couple of explicitly allowed properties
        Assert.assertEquals(4, properties.size());

        Assert.assertTrue(properties.get("bean.doubleValue"));

        // list property has getter
        Assert.assertTrue(properties.get("bean"));
    }

    @Test
    public void denySubPropertyDeclared_propertyIsNotTwoWayDataBinding_propertyIsDisallowed() {
        Map<String, Boolean> properties = getClientUpdateAllowedProperties(
                SubPropertyModel.class);
        // Bean has a couple of explicitly allowed properties
        Assert.assertEquals(3, properties.size());

        Assert.assertFalse(properties.containsKey("bean.doubleValue"));

        // list property has getter
        Assert.assertTrue(properties.get("bean"));
    }

    @Test
    public void importBean() {
        BeanModelType<Bean> beanType = new BeanModelType<>(Bean.class,
                PropertyFilter.ACCEPT_ALL, false);

        ElementPropertyMap model = createEmptyModel();

        Bean bean = new Bean(3);

        beanType.importProperties(model, bean, PropertyFilter.ACCEPT_ALL);

        assertThreeBean(model);
    }

    private void assertThreeBean(ElementPropertyMap model) {
        Assert.assertEquals(7, model.getPropertyNames().count());

        Assert.assertEquals(Integer.valueOf(3), model.getProperty("intValue"));
        Assert.assertEquals(Integer.valueOf(3), model.getProperty("intObject"));

        Assert.assertEquals(Double.valueOf(3),
                model.getProperty("doubleValue"));
        Assert.assertEquals(Double.valueOf(3),
                model.getProperty("doubleObject"));

        Assert.assertEquals(Boolean.TRUE, model.getProperty("booleanValue"));
        Assert.assertEquals(Boolean.TRUE, model.getProperty("booleanObject"));

        Assert.assertEquals("3", model.getProperty("string"));
    }

    @Test
    public void importBean_withTypeFilter() {
        BeanModelType<Bean> beanType = new BeanModelType<>(Bean.class,
                new PropertyFilter(name -> "intValue".equals(name)), false);

        ElementPropertyMap model = createEmptyModel();

        Bean bean = new Bean(3);

        beanType.importProperties(model, bean, PropertyFilter.ACCEPT_ALL);

        Assert.assertEquals(1, model.getPropertyNames().count());

        Assert.assertEquals(Integer.valueOf(3), model.getProperty("intValue"));
    }

    @Test
    public void importBean_withImportFilter() {
        BeanModelType<Bean> beanType = new BeanModelType<>(Bean.class,
                PropertyFilter.ACCEPT_ALL, false);

        ElementPropertyMap model = createEmptyModel();

        Bean bean = new Bean(3);

        beanType.importProperties(model, bean,
                new PropertyFilter(name -> "intObject".equals(name)));

        Assert.assertEquals(1, model.getPropertyNames().count());

        Assert.assertEquals(Integer.valueOf(3), model.getProperty("intObject"));
    }

    @Test
    public void importBean_differentBean() {
        BeanModelType<Bean> beanType = new BeanModelType<>(Bean.class,
                PropertyFilter.ACCEPT_ALL, false);

        ElementPropertyMap model = createEmptyModel();

        DifferentBean bean = new DifferentBean(3);

        // Ignore intValue which has an incompatible type
        beanType.importProperties(model, bean,
                new PropertyFilter(name -> !"intValue".equals(name)));

        Assert.assertEquals(1, model.getPropertyNames().count());

        Assert.assertEquals("3", model.getProperty("string"));

        Assert.assertFalse(model.hasProperty("date"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void importBean_incompatibleBean() {
        BeanModelType<Bean> beanType = new BeanModelType<>(Bean.class,
                PropertyFilter.ACCEPT_ALL, false);

        ElementPropertyMap model = createEmptyModel();

        DifferentBean bean = new DifferentBean(3);

        beanType.importProperties(model, bean, PropertyFilter.ACCEPT_ALL);
    }

    @Test
    public void modelToApplication() {
        BeanModelType<Bean> beanType = new BeanModelType<>(Bean.class,
                PropertyFilter.ACCEPT_ALL, false);

        ElementPropertyMap model = createEmptyModel();
        model.setProperty("string", "3");
        model.setProperty("intValue", Integer.valueOf(3));

        Bean bean = beanType.modelToApplication(model.getNode());

        Assert.assertEquals("3", bean.getString());
        Assert.assertEquals(3, bean.getIntValue());
        Assert.assertNull(bean.getIntObject());
    }

    @Test
    public void clientValueToApplication() {
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

        Assert.assertEquals(3.0, bean.getDoubleValue(), 0);
        Assert.assertEquals(3.0, bean.getDoubleObject(), 0);
        Assert.assertEquals(3, bean.getIntValue());
        Assert.assertEquals(3, bean.getIntObject().intValue());
        Assert.assertEquals(Boolean.TRUE, bean.getBooleanObject());
        Assert.assertTrue(bean.isBooleanValue());
        Assert.assertEquals("3", bean.getString());
    }

    @Test
    public void applicationToModel() {
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
    public void applicationToModel_filtered() {
        BeanModelType<Bean> beanType = new BeanModelType<>(Bean.class,
                new PropertyFilter(name -> !name.equals("intValue")), false);

        Bean bean = new Bean(3);

        StateNode applicationToModel = beanType.applicationToModel(bean,
                new PropertyFilter(name -> name.equals("string")
                        || name.equals("intValue")));

        ElementPropertyMap model = ElementPropertyMap
                .getModel(applicationToModel);

        Assert.assertEquals(Arrays.asList("string"),
                model.getPropertyNames().collect(Collectors.toList()));

        Assert.assertEquals("3", model.getProperty("string"));
    }

    @Test
    public void clientUpdateModes() {
        BeanModelType<BeanContainingBeans> beanType = new BeanModelType<>(
                BeanContainingBeans.class, PropertyFilter.ACCEPT_ALL, false);

        BeanModelType<?> bean1Type = (BeanModelType<?>) beanType
                .getPropertyType("bean1");
        BeanModelType<?> bean2Type = (BeanModelType<?>) beanType
                .getPropertyType("bean2");

        BeanModelType<?> bean3Type = (BeanModelType<?>) beanType
                .getPropertyType("bean3");

        Assert.assertEquals(ClientUpdateMode.ALLOW, bean1Type
                .getClientUpdateMode(bean1Type.getExistingProperty("string")));
        Assert.assertEquals(ClientUpdateMode.ALLOW,
                bean1Type.getClientUpdateMode(
                        bean1Type.getExistingProperty("booleanObject")));
        Assert.assertEquals(ClientUpdateMode.IF_TWO_WAY_BINDING,
                bean1Type.getClientUpdateMode(
                        bean1Type.getExistingProperty("intValue")));

        Assert.assertEquals(ClientUpdateMode.ALLOW, bean2Type
                .getClientUpdateMode(bean2Type.getExistingProperty("string")));
        Assert.assertEquals(ClientUpdateMode.IF_TWO_WAY_BINDING,
                bean2Type.getClientUpdateMode(
                        bean2Type.getExistingProperty("booleanObject")));
        Assert.assertEquals(ClientUpdateMode.ALLOW,
                bean2Type.getClientUpdateMode(
                        bean2Type.getExistingProperty("intValue")));

        Assert.assertEquals(ClientUpdateMode.DENY, bean3Type
                .getClientUpdateMode(bean3Type.getExistingProperty("denyInt")));

        Assert.assertEquals(ClientUpdateMode.IF_TWO_WAY_BINDING,
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
