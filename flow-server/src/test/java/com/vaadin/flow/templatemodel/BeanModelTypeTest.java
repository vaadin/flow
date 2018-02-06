/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.templatemodel;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

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

    @Test
    public void onlyDenyProperty_noAllowedProperties() {
        ModelDescriptor<? extends DeniedPropertyWithGetterModel> descriptor = ModelDescriptor
                .get(DeniedPropertyWithGetterModel.class);

        DeniedPropertyWithGetterModel model = TemplateModelProxyHandler
                .createModelProxy(createEmptyModel().getNode(), descriptor);

        BeanModelType<?> modelType = TemplateModelProxyHandler
                .getModelTypeForProxy(model);

        Assert.assertEquals(0, modelType
                .getClientUpdateAllowedProperties(Collections.emptySet())
                .size());

        Assert.assertEquals(0, modelType
                .getClientUpdateAllowedProperties(Collections.singleton("name"))
                .size());
    }

    @Test
    public void twoWayDatabindingPropertyDeclared_propertyIsNotAllowedIfNotTwoWayDataBinding() {
        ModelDescriptor<? extends TwoWayBindingPropertyModel> descriptor = ModelDescriptor
                .get(TwoWayBindingPropertyModel.class);

        TwoWayBindingPropertyModel model = TemplateModelProxyHandler
                .createModelProxy(createEmptyModel().getNode(), descriptor);

        BeanModelType<?> modelType = TemplateModelProxyHandler
                .getModelTypeForProxy(model);

        Assert.assertEquals(0, modelType
                .getClientUpdateAllowedProperties(Collections.emptySet())
                .size());

        // test name property
        Assert.assertEquals(1, modelType
                .getClientUpdateAllowedProperties(Collections.singleton("name"))
                .size());
        // name property has getter
        Assert.assertTrue(modelType
                .getClientUpdateAllowedProperties(Collections.singleton("name"))
                .get("name"));

        // test age property
        Assert.assertEquals(1, modelType
                .getClientUpdateAllowedProperties(Collections.singleton("age"))
                .size());
        // age property has no getter
        Assert.assertFalse(modelType
                .getClientUpdateAllowedProperties(Collections.singleton("age"))
                .get("age"));
    }

    @Test
    public void allowPropertyDeclared_propertyIsAllowed() {
        ModelDescriptor<? extends AllowPropertyModel> descriptor = ModelDescriptor
                .get(AllowPropertyModel.class);

        AllowPropertyModel model = TemplateModelProxyHandler
                .createModelProxy(createEmptyModel().getNode(), descriptor);

        BeanModelType<?> modelType = TemplateModelProxyHandler
                .getModelTypeForProxy(model);

        Map<String, Boolean> properties = modelType
                .getClientUpdateAllowedProperties(Collections.emptySet());
        Assert.assertEquals(2, properties.size());

        // name property has getter
        Assert.assertTrue(properties.get("name"));

        // age property has no getter
        Assert.assertFalse(properties.get("age"));
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

    private static ElementPropertyMap createEmptyModel() {
        StateNode node = new StateNode(ElementPropertyMap.class);
        return ElementPropertyMap.getModel(node);
    }
}
